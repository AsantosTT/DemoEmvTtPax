package com.techun.demoemvttpax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.techun.demoemvttpax.databinding.ActivityCustomImplementationBinding
import com.techun.demoemvttpax.databinding.ActivityMainBinding
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.TransProcessContract
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.DetectCardContract

class CustomImplementationActivity : AppCompatActivity(),
    TransProcessContract.View,
    DetectCardContract.View {

    private lateinit var binding: ActivityCustomImplementationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =ActivityCustomImplementationBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

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