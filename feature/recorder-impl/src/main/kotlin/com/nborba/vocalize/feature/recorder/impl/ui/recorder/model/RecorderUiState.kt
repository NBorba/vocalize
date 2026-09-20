package com.nborba.vocalize.feature.recorder.impl.ui.recorder.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.nborba.vocalize.core.designsystem.icon.VocalizeIcons
import java.util.Locale

sealed interface RecorderEffect {
    data class RequestPermission(
        val permissions: List<String>,
    ) : RecorderEffect

    data class ShowToast(
        val message: String,
    ) : RecorderEffect

    data class Dismiss(
        val message: String? = null,
    ) : RecorderEffect
}

enum class RecorderState {
    Idle,
    Recording,
    Paused,
}

data class RecorderUiState(
    val state: RecorderState = RecorderState.Idle,
    val durationMillis: Long = 0L,
    val audioWaveform: List<Float> = emptyList(),
    val effect: RecorderEffect? = null,
) {
    val mainButtonIcon: ImageVector
        get() =
            when (state) {
                RecorderState.Idle -> VocalizeIcons.Record
                RecorderState.Recording -> VocalizeIcons.Pause
                RecorderState.Paused -> VocalizeIcons.Play
            }

    val formattedDuration: String
        get() {
            val totalSeconds = durationMillis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
}
