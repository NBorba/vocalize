package com.nborba.vocalize.feature.recorder.impl.ui.recorder

import android.Manifest
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nborba.vocalize.core.audio.domain.AudioRecorder
import com.nborba.vocalize.core.audio.domain.AudioRecorderState
import com.nborba.vocalize.core.audio.domain.InterruptionReason
import com.nborba.vocalize.core.common.util.StringProvider
import com.nborba.vocalize.core.permission.domain.PermissionChecker
import com.nborba.vocalize.core.permission.host.PermissionResult
import com.nborba.vocalize.feature.recorder.impl.R
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.model.RecorderEffect
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.model.RecorderState
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.model.RecorderUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class RecorderBottomSheetViewModel
    @Inject
    constructor(
        private val audioRecorder: AudioRecorder,
        private val permissionChecker: PermissionChecker,
        private val stringProvider: StringProvider,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecorderUiState())
        val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

        private var currentOutputFile: File? = null

        val effects = uiState.map { it.effect }

        init {
            observeAudioRecorderState()
            onMainButtonClick()
        }

        override fun onCleared() {
            audioRecorder.close()
        }

        private fun observeAudioRecorderState() {
            viewModelScope.launch {
                audioRecorder.state.collect { recorderState ->
                    val uiRecorderState =
                        when (recorderState) {
                            AudioRecorderState.Recording -> RecorderState.Recording
                            is AudioRecorderState.Paused -> RecorderState.Paused
                            is AudioRecorderState.Idle -> RecorderState.Idle
                        }

                    val reason =
                        when (recorderState) {
                            is AudioRecorderState.Paused -> recorderState.reason
                            is AudioRecorderState.Idle -> recorderState.reason
                            AudioRecorderState.Recording -> null
                        }

                    val toastEffect = reason?.let { getInterruptionToastEffect(it) }

                    _uiState.update { current ->
                        current.copy(
                            state = uiRecorderState,
                            effect = toastEffect ?: current.effect,
                        )
                    }
                }
            }

            viewModelScope.launch {
                audioRecorder.durationMillis.collect { duration ->
                    _uiState.update { it.copy(durationMillis = duration) }
                }
            }

            viewModelScope.launch {
                audioRecorder.audioWaveform.collect { waveform ->
                    _uiState.update { it.copy(audioWaveform = waveform) }
                }
            }
        }

        fun onMainButtonClick() {
            when (getRecorderState()) {
                RecorderState.Idle -> startRecording()
                RecorderState.Recording -> pauseRecording()
                RecorderState.Paused -> resumeRecording()
            }
        }

        fun onStopButtonClick() {
            onDismissRequest()
        }

        fun onDismissRequest() {
            if (getRecorderState() == RecorderState.Idle) {
                _uiState.update { it.copy(effect = RecorderEffect.Dismiss()) }
            } else {
                stopRecording()
                _uiState.update {
                    it.copy(
                        effect =
                            RecorderEffect.Dismiss(
                                stringProvider.getString(R.string.recorder_recording_saved),
                            ),
                    )
                }
            }
        }

        fun onPermissionsRequestResult(result: PermissionResult) {
            if (result == PermissionResult.Granted) {
                onMainButtonClick()
            } else {
                _uiState.update { it.copy(effect = RecorderEffect.Dismiss()) }
            }
        }

        fun onEffectConsumed() {
            _uiState.update { it.copy(effect = null) }
        }

        private fun getRecorderState(): RecorderState = uiState.value.state

        private fun startRecording() {
            requireAudioPermission {
                val outputFile = File(context.cacheDir, getFileName())
                currentOutputFile = outputFile
                audioRecorder.start(outputFile)
            }
        }

        private fun pauseRecording() {
            audioRecorder.pause()
        }

        private fun resumeRecording() {
            requireAudioPermission {
                audioRecorder.resume()
            }
        }

        private fun stopRecording() {
            audioRecorder.stop()
        }

        private fun getInterruptionToastEffect(reason: InterruptionReason): RecorderEffect.ShowToast {
            val message =
                when (reason) {
                    InterruptionReason.AudioFocusLoss -> "Recording paused: Audio focus lost"
                    InterruptionReason.IncomingCall -> "Recording paused due to incoming call"
                    InterruptionReason.StorageFull -> "Recording stopped: Device storage full"
                    InterruptionReason.UnknownError -> "Recording stopped due to an error"
                }
            return RecorderEffect.ShowToast(message)
        }

        private fun requireAudioPermission(onPermissionGranted: () -> Unit) {
            if (permissionChecker.hasAllPermissions(requiredPermissions)) {
                onPermissionGranted()
            } else {
                requestPermissions()
            }
        }

        private fun requestPermissions() {
            _uiState.update {
                it.copy(effect = RecorderEffect.RequestPermission(requiredPermissions))
            }
        }

        private fun getFileName(): String = "$FILE_NAME_PREFIX${System.currentTimeMillis()}.m4a"

        private companion object {
            const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
            const val PHONE_STATE_PERMISSION = Manifest.permission.READ_PHONE_STATE
            const val FILE_NAME_PREFIX = "recording_"

            val requiredPermissions = listOf(AUDIO_PERMISSION, PHONE_STATE_PERMISSION)
        }
    }
