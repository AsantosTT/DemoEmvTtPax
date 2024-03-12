package com.techun.demoemvttpax.domain

import android.content.Context
import com.pax.dal.entity.EReaderType
import com.techun.demoemvttpax.R
import com.techun.demoemvttpax.domain.repository.DetectCardContractRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.CardInfoUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils.TXN_TYPE_ICC
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils.TXN_TYPE_MAG
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils.TXN_TYPE_PICC
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val TAG = "GetEmvUseCase"

class GetEmvUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detectCardContractRepository: DetectCardContractRepository
) {
    suspend operator fun invoke(readerType: EReaderType): Flow<DataState<DetectCardResult>> = flow {
        when (val dataState = detectCardContractRepository.startDetectCard(readerType)) {
            is DataState.Success -> {
                val detectResult = dataState.data
                val readerType = detectResult.readType

                when (readerType) {
                    EReaderType.PICC -> {
                        glStatus.GetInstance().currentReaderType = EReaderType.PICC.eReaderType.toInt()


                        emit(DataState.Success(detectResult))
                        emit(DataState.Finished)
                    }

                    EReaderType.ICC -> {
                        glStatus.GetInstance().currentReaderType = EReaderType.ICC.eReaderType.toInt()

                        emit(DataState.Success(detectResult))
                        emit(DataState.Finished)
                    }

                    EReaderType.MAG -> {
                        println("onMagDetectOK: ${context.getString(R.string.prompt_swipe_card)}")
                        val track2: String = detectResult.track2
                        val pan = CardInfoUtils.getPan(track2)
                        val expiryDate = CardInfoUtils.getExpDate(track2)
                        //add CVM process, such as enter pin or signature and so on.
                        glStatus.GetInstance().PAN = pan
                        glStatus.GetInstance().ExpirationDate = expiryDate
                        glStatus.GetInstance().Track2 = track2
                        glStatus.GetInstance().currentReaderType = EReaderType.MAG.eReaderType.toInt()

                        emit(DataState.Success(detectResult))
                        emit(DataState.Finished)
                    }

                    else -> {
                        emit(DataState.Success(detectResult))
                        emit(DataState.Finished)
                    }
                }
            }

            is DataState.ErrorPrinter -> {
                val exception = dataState.exception
                val errorCode = exception.errCode
                println("$TAG: $errorCode")
                emit(DataState.Error(Exception(exception)))
                emit(DataState.Finished)
            }

            else -> Unit
        }
    }

}