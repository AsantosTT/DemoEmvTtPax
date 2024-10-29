package com.techun.demoemvttpax.utils.emvreader

import android.os.ConditionVariable
import android.os.SystemClock
import android.util.Log
import com.pax.dal.entity.EBeepMode
import com.pax.dal.entity.EPiccRemoveMode
import com.pax.dal.entity.EPiccType
import com.pax.dal.exceptions.PiccDevException
import com.pax.jemv.clcommon.ByteArray
import com.pax.jemv.clcommon.RetCode
import com.pax.jemv.device.DeviceManager
import com.techun.demoemvttpax.utils.incDUKPTKsn
import com.techun.demoemvttpax.utils.pinutils.DUKPTResult
import com.techun.demoemvttpax.utils.pinutils.EnterPinTask
import com.tecnologiatransaccional.ttpaxsdk.App
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.emv_reader.AppSelectTask
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.BasePresenter
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.TransProcessContract
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvProcessParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.IStatusListener
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.CandidateAID
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.EmvProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.IEmvTransProcessListener
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.ClssProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.IClssStatusListener
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.IssuerRspData
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.CardInfoUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DeviceImplNeptune
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EEnterPinType
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EnterPinResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.LogUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.PedApiUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Config

class TransProcessPresenter : BasePresenter<TransProcessContractView?>(), TransProcessContract.Presenter {
    private val enterPinCv = ConditionVariable()
    private val appSelectCv = ConditionVariable()
    private var enterPinRet = 0
    private var appSelectRet = 0
    private var enterPinTask: EnterPinTask? = null
    private var needShowRemoveCard = true

    private val emvTransProcessListener: IEmvTransProcessListener =
        object : IEmvTransProcessListener {
            override fun onWaitAppSelect(
                isFirstSelect: Boolean, candList: List<CandidateAID>
            ): Int {
                if (candList == null || candList.size == 0) {
                    return RetCode.EMV_NO_APP
                }
                val selectAppTask = AppSelectTask()
                selectAppTask.registerAppSelectListener { selectRetCode: Int ->
                    appSelectRet = selectRetCode
                    appSelectCv.open()
                }
                selectAppTask.startSelectApp(isFirstSelect, candList)
                appSelectCv.block()
                return appSelectRet
            }

            override fun onCardHolderPwd(
                bOnlinePin: Boolean, leftTimes: Int, pinData: kotlin.ByteArray?
            ): Int {
                LogUtils.w(
                    TAG,
                    "onCardHolderPwd, current thread " + Thread.currentThread().name + ", id:" + Thread.currentThread().id
                )
                enterPinProcess(true, bOnlinePin, leftTimes)
                enterPinCv.block()
                return enterPinRet
            }
        }

    private val clssStatusListener = IClssStatusListener {
        while (!isCardRemove) {
            if (needShowRemoveCard) {
                App.instance.runOnUiThread(object : Runnable {
                    override fun run() {
                        mView!!.onRemoveCard()
                        needShowRemoveCard = false
                        App.instance.killThread { this.run() }
                    }
                })
            }
        }
    }

    private val statusListener = IStatusListener {
        App.instance.runOnUiThread(object : Runnable {
            override fun run() {
                mView!!.onReadCardOK()
                App.instance.killThread { this.run() }
            }
        })
        Sdk.instance?.getDal(App.mBaseApplication)?.sys?.beep(EBeepMode.FREQUENCE_LEVEL_5, 100)
        SystemClock.sleep(750) //blue yellow green clss light remain lit for a minimum of approximately 750ms
    }

    val isCardRemove: Boolean
        get() {
            try {
                LogUtils.d(
                    TAG, "isCardRemove"
                )
                Sdk.instance?.getDal(App.mBaseApplication)?.getPicc(EPiccType.INTERNAL)
                    ?.remove(EPiccRemoveMode.REMOVE, 0.toByte())
            } catch (e: PiccDevException) {
                LogUtils.e(
                    TAG, "isCardRemove : " + e.message
                )
                return false
            }
            return true
        }

