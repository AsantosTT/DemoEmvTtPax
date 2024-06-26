package com.techun.demoemvttpax.domain.usecases

import android.content.Context
import com.pax.dal.entity.EReaderType
import com.techun.demoemvttpax.domain.models.DataCard
import com.techun.demoemvttpax.domain.repository.DetectCardContractRepository
import com.techun.demoemvttpax.domain.repository.TransProcessContractRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.CardInfoUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetEmvUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdk: TTPaxApi,
    private val detectCardContractRepository: DetectCardContractRepository,
    private val transProcessContractRepository: TransProcessContractRepository
) {
    suspend operator fun invoke(
        readerType: EReaderType, data: EmvTransParam
    ): Flow<DataState<DataCard>> = flow {
        emit(DataState.Loading)

        transProcessContractRepository.preTrans(data, true)
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
}