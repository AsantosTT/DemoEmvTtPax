package com.techun.demoemvttpax.data

import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.TransProcessContract
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import javax.inject.Inject

class TransProcessContractViewsImpl @Inject constructor():TransProcessContract.View {
    override fun onUpdatePinLen(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun getEnteredPin(): String {
        TODO("Not yet implemented")
    }

    override fun onEnterPinFinish(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onStartEnterPin(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun onTransFinish(p0: TransResult?) {
        TODO("Not yet implemented")
    }

    override fun onCompleteTrans(p0: TransResult?) {
        TODO("Not yet implemented")
    }

    override fun onRemoveCard() {
        TODO("Not yet implemented")
    }

    override fun onReadCardOK() {
        TODO("Not yet implemented")
    }
}