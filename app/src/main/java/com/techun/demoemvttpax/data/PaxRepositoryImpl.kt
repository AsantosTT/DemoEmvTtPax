package com.techun.demoemvttpax.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.pax.dal.IPed
import com.pax.dal.entity.DUKPTResult
import com.pax.dal.entity.ECheckMode
import com.pax.dal.entity.EDUKPTPinMode
import com.pax.dal.entity.EPedType
import com.pax.dal.exceptions.PedDevException
import com.techun.demoemvttpax.domain.repository.PaxRepository
import com.techun.demoemvttpax.utils.DataState
import com.techun.demoemvttpax.utils.writeKeys
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.PedApiUtils
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
                        //Add TIK & KSN
                        val tik16Clr = byteArrayOf(
                            0x6A.toByte(),
                            0xC2.toByte(),
                            0x92.toByte(),
                            0xFA.toByte(),
                            0xA1.toByte(),
                            0x31.toByte(),
                            0x5B.toByte(),
                            0x4D.toByte(),
                            0x85.toByte(),
                            0x8A.toByte(),
                            0xB3.toByte(),
                            0xA3.toByte(),
                            0xD7.toByte(),
                            0xD5.toByte(),
                            0x93.toByte(),
                            0x3A.toByte()
                        )
                        val ksn = byteArrayOf(
                            0xff.toByte(),
                            0xff.toByte(),
                            0x98.toByte(),
                            0x76.toByte(),
                            0x54.toByte(),
                            0x32.toByte(),
                            0x10.toByte(),
                            0xE0.toByte(),
                            0x00.toByte(),
                            0x00.toByte()
                        )

                        PedApiUtils.eraseKey()

                        val status = PedApiUtils().writeKeys(
                            groupIndex = 1.toByte(),
                            srcKeyIndex = 0.toByte(),
                            keyValue = tik16Clr,
                            ksn = ksn,
                            checkMode = ECheckMode.KCV_NONE,
                            checkBuf = null as ByteArray?
                        )

                        Log.i(TAG, "initSdk: writeKeys status $status")




                        /*
                                                 val ped = sdk.getDal(context)?.getPed(EPedType.INTERNAL)


                         ped?.let {


                               val verString: Boolean = it.writeTIK(tik16Clr, ksn)

                               if (verString) {
                                   val num = "4391242409331300"
                                   val panArray: ByteArray = num.substring(num.length - 13, num.length - 1).toByteArray()
                                   val dataIn = ByteArray(16)
                                   dataIn[0] = 0x00
                                   dataIn[1] = 0x00
                                   dataIn[2] = 0x00
                                   dataIn[3] = 0x00
                                   System.arraycopy(panArray, 0, dataIn, 4, panArray.size)

                                   val result: DUKPTResult? = it.getDUKPTPin(dataIn)
                                   if (result != null) {
                                       Log.i(
                                           "ss",
                                           "ksn:" + ConvertHelper.getConvert().bcdToStr(result.ksn)
                                       )
                                       Log.i(
                                           "ss",
                                           "Pinblock:" + ConvertHelper.getConvert().bcdToStr(result.result)
                                       )

                                       //ksn Increment
                                       ped.incDUKPTKsn(0x01.toByte())

                                       val verString = it.incDUKPTKsn()
                                       if (verString) {
                                           Log.i(TAG, "initSdk: inDUPKTKsn  success")

                                          val data = com.pax.jemv.clcommon.ByteArray()
                                          val ret = EmvProcess.getInstance().getTlv(0x52, data)

                                           if (ret == RetCode.EMV_OK) {
                                               val dataArr = ByteArray(data.length)
                                               System.arraycopy(data.data, 0, dataArr, 0, data.length)
                                               val firstGacTSI = ConvertHelper.getConvert().bcdToStr(dataArr)
                                               Log.i("getFirstGACTag", "firstGacTSI: $firstGacTSI")
                                           } else {
                                               Log.e(TAG, "initSdk: Fallo")
                                           }

                                           *//*val num = "4391242409331300"
                                        val panArray: ByteArray = num.substring(num.length - 13, num.length - 1).toByteArray()
                                        val dataIn = ByteArray(16)
                                        dataIn[0] = 0x00
                                        dataIn[1] = 0x00
                                        dataIn[2] = 0x00
                                        dataIn[3] = 0x00
                                        System.arraycopy(panArray, 0, dataIn, 4, panArray.size)

                                        val result: DUKPTResult? = it.getDUKPTPin(dataIn)
                                        if (result != null) {
                                            Log.i(
                                                "ssX2",
                                                "ksn:" + ConvertHelper.getConvert().bcdToStr(result.ksn)
                                            )
                                            Log.i(
                                                "ssX2",
                                                "Pinblock:" + ConvertHelper.getConvert().bcdToStr(result.result)
                                            )

                                            //ksn Increment
                                            ped.incDUKPTKsn(0x01.toByte())

                                            val verString = it.incDUKPTKsn()
                                            if (verString) {
                                                Log.i(TAG, "initSdkX2: inDUPKTKsn  success")
                                            } else {
                                                Log.e(TAG, "initSdkX2: inDUPKTKsn  error")
                                            }

                                        } else {
                                            Log.e(TAG, "initSdkX2: Error DUKPTResult")
                                        }
*//*
                                    } else {
                                        Log.e(TAG, "initSdk: inDUPKTKsn  error")
                                    }

                                } else {
                                    Log.e(TAG, "initSdk: Error DUKPTResult")
                                }
                            } else {
                                Log.e(TAG, "initSdk: Error Loading Keys")
                            }

                        } ?: run {
                            Log.e(TAG, "initSdk: Error ped")
                        }*/

                        /*
                                                //54DCBF79AEB970329E97B98651E619CE
                                                //67BC0E979825972CC729FE6246E0F7AB
                                                //D3249731AFAD89F7D96C1D4133225349

                                                val byte_TPK: ByteArray = strToBcd(
                                                    "D3249731AFAD89F7D96C1D4133225349",
                                                    IConvert.EPaddingPosition.PADDING_LEFT
                                                )


                                                val writeResult = writeKey(
                                                    ped!!,
                                                    EPedKeyType.TMK,
                                                    1.toByte(),
                                                    EPedKeyType.TPK,
                                                    1.toByte(),
                                                    byte_TPK,
                                                    ECheckMode.KCV_NONE,
                                                    null
                                                )

                                                Log.i(TAG, "write Key ${if (writeResult) "success" else "failed"}")

                                                val num = "4391242409331300"
                                                val panArray: ByteArray =
                                                    num.substring(num.length - 13, num.length - 1).toByteArray()
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

                                                val result: DUKPTResult =
                                                    ped.getDUKPTPin(1.toByte(), "4", dataIn, EDUKPTPinMode.ISO9564_0, 60000)

                                                Log.e(
                                                    TAG, "X1 -- Generated Pinblock: ${
                                                        ConvertHelper.getConvert().bcdToStr(result.ksn)
                                                    }"
                                                )*/

                        //Increment


                        /*val ksn = ped.getDUKPTKsn(0x01)

                        Log.e(TAG, "KSN: ${ConvertHelper.getConvert().bcdToStr(ksn)}")
*/
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


