package com.nborba.vocalize.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.nborba.vocalize.core.audio.domain.AudioRecorder
import com.nborba.vocalize.core.audio.domain.AudioRecorderState
import com.nborba.vocalize.core.audio.domain.InterruptionReason
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal class MediaRecorderAudioRecorder
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AudioRecorder {
        private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        private var audioFocusRequest: AudioFocusRequest? = null

        private var recorder: MediaRecorder? = null

        private val _state = MutableStateFlow<AudioRecorderState>(AudioRecorderState.Idle())
        override val state: StateFlow<AudioRecorderState> = _state.asStateFlow()

        private val _durationMillis = MutableStateFlow(0L)
        override val durationMillis: StateFlow<Long> = _durationMillis.asStateFlow()

        private val _audioWaveform = MutableStateFlow<List<Float>>(emptyList())
        override val audioWaveform: StateFlow<List<Float>> = _audioWaveform.asStateFlow()

        private var trackingJob: Job? = null

        private fun createRecorder(): MediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }

        private fun requestAudioFocus(onFocusLost: () -> Unit): Boolean {
            val request =
                AudioFocusRequest
                    .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    ).setOnAudioFocusChangeListener { focusChange ->
                        if ((focusChange == AudioManager.AUDIOFOCUS_LOSS) ||
                            (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
                        ) {
                            onFocusLost()
                        }
                    }.build()
            audioFocusRequest = request

            return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        private fun releaseAudioFocus() {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }

        override fun start(outputFile: File) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("MediaRecorderAudioRecorder", "RECORD_AUDIO permission not granted")
                _state.value = AudioRecorderState.Idle(InterruptionReason.UnknownError)
                return
            }

            val focusGranted =
                requestAudioFocus {
                    pause()
                    _state.value = AudioRecorderState.Paused(InterruptionReason.AudioFocusLoss)
                }

            if (!focusGranted) {
                _state.value = AudioRecorderState.Idle(InterruptionReason.AudioFocusLoss)
                return
            }

            registerCallStateListener {
                pause()
                _state.value = AudioRecorderState.Paused(InterruptionReason.IncomingCall)
            }

            val minRequiredBytes = 5_000_000L // 5 MB minimum threshold to allow starting
            val availableBytes = outputFile.parentFile?.freeSpace ?: 0L

            // Early return if space is insufficient to start recording
            if (availableBytes < minRequiredBytes) {
                Log.w("MediaRecorderAudioRecorder", "Insufficient storage space ($availableBytes bytes free)")
                _state.value = AudioRecorderState.Idle(InterruptionReason.StorageFull)
                return
            }

            val safetyMargin = 50_000_000L // 50 MB
            val maxSizeBytes = (availableBytes - safetyMargin).coerceAtLeast(minRequiredBytes)

            try {
                val recorderInstance =
                    createRecorder().apply {
                        setOutputFile(outputFile.absolutePath)

                        try {
                            setMaxFileSize(maxSizeBytes)
                        } catch (ex: Exception) {
                            Log.e("MediaRecorderAudioRecorder", "Failed to set max file size", ex)
                        }

                        // Triggered when storage max size limit is reached
                        setOnInfoListener { _, what, _ ->
                            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                                stop()
                                _state.value = AudioRecorderState.Idle(InterruptionReason.StorageFull)
                            }
                        }

                        // Triggered on hardware/encoder native errors
                        setOnErrorListener { _, _, _ ->
                            stop()
                            _state.value = AudioRecorderState.Idle(InterruptionReason.UnknownError)
                        }

                        prepare()
                        start()
                    }
                recorder = recorderInstance
                _durationMillis.value = 0L
                _audioWaveform.value = emptyList()
                _state.value = AudioRecorderState.Recording
                startTracking()
            } catch (ex: Exception) {
                Log.e("MediaRecorderAudioRecorder", "Failed to prepare or start MediaRecorder", ex)
                stop()
                _state.value = AudioRecorderState.Idle(InterruptionReason.UnknownError)
            }
        }

        override fun pause() {
            try {
                recorder?.pause()
                _state.value = AudioRecorderState.Paused()
            } catch (ex: Exception) {
                Log.e("MediaRecorderAudioRecorder", "Failed to pause recorder", ex)
            }
        }

        override fun resume() {
            try {
                recorder?.resume()
                _state.value = AudioRecorderState.Recording
            } catch (ex: Exception) {
                Log.e("MediaRecorderAudioRecorder", "Failed to resume recorder", ex)
            }
        }

        override fun stop() {
            stopTracking()
            unregisterCallStateListener()
            releaseAudioFocus()

            recorder?.apply {
                try {
                    stop()
                } catch (ex: RuntimeException) {
                    Log.e("MediaRecorderAudioRecorder", "Failed to stop", ex)
                } finally {
                    reset()
                    release()
                }
            }
            recorder = null
            _durationMillis.value = 0L
            _audioWaveform.value = emptyList()
            _state.value = AudioRecorderState.Idle()
        }

        override fun getMaxAmplitude(): Int = recorder?.maxAmplitude ?: 0

        override fun close() {
            stop()
        }

        private fun startTracking() {
            trackingJob?.cancel()
            trackingJob =
                CoroutineScope(Dispatchers.Default).launch {
                    var lastTime = System.currentTimeMillis()
                    while (isActive) {
                        delay(SAMPLE_INTERVAL_MS.milliseconds)
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        lastTime = now

                        if (_state.value is AudioRecorderState.Recording) {
                            _durationMillis.update { it + elapsed }
                            val rawAmplitude = getMaxAmplitude()
                            val normalized = (rawAmplitude / MAX_AMPLITUDE_RANGE).coerceIn(0f, 1f)
                            _audioWaveform.update { current ->
                                val updated = current + normalized
                                if (updated.size > MAX_WAVEFORM_SAMPLES) {
                                    updated.takeLast(MAX_WAVEFORM_SAMPLES)
                                } else {
                                    updated
                                }
                            }
                        }
                    }
                }
        }

        private fun stopTracking() {
            trackingJob?.cancel()
            trackingJob = null
        }

        private var telephonyCallback: TelephonyCallback? = null

        private fun registerCallStateListener(onCallStarted: () -> Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                        val callback =
                            object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                                override fun onCallStateChanged(state: Int) {
                                    if (state == TelephonyManager.CALL_STATE_RINGING ||
                                        state == TelephonyManager.CALL_STATE_OFFHOOK
                                    ) {
                                        onCallStarted()
                                    }
                                }
                            }
                        telephonyCallback = callback
                        telephonyManager?.registerTelephonyCallback(context.mainExecutor, callback)
                    } catch (e: SecurityException) {
                        Log.w("MediaRecorderAudioRecorder", "Failed to register call state listener", e)
                    }
                } else {
                    Log.w(
                        "MediaRecorderAudioRecorder",
                        "READ_PHONE_STATE permission not granted; skipping call state listener registration",
                    )
                }
            }
        }

        private fun unregisterCallStateListener() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { callback ->
                    try {
                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                        telephonyManager?.unregisterTelephonyCallback(callback)
                    } catch (e: SecurityException) {
                        Log.w("MediaRecorderAudioRecorder", "Failed to unregister call state listener", e)
                    }
                }
                telephonyCallback = null
            }
        }

        private companion object {
            const val SAMPLE_INTERVAL_MS = 100L
            const val MAX_WAVEFORM_SAMPLES = 50
            const val MAX_AMPLITUDE_RANGE = 32767f
        }
    }
