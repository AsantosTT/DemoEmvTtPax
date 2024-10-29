package com.techun.demoemvttpax.domain.models

import com.pax.dal.entity.EReaderType
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult

data class DataCard(
    val readerType: EReaderType? = null,
    val dataPiccIcc: TransResult? = null,
    val dataMag: DetectCardResult? = null
)