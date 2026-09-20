package com.nborba.vocalize.feature.recorder.impl.ui.recorder

import android.Manifest
import android.content.Context
import com.nborba.vocalize.core.audio.domain.AudioRecorder
import com.nborba.vocalize.core.audio.domain.AudioRecorderState
import com.nborba.vocalize.core.audio.domain.InterruptionReason
import com.nborba.vocalize.core.common.util.MainDispatcherExtension
import com.nborba.vocalize.core.common.util.StringProvider
import com.nborba.vocalize.core.permission.domain.PermissionChecker
import com.nborba.vocalize.core.permission.host.PermissionResult
import com.nborba.vocalize.feature.recorder.impl.R
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.model.RecorderEffect
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.model.RecorderState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

internal class RecorderBottomSheetViewModelTest {
    @JvmField
    @RegisterExtension
    val mainCoroutinesDispatcher = MainDispatcherExtension()

    private val audioRecorderStateFlow = MutableStateFlow<AudioRecorderState>(AudioRecorderState.Idle())
    private val durationMillisFlow = MutableStateFlow(0L)
    private val audioWaveformFlow = MutableStateFlow<List<Float>>(emptyList())
    private val audioRecorder: AudioRecorder = mockk(relaxed = true)
    private val permissionChecker: PermissionChecker = mockk()
    private val stringProvider: StringProvider = mockk()
    private val context: Context = mockk(relaxed = true)
    private lateinit var viewModel: RecorderBottomSheetViewModel

    @BeforeEach
    fun setUp() {
        audioRecorderStateFlow.value = AudioRecorderState.Idle()
        durationMillisFlow.value = 0L
        audioWaveformFlow.value = emptyList()
        every { audioRecorder.state } returns audioRecorderStateFlow
        every { audioRecorder.durationMillis } returns durationMillisFlow
        every { audioRecorder.audioWaveform } returns audioWaveformFlow
        every { audioRecorder.start(any()) } answers {
            audioRecorderStateFlow.value = AudioRecorderState.Recording
        }
        every { audioRecorder.pause() } answers {
            audioRecorderStateFlow.value = AudioRecorderState.Paused()
        }
        every { audioRecorder.resume() } answers {
            audioRecorderStateFlow.value = AudioRecorderState.Recording
        }
        every { audioRecorder.stop() } answers {
            audioRecorderStateFlow.value = AudioRecorderState.Idle()
        }
        every { context.cacheDir } returns File("/tmp")
        every { permissionChecker.hasAllPermissions(any()) } returns true
        every { stringProvider.getString(R.string.recorder_recording_saved) } returns "Recording has been saved"
    }

    @Test
    fun `when audio permission granted, init starts recording`() {
        viewModel = viewModel(permissionChecker = permissionChecker)

        assertEquals(RecorderState.Recording, viewModel.uiState.value.state)
    }

