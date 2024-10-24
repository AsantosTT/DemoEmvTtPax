package com.techun.demoemvttpax.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.pax.dal.IPed
import com.pax.dal.entity.ECheckMode
import com.pax.dal.entity.EDUKPTPinMode
import com.pax.dal.entity.EPedType
import com.pax.dal.exceptions.PedDevException
import com.techun.demoemvttpax.data.TAG
import com.techun.demoemvttpax.utils.pinutils.DUKPTResult
import com.tecnologiatransaccional.ttpaxsdk.App
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.PedApiUtils


inline fun <reified T : Activity> Activity.goToActivity(
    noinline init: Intent.() -> Unit = {}, finish: Boolean = false
) {
    val intent = Intent(this, T::class.java)
    intent.init()
    startActivity(intent)
    if (finish) finish()
}


fun Activity.toast(text: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, length).show()
}


/**
 * PINBlock Input the PIN on PED,and use the PINkey of DUKPT to calculate the PINBlock.
 * EPedType.INTERNAL,EPedType.EXTERNAL_TYPEA is supported.
 *
 * @param groupIndex - [1~100] DUKPT key group id
 * @param expPinLen - Enumeration of 0-12.The enter password string with legal length. Application
 * enumerates of all possible lengths of PIN. ","will be used to separate each number of length.
 * If no PIN,or 4 or 6 digits of PIN are allowed, the string will be set as "0,4,6". 0 means that
 * no PIN is required, and pressing "Enter" will return. For type-c, the legal Enumeration is 4-12
 * @param dataIn -
 *            When mode=EDUKPTPinMode.ISO9564_0_INC, DataIn is the 16 bytes primary account number after shifting.
 *            When mode=EDUKPTPinMode.ISO9564_1_INC/EDUKPTPinMode.ISO9564_1,DataIn is ignored. The interface uses random numbers to fill PINBlock internally.
 *            When mode=EDUKPTPinMode.ISO9564_2_INC/EDUKPTPinMode.ISO9564_2,DataIn is the 16 bytes primary account number after shifting.
 *            When mode=EDUKPTPinMode.HKEPS_INC, dataIn is ISN [6 Bytes, ASCII code]
 *            For type-c, dataIn is the 16 bytes primary account number after shifting.
 * @param mode - EDUKPTPinMode
 * Not used for type-c
 * @param timeoutMs -
 * The timeout of PIN entry [unit:ms] Maximum is 300000ms.
 * 0: No timeout time, not doing timeout control for PED.
 *
 * @return com.techun.demoemvttpax.utils.pinutils.DUKPTResult?
 *
 * @throws PedDevException
 */
fun PedApiUtils.getDDUKPTResult(
    groupIndex: Byte = 1.toByte(),
    expPinLen: String = "4,5,6,7,8,9,10,11,12",
    dataIn: String,
    mode: EDUKPTPinMode = EDUKPTPinMode.ISO9564_0,
    timeout: Int
): DUKPTResult? {
    val supportBypass = true

    Sdk.instance?.getDal(App.mBaseApplication)?.let { dal ->
        val ped: IPed = dal.getPed(EPedType.INTERNAL)
        var pinLen = expPinLen
        if (supportBypass) {
            pinLen = "0,$expPinLen"
        }

        ped.setKeyboardLayoutLandscape(false)
        val pedDUKPTPinResult = ped.getDUKPTPin(
            groupIndex, pinLen, dataIn.toByteArray(), mode, timeout
        )

        return DUKPTResult(
            codeResult = 0, ksnUsed = pedDUKPTPinResult.ksn, pinblock = pedDUKPTPinResult.result
        )
    } ?: run {
        return null
    }
}

/**
 * Write in TIK, and can check the key correction by using KCV. EPedType.INTERNAL,
 * EPedType.EXTERNAL_TYPEA is supported.
 *
 * @param groupIndex - [1~100]DUKPT key group id
 * @param srcKeyIndex - [0~1] The index of the key protecting the key. 0 means writing in plaintext.
 * @param keyValue - The plaintext or ciphertext of TIK. When srcKeyIdx is 0, it means writing plain
 * text. The DUKPT algorithm supports keys with a length of 8/16 bytes.
 * @param ksn - Point to KSN initialization.
 * @param checkMode - reference writeKey(com.pax.dal.entity.EPedKeyType, byte, com.pax.dal.entity.EPedKeyType,
 * byte, byte[], com.pax.dal.entity.ECheckMode, byte[]) checkMode
 * @param checkBuf - reference writeKey(com.pax.dal.entity.EPedKeyType, byte, com.pax.dal.entity.EPedKeyType,
 * byte, byte[], com.pax.dal.entity.ECheckMode, byte[]) the information about checkBuf
 *
 * @return Boolean flag
 *
 * @throws PedDevException
 */
fun PedApiUtils.writeKeys(
    groupIndex: Byte,
    srcKeyIndex: Byte,
    keyValue: ByteArray?,
    ksn: ByteArray?,
    checkMode: ECheckMode? = ECheckMode.KCV_NONE,
    checkBuf: ByteArray?
): Boolean {

    Sdk.instance?.getDal(App.mBaseApplication)?.let { dal ->
        dal.getPed(EPedType.INTERNAL)
            .writeTIK(groupIndex, srcKeyIndex, keyValue, ksn, checkMode, checkBuf)
        return true
    } ?: run {
        return false
    }

}

/**
 * A single DUKPT key corresponding to a KSN can only be used at most 256 times, further use of
 * that key will result in EPedDevException.PED_ERR_DUKPT_NEED_INC_KSN after reaching the maxium
 * times. So please increase KSN before number of use of the key exceeding the maximum times.
 * EPedType.INTERNAL only is supported.
 *
 * @param groupIndex - [1~100] DUKPT group ID
 *
 * @return Boolean
 *
 * @throws PedDevException
 */
fun PedApiUtils.incDUKPTKsn(groupIndex: Byte = 0x01.toByte()): Boolean {
    Sdk.instance?.getDal(App.mBaseApplication)?.let { dal ->
        try {
            dal.getPed(EPedType.INTERNAL).incDUKPTKsn(groupIndex)
            Log.i(TAG, "incDUKPTKsn")
            return true
        } catch (e: PedDevException) {
            e.printStackTrace()
            Log.e(TAG, "incDUKPTKsn: $e")
            return false
        }
    } ?: run {
        return false
    }
}