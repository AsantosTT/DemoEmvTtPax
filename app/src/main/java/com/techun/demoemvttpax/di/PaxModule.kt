package com.techun.demoemvttpax.di

import android.content.Context
import com.pax.dal.ICardReaderHelper
import com.techun.demoemvttpax.data.NeptunePollingPresenterImpl
import com.techun.demoemvttpax.data.PaxRepositoryImpl
import com.techun.demoemvttpax.data.TransProcessContractImpl
import com.techun.demoemvttpax.domain.repository.DetectCardContractRepository
import com.techun.demoemvttpax.domain.repository.PaxRepository
import com.techun.demoemvttpax.domain.repository.TransProcessContractRepository
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object PaxModule {
    @Provides
    @Singleton
    fun providePaxRepository(sdk: TTPaxApi): PaxRepository = PaxRepositoryImpl(sdk)


    @Provides
    @Singleton
    fun provideDetectCardContractRepository(
        @ApplicationContext app: Context, cardReaderHelper: ICardReaderHelper, sdk: TTPaxApi
    ): DetectCardContractRepository = NeptunePollingPresenterImpl(app, cardReaderHelper, sdk)

    @Provides
    @Singleton
    fun provideTransProcessContractRepository(
        @ApplicationContext app: Context, sdk: TTPaxApi
    ): TransProcessContractRepository = TransProcessContractImpl(app, sdk)
}