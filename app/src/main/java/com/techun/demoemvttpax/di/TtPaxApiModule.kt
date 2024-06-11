package com.techun.demoemvttpax.di

import android.content.Context
import com.pax.gl.page.PaxGLPage
import com.techun.demoemvttpax.data.IEmvTransProcessListenerImpl
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.IEmvTransProcessListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object TtPaxApiModule {
    @Provides
    @Singleton
    fun provideTtPaxApi(@ApplicationContext context: Context) = TTPaxApi(context)

    @Provides
    @Singleton
    fun provideGlPage(@ApplicationContext context: Context) = PaxGLPage.getInstance(context)

    @Provides
    @Singleton
    fun provideICardReaderHelper(@ApplicationContext context: Context, ttPaxApi: TTPaxApi) =
        ttPaxApi.getDal(context)!!.cardReaderHelper


}