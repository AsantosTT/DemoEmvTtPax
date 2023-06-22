package com.techun.demoemvttpax.data

import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.TransProcessContract
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import javax.inject.Inject

class TransProcessContractViewsImpl @Inject constructor() : TransProcessContract.View {

    //Pin
    override fun onUpdatePinLen(pin: String?) {
        TODO("Not yet implemented")
    }

    override fun getEnteredPin(): String {
        TODO("Not yet implemented")
    }

    override fun onEnterPinFinish(pinResult: Int) {
        TODO("Not yet implemented")
    }

    override fun onStartEnterPin(prompt: String?) {
        TODO("Not yet implemented")
    }


    //Trans
    override fun onTransFinish(transResult: TransResult?) {
        TODO("Not yet implemented")
    }

    override fun onCompleteTrans(transResult: TransResult?) {
        TODO("Not yet implemented")
    }

    override fun onRemoveCard() {
        TODO("Not yet implemented")
    }

    override fun onReadCardOK() {
        TODO("Not yet implemented")
    }
}