    private fun enterPinProcess(isICC: Boolean, bOnlinePin: Boolean, leftTimes: Int) {
        if (enterPinTask != null) {
            enterPinTask!!.unregisterListener()
            enterPinTask = null
        }
        LogUtils.w(
            TAG, "isOnlinePin:$bOnlinePin,leftTimes:$leftTimes"
        )
        enterPinTask = EnterPinTask()
        var enterPinPrompt = "Please Enter PIN"
        if (bOnlinePin) {
            var pan = ""
            if (isICC) {
                val byteArray = ByteArray()
                EmvProcess.getInstance().getTlv(0x57, byteArray)
                val strTrack2 = CardInfoUtils.getTrack2FromTag57(byteArray.data)
                pan = CardInfoUtils.getPan(strTrack2)
            } else {
                val strTrack2 = ClssProcess.getInstance().track2
                LogUtils.d(
                    TAG, "ClssProcess getTrack2() = $strTrack2"
                )
                pan = CardInfoUtils.getPan(strTrack2)
                LogUtils.d(
                    TAG, "ClssProcess getPan() = $pan"
                )
            }

            enterPinTask!!.setOnlinePan(pan)
            enterPinTask!!.setEnterPinType(EEnterPinType.ONLINE_PIN)
        } else {
            enterPinPrompt = "$enterPinPrompt($leftTimes)"
            enterPinTask!!.setEnterPinType(EEnterPinType.OFFLINE_PCI_MODE)
        }
        mView!!.onUpdatePinLen("")

        enterPinTask!!.registerListener(object : EnterPinTask.IEnterPinListener {
            override fun onUpdatePinLen(pin: String?) {
                mView!!.onUpdatePinLen(pin)
            }

            override val enteredPin: String
                get() = mView!!.enteredPin

            override fun onEnterPinFinish(enterPinResult: EnterPinResult?) {
                if (enterPinResult != null) {
                    if (enterPinResult.ret == EnterPinResult.RET_OFFLINE_PIN_READY) {
                        enterPinRet = EnterPinResult.RET_SUCC
                    } else {
                        enterPinRet = enterPinResult.ret
                        mView!!.onEnterPinFinish(enterPinRet)
                        LogUtils.i(
                            TAG, "onEnterPinFinish, enterPinRet:$enterPinRet"
                        )
                    }
                    enterPinCv.open()
                } else {
                    Log.e(TAG, "onEnterPinFinish: Error")
                }

            }

            override fun onEnterDUKPTPinFinish(dukptResult: DUKPTResult?) {
                if (dukptResult != null) {
                    if (dukptResult.codeResult == EnterPinResult.RET_OFFLINE_PIN_READY) {
                        enterPinRet = EnterPinResult.RET_SUCC
                    } else {
                        enterPinRet = dukptResult.codeResult
                        mView!!.onEnterDUKPTPinFinish(dukptResult)
                        LogUtils.i(TAG, "onEnterPinFinish, enterPinRet:$enterPinRet")
                    }

                    val valueReturnOfIncKsn = PedApiUtils().incDUKPTKsn()

                    Log.i("onEnterDUKPTPinFinish", "incDUKPTKsn: Value($valueReturnOfIncKsn)")

                    enterPinCv.open()
                } else {
                    Log.e(TAG, "onEnterPinFinish: Error")
                }
            }
        })
        mView!!.onStartEnterPin(enterPinPrompt)
        enterPinTask!!.startEnterDUKPPin()
    }

    override fun startEmvTrans() {
        EmvProcess.getInstance().registerEmvProcessListener(emvTransProcessListener)
        val deviceImplNeptune = DeviceImplNeptune.getInstance()
        DeviceManager.getInstance().setIDevice(deviceImplNeptune)

        App.instance.runInBackground(object : Runnable {
            override fun run() {
                val transResult = EmvProcess.getInstance().startTransProcess()
                App.instance.runOnUiThread {
                    if (isViewAttached) {
                        mView!!.onTransFinish(transResult)
                        App.instance.killExecutor()
                        App.instance.killThread(Runnable { this.run() })
                    }
                }
            }
        })
    }

    override fun startClssTrans() {
        val deviceImplNeptune = DeviceImplNeptune.getInstance()
        DeviceManager.getInstance().setIDevice(deviceImplNeptune)
        App.instance.runInBackground {
            val transResult = ClssProcess.getInstance().startTransProcess()
            App.instance.runOnUiThread(object : Runnable {
                override fun run() {
                    if (isViewAttached) {
                        mView!!.onTransFinish(transResult)
                        App.instance.killExecutor()
                        App.instance.killThread(this)
                    }
                }
            })
        }
    }

    override fun preTrans(transParam: EmvTransParam, needContact: Boolean) {
        App.instance.runInBackground {
            var ret = 0
            val configParam: Config? = Sdk.instance?.paramManager?.configParam
            val capkParam: CapkParam? = Sdk.instance?.paramManager?.capkParam

            if (needContact) {
                ret = EmvProcess.getInstance().preTransProcess(
                    EmvProcessParam.Builder(transParam, configParam, capkParam)
                        .setEmvAidList(Sdk.instance?.paramManager!!.emvAidList).create()
                )
                LogUtils.d(
                    TAG, "transPreProcess, emv ret:$ret"
                )
            }

            ret = ClssProcess.getInstance().preTransProcess(
                EmvProcessParam.Builder(transParam, configParam, capkParam)
                    .setPayPassAidList(Sdk.instance?.paramManager!!.payPassAidList)
                    .setPayWaveParam(Sdk.instance?.paramManager!!.payWaveParam).create()
            )

            LogUtils.d(
                TAG, "transPreProcess, clss ret:$ret"
            )

            needShowRemoveCard = true

            ClssProcess.getInstance().registerClssStatusListener(clssStatusListener)
            ClssProcess.getInstance().registerStatusListener(statusListener)
            App.instance.killExecutor()
        }
    }

    override fun startMagTrans() {
    }

    override fun startOnlinePin() {
        // for contactless online pin process
        Log.i(TAG, "startOnlinePin: ")
        enterPinProcess(false, true, 0)
    }

    override fun completeEmvTrans(issuerRspData: IssuerRspData) {
        val deviceImplNeptune = DeviceImplNeptune.getInstance()
        DeviceManager.getInstance().setIDevice(deviceImplNeptune)
        App.instance.runInBackground {
            val transResult = EmvProcess.getInstance().completeTransProcess(issuerRspData)
            App.instance.runOnUiThread(object : Runnable {
                override fun run() {
                    if (isViewAttached) {
                        mView!!.onCompleteTrans(transResult)
                        App.instance.killExecutor()
                        App.instance.killThread { this.run() }
                    }
                }
            })
        }
    }

    override fun completeClssTrans(issuerRspData: IssuerRspData) {
        val deviceImplNeptune = DeviceImplNeptune.getInstance()
        DeviceManager.getInstance().setIDevice(deviceImplNeptune)
        App.instance.runInBackground {
            val transResult = ClssProcess.getInstance().completeTransProcess(issuerRspData)
            App.instance.runOnUiThread(object : Runnable {
                override fun run() {
                    if (isViewAttached) {
                        mView!!.onCompleteTrans(transResult)
                        App.instance.killExecutor()
                        App.instance.killThread(Runnable { this.run() })
                    }
                }
            })
        }
    }

    companion object {
        private const val TAG = "TransProcessPresenter"
    }
}
