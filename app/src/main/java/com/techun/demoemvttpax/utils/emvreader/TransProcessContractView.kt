package com.techun.demoemvttpax.utils.emvreader

import com.techun.demoemvttpax.utils.pinutils.DUKPTResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.TransProcessContract

interface TransProcessContractView : TransProcessContract.View {
    fun onEnterDUKPTPinFinish(dukptResult: DUKPTResult)
}