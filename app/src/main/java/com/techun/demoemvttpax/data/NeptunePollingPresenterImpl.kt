package com.techun.demoemvttpax.data

import android.content.Context
import com.pax.dal.ICardReaderHelper
import com.pax.dal.entity.EPiccType
import com.pax.dal.entity.EReaderType
import com.pax.dal.entity.PollingResult
import com.pax.dal.exceptions.IccDevException
import com.pax.dal.exceptions.MagDevException
import com.pax.dal.exceptions.PiccDevException
import com.techun.demoemvttpax.domain.repository.DetectCardContractRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.LogUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val TAG = "NeptunePollingPresenterImpl"

class NeptunePollingPresenterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardReaderHelper: ICardReaderHelper,
    private val sdk: TTPaxApi
) : DetectCardContractRepository {

    private fun close(flag: Byte) {
        if (flag.toInt() and 0x01 != 0) {
            try {
                println("$TAG: closeReader mag")
                sdk.getDal(context)!!.mag.close()
            } catch (e: MagDevException) {
                println("$TAG: ${e.message}")
            }
        }
        if (flag.toInt() and 0x02 != 0) {
            try {
                println("$TAG: closeReader icc")
                sdk.getDal(context)!!.icc.close(0x00.toByte())
            } catch (e: IccDevException) {
                println("$TAG: ${e.message}}")
            }
        }
        val test = flag.toInt() and 0x04
        if (test != 0) {
            try {
                println("$TAG: closeReader picc")
                sdk.getDal(context)!!.getPicc(EPiccType.INTERNAL).close()
            } catch (e: PiccDevException) {
                println("$TAG: ${e.message}}")
            }
        }
    }

    override suspend fun startDetectCard(readType: EReaderType?): DataState<DetectCardResult> {
        return withContext(Dispatchers.Default) {
            val detectResult = DetectCardResult()
            try {
                println("$TAG start polling")

                val pollingResult = cardReaderHelper.polling(readType, 15 * 1000)
                if (pollingResult.operationType == PollingResult.EOperationType.OK) {
                    detectResult.retCode = DetectCardResult.ERetCode.OK
                    detectResult.readType = pollingResult.readerType
                    detectResult.track2 = pollingResult.track2
                } else if (pollingResult.operationType == PollingResult.EOperationType.TIMEOUT) {
                    detectResult.retCode = DetectCardResult.ERetCode.TIMEOUT
                } else if (pollingResult.operationType == PollingResult.EOperationType.CANCEL) {
                    detectResult.retCode = DetectCardResult.ERetCode.CANCEL
                }
                println("NeptunePollingPresenterImpl: polling end,code :${detectResult.retCode} ,type:${detectResult.readType}")


                if (detectResult.retCode == DetectCardResult.ERetCode.OK) {
                    val readTypeRet = detectResult.readType
                    when (readTypeRet) {
                        EReaderType.PICC -> {
                            DataState.Success(detectResult)
                        }

                        EReaderType.ICC -> {
                            DataState.Success(detectResult)
                        }

                        EReaderType.MAG -> {
                            val track2 = detectResult.track2
                            val track1 = detectResult.track1
                            println("NeptunePollingPresenterImpl: detectSuccProcess, track: $track2")
                            DataState.Success(detectResult)
                        }

                        else -> DataState.Success(detectResult)
                    }
                } else {
                    DataState.Error(Exception(detectResult.retCode.toString()))
                }

            } catch (e: MagDevException) {
                LogUtils.w(TAG, "polling error:$e")
                detectResult.retCode = DetectCardResult.ERetCode.ERR_OTHER
                DataState.Error(e)
            } catch (e: IccDevException) {
                LogUtils.w(TAG, "polling error:$e")
                detectResult.retCode = DetectCardResult.ERetCode.ERR_OTHER
                DataState.Error(e)
            } catch (e: PiccDevException) {
                LogUtils.w(TAG, "polling error:$e")
                detectResult.retCode = DetectCardResult.ERetCode.ERR_OTHER
                DataState.Error(e)
            } finally {
                cardReaderHelper.stopPolling()
                var closeByte: Byte = 0x07
                if (detectResult.readType == EReaderType.PICC) {
                    closeByte =
                        (0x03 and readType!!.eReaderType.toInt()).toByte() //close mag and icc
                } else if (detectResult.readType == EReaderType.ICC) {
                    closeByte =
                        (0x05 and readType!!.eReaderType.toInt()).toByte() //close picc and mag
                } else if (detectResult.readType == EReaderType.MAG) {
                    closeByte =
                        (0x06 and readType!!.eReaderType.toInt()).toByte() //close icc and picc
                }
                close(closeByte)
                DataState.Error(Exception(""))
            }
        }
    }
}