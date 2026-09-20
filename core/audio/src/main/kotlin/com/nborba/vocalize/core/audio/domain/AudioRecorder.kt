package com.nborba.vocalize.core.audio.domain

import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class InterruptionReason {
    AudioFocusLoss,
    IncomingCall,
    StorageFull,
    UnknownError,
}

sealed interface AudioRecorderState {
    data object Recording : AudioRecorderState

    data class Paused(
        val reason: InterruptionReason? = null,
    ) : AudioRecorderState

    data class Idle(
        val reason: InterruptionReason? = null,
    ) : AudioRecorderState
}

interface AudioRecorder : AutoCloseable {
    val state: StateFlow<AudioRecorderState>
    val durationMillis: StateFlow<Long>
    val audioWaveform: StateFlow<List<Float>>

    fun start(outputFile: File)

    fun pause()

    fun resume()

    fun stop()

    fun getMaxAmplitude(): Int

    override fun close() {
        stop()
    }
}
