package com.techun.demoemvttpax.utils.pinutils

import android.util.Log
import com.pax.dal.IPed
import com.pax.dal.entity.EKeyCode
import com.pax.dal.entity.EPedType
import com.pax.dal.exceptions.EPedDevException
import com.pax.dal.exceptions.PedDevException
import com.pax.jemv.clcommon.RetCode
import com.techun.demoemvttpax.utils.getDDUKPTResult
import com.tecnologiatransaccional.ttpaxsdk.App
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.CardInfoUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EEnterPinType
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EnterPinResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.PedApiUtils
import java.util.Objects

/**
 *
 */
class EnterPinTask {
    private val ped: IPed = Objects.requireNonNull<IPed>(
        Sdk.instance?.getDal(App.mBaseApplication)?.getPed(EPedType.INTERNAL)
    )


    private var enterPinType: EEnterPinType? = null
    private var onlinePan = ""
    private var listener: IEnterPinListener? = null

    fun registerListener(listener: IEnterPinListener?): EnterPinTask {
        this.listener = listener
        return this
    }

    fun setEnterPinType(enterPinType: EEnterPinType?): EnterPinTask {
        this.enterPinType = enterPinType
        return this
    }

    fun setOnlinePan(pan: String): EnterPinTask {
        this.onlinePan = pan
        return this
    }

    fun unregisterListener() {
        this.listener = null
    }

    fun startEnterPin() {
        if (this.listener == null) {
            return
        }

        Log.i(TAG, "onlinePan:$onlinePan,enterPinType:$enterPinType")

        App.mBaseApplication.runInBackground {
            if (enterPinType == EEnterPinType.ONLINE_PIN) {
                val pan = CardInfoUtils.getPanBlock(onlinePan, CardInfoUtils.X9_8_WITH_PAN)
                enterOnlinePin(pan)
            } else if (enterPinType == EEnterPinType.OFFLINE_PCI_MODE) {
                enterOfflinePin()
            }
        }
    }

    fun startEnterDUKPPin() {
        if (this.listener == null) {
            return
        }

        Log.i(TAG, "onlinePan:$onlinePan,enterPinType:$enterPinType")

        App.mBaseApplication.runInBackground {
            if (enterPinType == EEnterPinType.ONLINE_PIN) {
                val pan = CardInfoUtils.getPanBlock(onlinePan, CardInfoUtils.X9_8_WITH_PAN)
                enterOnlineDUKPTPin(pan)
            } else if (enterPinType == EEnterPinType.OFFLINE_PCI_MODE) {
                enterOfflinePin()
            }
        }
    }

    private fun enterOfflinePin() {
        try {
            ped.setIntervalTime(1, 1)
            ped.setInputPinListener { eKeyCode: EKeyCode ->
                var temp = listener!!.enteredPin
                if (eKeyCode == EKeyCode.KEY_CLEAR) {
                    temp = ""
                } else if (eKeyCode == EKeyCode.KEY_CANCEL) {
                    ped.setInputPinListener(null)
                    listener!!.onEnterPinFinish(EnterPinResult(EnterPinResult.RET_CANCEL))
                    return@setInputPinListener
                } else if (eKeyCode == EKeyCode.KEY_ENTER) {
                    if (temp.length > 3 || temp.isEmpty()) {
                        ped.setInputPinListener(null)
                        listener!!.onEnterPinFinish(EnterPinResult(EnterPinResult.RET_SUCC))
                        return@setInputPinListener
                    }
                } else {
                    temp += "*"
                }
                listener!!.onUpdatePinLen(temp)
            }
            listener!!.onEnterPinFinish(EnterPinResult(EnterPinResult.RET_OFFLINE_PIN_READY))
        } catch (e: PedDevException) {
            Log.i(TAG, "enterOfflinePin:$e")
            if (e.errCode == EPedDevException.PED_ERR_INPUT_TIMEOUT.errCodeFromBasement) {
                listener!!.onEnterPinFinish(EnterPinResult(EnterPinResult.RET_TIMEOUT))
            } else {
                listener!!.onEnterPinFinish(EnterPinResult(e.errCode))
            }
        }
    }

