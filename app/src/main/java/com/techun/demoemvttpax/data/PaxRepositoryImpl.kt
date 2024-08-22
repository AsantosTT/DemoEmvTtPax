package com.techun.demoemvttpax.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.pax.dal.IPed
import com.pax.dal.entity.ECheckMode
import com.pax.dal.entity.EPedKeyType
import com.pax.dal.entity.EPedType
import com.pax.dal.entity.EPinBlockMode
import com.pax.dal.exceptions.PedDevException
import com.techun.demoemvttpax.domain.repository.PaxRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.ConvertHelper
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.IConvert
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayPassAid
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayWaveParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Config
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.contact.EmvAid
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.printer.exception.PrinterException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject


class PaxRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context, private val sdk: TTPaxApi
) : PaxRepository {
    override suspend fun initSdk(
        capkParam: CapkParam,
        emvAidList: ArrayList<EmvAid>,
        emvConfig: Config,
        paywaveParams: PayWaveParam,
        paypassParam: ArrayList<PayPassAid>
    ): Flow<DataState<Boolean>> {
        return flow {
            emit(DataState.Loading)
            try {
                var isSuccessful = false

                sdk.initPaxSdk(capkParam = capkParam,
                    emvAidList = emvAidList,
                    configParam = emvConfig,
                    paramPayWave = paywaveParams,
                    paramPayPassAids = paypassParam,
                    onSuccess = {

                        val byte_TMK: ByteArray = strToBcd(
                            "D3249731AFAD89F7D96C1D4133225349",
                            IConvert.EPaddingPosition.PADDING_LEFT
                        )
                        //54DCBF79AEB970329E97B98651E619CE

                        //67BC0E979825972CC729FE6246E0F7AB

                        //D3249731AFAD89F7D96C1D4133225349

                        val ped = sdk.getDal(context)?.getPed(EPedType.INTERNAL)

                        val writeResult = writeKey(
                            ped!!,
                            EPedKeyType.TLK,
                            0x00.toByte(),
                            EPedKeyType.TMK,
                            1.toByte(),
                            byte_TMK,
                            ECheckMode.KCV_NONE,
                            null
                        )

                        Log.i(TAG, "write Key Result: $writeResult")

                        val num = "4391242409331300"
                        val panArray: ByteArray = num.substring(num.length - 13, num.length - 1).toByteArray()
                        val dataIn = ByteArray(16)
                        dataIn[0] = 0x00
                        dataIn[1] = 0x00
                        dataIn[2] = 0x00
                        dataIn[3] = 0x00
                        System.arraycopy(panArray, 0, dataIn, 4, panArray.size)

                        val pinblock = getPinBlock(ped, dataIn)


                        Log.e(
                            TAG,
                            "Generated Pinblock: ${ConvertHelper.getConvert().bcdToStr(pinblock)}"
                        )

                        isSuccessful = true
                    },
                    onFeature = {
                        isSuccessful = false
                    })

                emit(DataState.Success(isSuccessful))
                emit(DataState.Finished)
            } catch (e: Exception) {
                emit(DataState.Error(e))
                emit(DataState.Finished)
            }
        }.flowOn(Dispatchers.IO)
    }

    // PED writeKey include TMK,TPK,TAK,TDk
    // ===============================================================================================================
    private fun writeKey(
        ped: IPed,
        srcKeyType: EPedKeyType?,
        srcKeyIndex: Byte,
        destKeyType: EPedKeyType?,
        destkeyIndex: Byte,
        destKeyValue: ByteArray?,
        checkMode: ECheckMode?,
        checkBuf: ByteArray?
    ): Boolean {
        try {
            ped.writeKey(
                srcKeyType,
                srcKeyIndex,
                destKeyType,
                destkeyIndex,
                destKeyValue,
                checkMode,
                checkBuf
            )
            Log.i(TAG, "writeKey: Key successfully loaded")
            return true
        } catch (e: PedDevException) {
            e.printStackTrace()
            Log.e(TAG, "Error writeKey: $e")
        }
        return false
    }

    private fun getPinBlock(ped: IPed?, dataIn: ByteArray?): ByteArray? {
        try {
            val result =
                ped?.getPinBlock(1.toByte(), "0,4,6", dataIn, EPinBlockMode.ISO9564_0, 60000)
            println("getPinBlock")
            return result
        } catch (e: PedDevException) {
            e.printStackTrace()
            println("getPinBlock$e")
            return null
        }
    }


    private fun strToBcd(str: String?, paddingPosition: IConvert.EPaddingPosition?): ByteArray {
        var str = str
        if (str == null || paddingPosition == null) {
            Log.e(TAG, "strToBcd input arg is null")
            throw IllegalArgumentException("strToBcd input arg is null")
        }

        var len = str.length
        val mod = len % 2
        if (mod != 0) {
            str = if (paddingPosition == IConvert.EPaddingPosition.PADDING_RIGHT) {
                str + "0"
            } else {
                "0$str"
            }
            len = str.length
        }
        var abt = ByteArray(len)
        if (len >= 2) {
            len = len / 2
        }
        val bbt = ByteArray(len)
        abt = str.toByteArray()
        var j: Int
        var k: Int
        for (p in 0 until str.length / 2) {
            j = if ((abt[2 * p] >= 'a'.code.toByte()) && (abt[2 * p] <= 'z'.code.toByte())) {
                abt[2 * p] - 'a'.code.toByte() + 0x0a
            } else if ((abt[2 * p] >= 'A'.code.toByte()) && (abt[2 * p] <= 'Z'.code.toByte())) {
                abt[2 * p] - 'A'.code.toByte() + 0x0a
            } else {
                abt[2 * p] - '0'.code.toByte()
            }

            k =
                if ((abt[2 * p + 1] >= 'a'.code.toByte()) && (abt[2 * p + 1] <= 'z'.code.toByte())) {
                    abt[2 * p + 1] - 'a'.code.toByte() + 0x0a
                } else if ((abt[2 * p + 1] >= 'A'.code.toByte()) && (abt[2 * p + 1] <= 'Z'.code.toByte())) {
                    abt[2 * p + 1] - 'A'.code.toByte() + 0x0a
                } else {
                    abt[2 * p + 1] - '0'.code.toByte()
                }

            val a = (j shl 4) + k
            val b = a.toByte()
            bbt[p] = b
        }
        return bbt
    }

    override suspend fun printer(voucher: Bitmap): DataState<Int> {
        return withContext(Dispatchers.Default) {

            try {
                val status = sdk.printer(voucher)
                DataState.Success(status)
            } catch (e: PrinterException) {
                DataState.ErrorPrinter(e)
            }
        }
    }
}