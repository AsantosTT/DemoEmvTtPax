package com.techun.demoemvttpax.data

import android.graphics.Bitmap
import com.techun.demoemvttpax.domain.repository.PaxRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayPassAid
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayWaveParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Config
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.contact.EmvAid
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
    override suspend fun initSdk(
        capkParam: CapkParam,
        emvAidList: ArrayList<EmvAid>,
        emvConfig: Config,
        paywaveParams: PayWaveParam,
        paypassParam: ArrayList<PayPassAid>
    ): Flow<DataState<Boolean>> {
        return flow {
            emit(DataState.Loading)
            try {
                var isSuccessful = false
                sdk.initPaxSdk(
                    capkParam = capkParam,
                    emvAidList = emvAidList,
                    configParam = emvConfig,
                    paramPayWave = paywaveParams,
                    paramPayPassAids = paypassParam,
                    onSuccess = {
                    isSuccessful = true
                    },
                    onFeature = {
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