    private fun enterOnlinePin(panBlock: String) {
        val pinResult = EnterPinResult()
        try {
            ped.setIntervalTime(1, 1)
            ped.setInputPinListener { eKeyCode: EKeyCode ->
                var temp: String
                if (eKeyCode == EKeyCode.KEY_CLEAR) {
                    temp = ""
                } else if (eKeyCode == EKeyCode.KEY_ENTER || eKeyCode == EKeyCode.KEY_CANCEL) {
                    // do nothing
                    return@setInputPinListener
                } else {
                    temp = listener!!.enteredPin
                    temp += "*"
                }
                listener!!.onUpdatePinLen(temp)
            }
            val pinBlock = PedApiUtils.getPinBlock(panBlock, 60 * 1000)
            if (pinBlock == null || pinBlock.isEmpty()) {
                pinResult.ret = RetCode.EMV_NO_PASSWORD
            } else {
                pinResult.ret = EnterPinResult.RET_SUCC
            }
        } catch (e: PedDevException) {
            Log.i(TAG, "EnterPinTask:" + e.errCode)
            if (e.errCode == EPedDevException.PED_ERR_INPUT_CANCEL.errCodeFromBasement) {
                pinResult.ret = EnterPinResult.RET_CANCEL
            } else if (e.errCode == EPedDevException.PED_ERR_INPUT_TIMEOUT.errCodeFromBasement) {
                pinResult.ret = EnterPinResult.RET_TIMEOUT
            } else if (e.errCode == EPedDevException.PED_ERR_NO_KEY.errCodeFromBasement) {
                pinResult.ret = EnterPinResult.RET_NO_KEY
            } else {
                pinResult.ret = RetCode.EMV_RSP_ERR
            }
        } finally {
            ped.setInputPinListener(null)
        }
        if (listener != null) {
            listener!!.onEnterPinFinish(pinResult)
        }
    }

    /**
     *
     */
    private fun enterOnlineDUKPTPin(panBlock: String) {
        var dukptResult = DUKPTResult()

        try {
            ped.setIntervalTime(1, 1)
            ped.setInputPinListener { eKeyCode: EKeyCode ->
                var temp: String?
                if (eKeyCode == EKeyCode.KEY_CLEAR) {
                    temp = ""
                } else if (eKeyCode == EKeyCode.KEY_ENTER || eKeyCode == EKeyCode.KEY_CANCEL) {
                    // do nothing
                    return@setInputPinListener
                } else {
                    temp = listener!!.enteredPin
                    temp += "*"
                }
                listener!!.onUpdatePinLen(temp)
            }
            val result = PedApiUtils().getDDUKPTResult(dataIn = panBlock, timeout = 60 * 1000)
            if (result != null) {
                dukptResult = result
                dukptResult.codeResult =
                    if (dukptResult.result == null || dukptResult.result!!.isEmpty()) {
                        RetCode.EMV_NO_PASSWORD
                    } else {
                        EnterPinResult.RET_SUCC
                    }
            }

        } catch (e: PedDevException) {

            Log.i(TAG, "EnterPinTask:" + e.errCode)
            dukptResult.codeResult = when (e.errCode) {
                EPedDevException.PED_ERR_INPUT_CANCEL.errCodeFromBasement -> EnterPinResult.RET_CANCEL
                EPedDevException.PED_ERR_INPUT_TIMEOUT.errCodeFromBasement -> EnterPinResult.RET_TIMEOUT
                EPedDevException.PED_ERR_NO_KEY.errCodeFromBasement -> EnterPinResult.RET_NO_KEY
                else -> RetCode.EMV_RSP_ERR
            }
        } finally {
            ped.setInputPinListener(null)
        }
        if (listener != null) {
            listener!!.onEnterDUKPTPinFinish(dukptResult)
        }
    }

    interface IEnterPinListener {
        fun onUpdatePinLen(pin: String?)

        val enteredPin: String

        fun onEnterPinFinish(enterPinResult: EnterPinResult?)

        fun onEnterDUKPTPinFinish(dukptResult: DUKPTResult?)
    }

    companion object {
        private const val TAG = "EnterPinTask"
    }
}
