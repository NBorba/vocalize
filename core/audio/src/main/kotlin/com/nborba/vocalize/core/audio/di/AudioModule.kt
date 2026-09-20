package com.nborba.vocalize.core.audio.di

import com.nborba.vocalize.core.audio.MediaRecorderAudioRecorder
import com.nborba.vocalize.core.audio.domain.AudioRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AudioModule {
    @Binds
    @Singleton
    abstract fun bindAudioRecorder(impl: MediaRecorderAudioRecorder): AudioRecorder
}
