package com.techun.demoemvttpax.data

import android.os.ConditionVariable
import com.pax.jemv.clcommon.RetCode
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.emv_reader.AppSelectTask
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.CandidateAID
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.IEmvTransProcessListener
import javax.inject.Inject

class IEmvTransProcessListenerImpl @Inject constructor() : IEmvTransProcessListener {
    val enterPinCv = ConditionVariable()
    val appSelectCv = ConditionVariable()
    val enterPinRet = 0
    var appSelectRet = 0
    override fun onWaitAppSelect(isFirstSelect: Boolean, candList: List<CandidateAID?>?): Int {
        if (candList.isNullOrEmpty()) {
            return RetCode.EMV_NO_APP
        }
        val selectAppTask = AppSelectTask()
        selectAppTask.registerAppSelectListener { selectRetCode ->
            appSelectRet = selectRetCode
            appSelectCv.open()
        }
        selectAppTask.startSelectApp(isFirstSelect, candList)
        appSelectCv.block()
        return appSelectRet
    }

    override fun onCardHolderPwd(p0: Boolean, p1: Int, p2: ByteArray?): Int {
        println("onCardHolderPwd, current thread " + Thread.currentThread().name + ", id:" + Thread.currentThread().id)
//        enterPinProcess(true, bOnlinePin, leftTimes)
        enterPinCv.block()
        return enterPinRet
    }
}