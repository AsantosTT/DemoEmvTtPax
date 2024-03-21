package com.techun.demoemvttpax.domain.repository

import com.pax.dal.entity.EReaderType
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import kotlinx.coroutines.flow.Flow

interface DetectCardContractRepository {

    suspend fun startDetectCard(readType: EReaderType?): DataState<DetectCardResult>

}