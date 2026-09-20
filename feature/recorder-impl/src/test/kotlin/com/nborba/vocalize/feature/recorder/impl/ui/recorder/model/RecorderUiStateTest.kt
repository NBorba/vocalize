package com.nborba.vocalize.feature.recorder.impl.ui.recorder.model

import com.nborba.vocalize.core.designsystem.icon.VocalizeIcons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RecorderUiStateTest {
    @Test
    fun `when state is Idle, mainButtonIcon returns Record icon`() {
        val uiState = RecorderUiState(state = RecorderState.Idle)

        assertEquals(VocalizeIcons.Record, uiState.mainButtonIcon)
    }

    @Test
    fun `when state is Recording, mainButtonIcon returns Pause icon`() {
        val uiState = RecorderUiState(state = RecorderState.Recording)

        assertEquals(VocalizeIcons.Pause, uiState.mainButtonIcon)
    }

    @Test
    fun `when state is Paused, mainButtonIcon returns Play icon`() {
        val uiState = RecorderUiState(state = RecorderState.Paused)

        assertEquals(VocalizeIcons.Play, uiState.mainButtonIcon)
    }
}
