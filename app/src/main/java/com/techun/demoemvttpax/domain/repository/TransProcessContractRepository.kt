package com.techun.demoemvttpax.domain.repository

import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.IssuerRspData
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import kotlinx.coroutines.flow.Flow

interface TransProcessContractRepository {
    suspend fun preTrans(transParam: EmvTransParam?, needContact: Boolean)/*: DataState<Unit>*/

    suspend fun startEmvTrans(): DataState<TransResult>

    suspend fun startClssTrans(): DataState<TransResult>

    suspend fun startMagTrans()

    suspend fun startOnlinePin()

    suspend fun completeEmvTrans(issuerRspData: IssuerRspData?)

    suspend fun completeClssTrans(issuerRspData: IssuerRspData?)
}