    @Test
    fun `when audio permission not granted, init requests permission`() {
        every { permissionChecker.hasAllPermissions(any()) } returns false

        viewModel = viewModel(permissionChecker = permissionChecker)

        assertEquals(RecorderState.Idle, viewModel.uiState.value.state)
        assertEquals(
            RecorderEffect.RequestPermission(
                listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE),
            ),
            viewModel.uiState.value.effect,
        )
    }

    @Test
    fun `when onMainButtonClick while recording, pauses recording`() {
        viewModel = viewModel(permissionChecker = permissionChecker)

        assertEquals(RecorderState.Recording, viewModel.uiState.value.state)

        viewModel.onMainButtonClick()

        assertEquals(RecorderState.Paused, viewModel.uiState.value.state)
    }

    @Test
    fun `when onMainButtonClick while paused, resumes recording`() {
        viewModel = viewModel(permissionChecker = permissionChecker)
        viewModel.onMainButtonClick() // Recording -> Paused
        assertEquals(RecorderState.Paused, viewModel.uiState.value.state)

        viewModel.onMainButtonClick() // Paused -> Recording

        assertEquals(RecorderState.Recording, viewModel.uiState.value.state)
    }

    @Test
    fun `when onDismissRequest while idle, emits Dismiss effect`() {
        every { permissionChecker.hasAllPermissions(any()) } returns false

        viewModel = viewModel(permissionChecker = permissionChecker)
        assertEquals(RecorderState.Idle, viewModel.uiState.value.state)

        viewModel.onDismissRequest()

        assertEquals(RecorderState.Idle, viewModel.uiState.value.state)
        assertEquals(RecorderEffect.Dismiss(), viewModel.uiState.value.effect)
    }

    @Test
    fun `when onDismissRequest while recording, emits Dismiss effect and stops recording`() {
        viewModel = viewModel(permissionChecker = permissionChecker)

        assertEquals(RecorderState.Recording, viewModel.uiState.value.state)

        viewModel.onDismissRequest()

        assertEquals(RecorderState.Idle, viewModel.uiState.value.state)
        assertEquals(
            RecorderEffect.Dismiss("Recording has been saved"),
            viewModel.uiState.value.effect,
        )
    }

    @Test
    fun `when onDismissRequest while paused, emits Dismiss effect and stops recording`() {
        viewModel = viewModel(permissionChecker = permissionChecker)

        viewModel.onMainButtonClick() // Recording -> Paused
        assertEquals(RecorderState.Paused, viewModel.uiState.value.state)

        viewModel.onDismissRequest() // Paused -> Idle

        assertEquals(RecorderState.Idle, viewModel.uiState.value.state)
        assertEquals(
            RecorderEffect.Dismiss("Recording has been saved"),
            viewModel.uiState.value.effect,
        )
    }

    @Test
    fun `when onPermissionsRequestResult Granted, starts recording`() {
        every { permissionChecker.hasAllPermissions(any()) } returns false
        viewModel = viewModel(permissionChecker = permissionChecker)

        every { permissionChecker.hasAllPermissions(any()) } returns true
        viewModel.onPermissionsRequestResult(PermissionResult.Granted)

        assertEquals(RecorderState.Recording, viewModel.uiState.value.state)
    }

    @Test
    fun `when onPermissionsRequestResult Denied, emits Dismiss effect`() {
        every { permissionChecker.hasAllPermissions(any()) } returns false
        viewModel = viewModel(permissionChecker = permissionChecker)

        viewModel.onPermissionsRequestResult(PermissionResult.Denied)

        assertEquals(RecorderEffect.Dismiss(), viewModel.uiState.value.effect)
    }

    @Test
    fun `when onEffectConsumed, clears effect`() {
        every { permissionChecker.hasAllPermissions(any()) } returns false
        viewModel = viewModel(permissionChecker = permissionChecker)

        viewModel.onEffectConsumed()

        assertNull(viewModel.uiState.value.effect)
    }

    @Test
    fun `when duration updates from audioRecorder, uiState duration updates`() {
        viewModel = viewModel()

        durationMillisFlow.value = 5000L

        assertEquals(5000L, viewModel.uiState.value.durationMillis)
    }

    @Test
    fun `when audioWaveform updates from audioRecorder, uiState audioWaveform updates`() {
        viewModel = viewModel()

        val sampleList = listOf(0.1f, 0.5f, 0.8f)
        audioWaveformFlow.value = sampleList

        assertEquals(sampleList, viewModel.uiState.value.audioWaveform)
    }

    @Test
    fun `when audio focus loss interruption occurs, pauses recording`() {
        viewModel = viewModel()

        audioRecorderStateFlow.value = AudioRecorderState.Paused(InterruptionReason.AudioFocusLoss)

        assertEquals(RecorderState.Paused, viewModel.uiState.value.state)
        assertEquals(
            RecorderEffect.ShowToast("Recording paused: Audio focus lost"),
            viewModel.uiState.value.effect,
        )
    }

    @Test
    fun `when incoming call interruption occurs, pauses recording`() {
        viewModel = viewModel()

        audioRecorderStateFlow.value = AudioRecorderState.Paused(InterruptionReason.IncomingCall)

        assertEquals(RecorderState.Paused, viewModel.uiState.value.state)
        assertEquals(
            RecorderEffect.ShowToast("Recording paused due to incoming call"),
            viewModel.uiState.value.effect,
        )
    }

    @Test
    fun `when storage full interruption occurs, stops recording and transitions to idle`() {
        viewModel = viewModel()

        audioRecorderStateFlow.value = AudioRecorderState.Idle(InterruptionReason.StorageFull)

        assertEquals(RecorderState.Idle, viewModel.uiState.value.state)
        assertEquals(
            RecorderEffect.ShowToast("Recording stopped: Device storage full"),
            viewModel.uiState.value.effect,
        )
    }

    @Test
    fun `when unknown error interruption occurs, stops recording and transitions to idle`() {
        viewModel = viewModel()

        audioRecorderStateFlow.value = AudioRecorderState.Idle(InterruptionReason.UnknownError)

        assertEquals(RecorderState.Idle, viewModel.uiState.value.state)
        assertEquals(
            RecorderEffect.ShowToast("Recording stopped due to an error"),
            viewModel.uiState.value.effect,
        )
    }

    private fun viewModel(
        audioRecorder: AudioRecorder = this.audioRecorder,
        permissionChecker: PermissionChecker = this.permissionChecker,
        stringProvider: StringProvider = this.stringProvider,
        context: Context = this.context,
    ): RecorderBottomSheetViewModel =
        RecorderBottomSheetViewModel(
            audioRecorder = audioRecorder,
            permissionChecker = permissionChecker,
            stringProvider = stringProvider,
            context = context,
        )
}
