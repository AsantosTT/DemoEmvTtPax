package com.techun.demoemvttpax.domain.repository

import android.graphics.Bitmap
import com.techun.demoemvttpax.utils.DataState
import kotlinx.coroutines.flow.Flow

interface PaxRepository {
    suspend fun initSdk(): Flow<DataState<Boolean>>

    suspend fun printer(voucher: Bitmap): DataState<Int>
}