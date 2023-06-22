package com.techun.demoemvttpax.data

import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.DetectCardContract
import javax.inject.Inject

class DetectCardContractViewImpl @Inject constructor() : DetectCardContract.View {
    override fun onMagDetectOK(
        pan: String?,
        expiryDate: String?,
        Track1: String?,
        Track2: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun onIccDetectOK() {
        TODO("Not yet implemented")
    }

    override fun onPiccDetectOK() {
        TODO("Not yet implemented")
    }

    override fun onDetectError(errorCode: DetectCardResult.ERetCode?) {
        TODO("Not yet implemented")
    }
}