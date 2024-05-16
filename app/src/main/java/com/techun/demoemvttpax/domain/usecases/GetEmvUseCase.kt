package com.techun.demoemvttpax.domain.usecases

import android.content.Context
import android.os.SystemClock
import com.pax.dal.entity.EBeepMode
import com.pax.dal.entity.EPiccRemoveMode
import com.pax.dal.entity.EPiccType
import com.pax.dal.entity.EReaderType
import com.pax.dal.exceptions.PiccDevException
import com.pax.jemv.device.DeviceManager
import com.techun.demoemvttpax.domain.models.DataCard
import com.techun.demoemvttpax.domain.repository.DetectCardContractRepository
import com.techun.demoemvttpax.domain.repository.TransProcessContractRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.IStatusListener
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.EmvProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.ClssProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.IClssStatusListener
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.CardInfoUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DeviceImplNeptune
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class GetEmvUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdk: TTPaxApi,
    private val detectCardContractRepository: DetectCardContractRepository,
    private val transProcessContractRepository: TransProcessContractRepository
) {
//    private var needShowRemoveCard: Boolean = true

    suspend operator fun invoke(
        readerType: EReaderType, data: EmvTransParam
    ): Flow<DataState<DataCard>> = flow {
        emit(DataState.Loading)

        transProcessContractRepository.preTrans(data, true)

        /*val clssStatusListener = IClssStatusListener {
            CoroutineScope(Dispatchers.IO).launch {
                while (!isCardRemove()) {
                    if (needShowRemoveCard) {
                        //Remove Card
                        needShowRemoveCard = false
                        emit(DataState.RemoveCard)
                    }
                }
            }
        }

        val statusListener = IStatusListener {
            CoroutineScope(Dispatchers.IO).launch {
                emit(DataState.ReadCardOK)
                sdk.getDal(context)?.sys?.beep(EBeepMode.FREQUENCE_LEVEL_5, 100)
                SystemClock.sleep(750) //blue yellow green clss light remain lit for a minimum of approximately 750ms
            }
        }

        needShowRemoveCard = true

        ClssProcess.getInstance().registerClssStatusListener(clssStatusListener)
        ClssProcess.getInstance().registerStatusListener(statusListener)*/

        /*  when (val temp = transProcessContractRepository.preTrans(data, true)) {
              is DataState.ReadCardOK -> {
                  emit(DataState.ReadCardOK)
              }
              is DataState.RemoveCard -> {
                  emit(DataState.RemoveCard)
              }
              is DataState.Error -> {
                  emit(DataState.Error(temp.exception))
              }

              else -> Unit
          }*/

        when (val dataState = detectCardContractRepository.startDetectCard(readerType)) {
            is DataState.Success -> {
                val detectResult = dataState.data
                when (val currentReaderType = detectResult.readType) {
                    EReaderType.PICC -> {
                        println("onPiccDetectOK")

                        glStatus.GetInstance().currentReaderType =
                            EReaderType.PICC.eReaderType.toInt()

                        when (val transResult = transProcessContractRepository.startClssTrans()) {
                            is DataState.Success -> {
                                emit(
                                    DataState.Success(
                                        DataCard(
                                            readerType = currentReaderType,
                                            dataPiccIcc = transResult.data
                                        )
                                    )
                                )
                                emit(DataState.Finished)
                            }

                            is DataState.Error -> {

                                emit(DataState.Error(transResult.exception))
                                emit(DataState.Finished)
                            }

                            else -> Unit
                        }

                    }

                    EReaderType.ICC -> {
                        println("onIccDetectOK")

                        glStatus.GetInstance().currentReaderType =
                            EReaderType.ICC.eReaderType.toInt()

                        when (val transResult = transProcessContractRepository.startEmvTrans()) {
                            is DataState.Success -> {
                                emit(
                                    DataState.Success(
                                        DataCard(
                                            readerType = currentReaderType,
                                            dataPiccIcc = transResult.data
                                        )
                                    )
                                )
                                emit(DataState.Finished)
                            }

                            is DataState.Error -> {

                                emit(DataState.Error(transResult.exception))
                                emit(DataState.Finished)
                            }

                            else -> Unit
                        }

                    }

                    EReaderType.MAG -> {
                        println("onMagDetectOK")
                        val track2: String = detectResult.track2
                        val pan = CardInfoUtils.getPan(track2)
                        val expiryDate = CardInfoUtils.getExpDate(track2)
                        //add CVM process, such as enter pin or signature and so on.
                        glStatus.GetInstance().PAN = pan
                        glStatus.GetInstance().ExpirationDate = expiryDate
                        glStatus.GetInstance().Track2 = track2
                        glStatus.GetInstance().currentReaderType =
                            EReaderType.MAG.eReaderType.toInt()

                        emit(
                            DataState.Success(
                                DataCard(
                                    readerType = currentReaderType, dataMag = detectResult
                                )
                            )
                        )
                        emit(DataState.Finished)
                    }

                    else -> {
                        emit(DataState.Error(Exception("Error in type")))
                        emit(DataState.Finished)
                    }
                }
            }

            is DataState.Error -> {
                emit(DataState.Error(dataState.exception))
                emit(DataState.Finished)
            }

            else -> Unit
        }
    }


/*    private fun isCardRemove(): Boolean {
        return try {
            sdk.getDal(context)?.getPicc(EPiccType.INTERNAL)
                ?.remove(EPiccRemoveMode.REMOVE, 0.toByte())
            true
        } catch (e: PiccDevException) {
            println("isCardRemove : ${e.message}")
            false
        }
    }*/
}