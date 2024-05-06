package com.techun.demoemvttpax.domain

import android.content.Context
import com.pax.dal.entity.EReaderType
import com.pax.jemv.device.DeviceManager
import com.techun.demoemvttpax.R
import com.techun.demoemvttpax.domain.models.DataCard
import com.techun.demoemvttpax.domain.repository.DetectCardContractRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.EmvProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.ClssProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.CardInfoUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DeviceImplNeptune
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val TAG = "GetEmvUseCase"

class GetEmvUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detectCardContractRepository: DetectCardContractRepository
) {
    suspend operator fun invoke(readerType: EReaderType): Flow<DataState<DataCard>> = flow {
        emit(DataState.Loading)
        when (val dataState = detectCardContractRepository.startDetectCard(readerType)) {
            is DataState.Success -> {
                val detectResult = dataState.data
                val readerType = detectResult.readType

                when (readerType) {
                    EReaderType.PICC -> {
                        glStatus.GetInstance().currentReaderType = EReaderType.PICC.eReaderType.toInt()

                        val deviceImplNeptune: DeviceImplNeptune = DeviceImplNeptune.getInstance()
                        DeviceManager.getInstance().setIDevice(deviceImplNeptune)

                        val transResult: TransResult = ClssProcess.getInstance().startTransProcess()

                        emit(DataState.Success(DataCard(dataPiccIcc = transResult)))
                        emit(DataState.Finished)
                    }

                    EReaderType.ICC -> {
                        glStatus.GetInstance().currentReaderType = EReaderType.ICC.eReaderType.toInt()

                        val deviceImplNeptune = DeviceImplNeptune.getInstance()
                        DeviceManager.getInstance().setIDevice(deviceImplNeptune)

                        val transResult: TransResult = EmvProcess.getInstance().startTransProcess()

                        emit(DataState.Success(DataCard(dataPiccIcc = transResult)))
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

                        emit(DataState.Success(DataCard(dataMag = detectResult)))
                        emit(DataState.Finished)
                    }

                    else -> {
                        emit(DataState.Error(Exception("Error in type")))
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