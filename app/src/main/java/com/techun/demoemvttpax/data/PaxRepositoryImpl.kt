package com.techun.demoemvttpax.data

import android.graphics.Bitmap
import com.techun.demoemvttpax.domain.repository.PaxRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.printer.exception.PrinterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PaxRepositoryImpl @Inject constructor(
    private val sdk: TTPaxApi
) : PaxRepository {
    override suspend fun initSdk(): Flow<DataState<Boolean>> {
        return flow {
            emit(DataState.Loading)
            try {
                var isSuccessful = false
                sdk.initPaxSdk({
                    isSuccessful = true
                }, {
                    isSuccessful = false
                })
                emit(DataState.Success(isSuccessful))
                emit(DataState.Finished)
            } catch (e: Exception) {
                emit(DataState.Error(e))
                emit(DataState.Finished)
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun printer(voucher: Bitmap): DataState<Int> {
        return withContext(Dispatchers.Default) {

            try {
                val status = sdk.printer(voucher)
                DataState.Success(status)
            } catch (e: PrinterException) {
                DataState.ErrorPrinter(e)
            }
        }
    }
}