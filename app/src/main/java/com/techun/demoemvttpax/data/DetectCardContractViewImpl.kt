package com.techun.demoemvttpax.data

import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.DetectCardContract
import javax.inject.Inject

class DetectCardContractViewImpl @Inject constructor() : DetectCardContract.View {
    override fun onMagDetectOK(p0: String?, p1: String?, p2: String?, p3: String?) {
        TODO("Not yet implemented")
    }

    override fun onIccDetectOK() {
        TODO("Not yet implemented")
    }

    override fun onPiccDetectOK() {
        TODO("Not yet implemented")
    }

    override fun onDetectError(p0: DetectCardResult.ERetCode?) {
        TODO("Not yet implemented")
    }
}