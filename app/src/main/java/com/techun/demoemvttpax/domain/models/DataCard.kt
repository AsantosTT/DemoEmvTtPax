package com.techun.demoemvttpax.domain.models

import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult

data class DataCard(val dataPiccIcc: TransResult? = null, val dataMag: DetectCardResult? = null)