private fun IPed.incDUKPTKsn(): Boolean {
    try {
        this.incDUKPTKsn(0x01.toByte())
        Log.i(TAG, "incDUKPTKsn")
        return true
    } catch (e: PedDevException) {
        e.printStackTrace()
        Log.e(TAG, "incDUKPTKsn: $e")
        return false
    }
}

private fun IPed.writeTIK(tik16Clr: ByteArray, ksn: ByteArray): Boolean {
    try {
        this.writeTIK(0x01.toByte(), 0x00.toByte(), tik16Clr, ksn, ECheckMode.KCV_NONE, null)
        Log.i(TAG, "writeTIK: Success")
        return true
    } catch (e: PedDevException) {
        e.printStackTrace()
        Log.e(TAG, "writeTIK: $e")
        return false
    }
}

private fun IPed.getDUKPTPin(dataIn: ByteArray): DUKPTResult? {
    try {
        val bytes_ped: DUKPTResult =
            this.getDUKPTPin(1.toByte(), "4", dataIn, EDUKPTPinMode.ISO9564_0_INC, 20000)
        Log.i(TAG, "getDUKPTPin")
        return bytes_ped
    } catch (e: PedDevException) {
        e.printStackTrace()
        Log.e(TAG, "getDUKPTPin: $e")
        return null
    }

}
