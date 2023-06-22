package com.techun.demoemvttpax.di

import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.emv_reader.TransProcessPresenter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SDKModule {


    //Presenter
    @Provides
    @Singleton
    fun provideAuthRepository() = TransProcessPresenter()
}