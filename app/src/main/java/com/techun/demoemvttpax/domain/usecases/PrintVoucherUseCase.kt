package com.techun.demoemvttpax.domain.usecases

import android.graphics.Bitmap
import com.techun.demoemvttpax.domain.repository.PaxRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.TAG
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PrintVoucherUseCase @Inject constructor(
    private val paxRepository: PaxRepository
) {

    suspend operator fun invoke(bitmap: Bitmap): Flow<DataState<Boolean>> = flow {
        when (val dataState = paxRepository.printer(bitmap)) {
            is DataState.Success -> {
                val test = dataState.data
                println("Printer Code: $test")
                emit(DataState.Success(true))
                emit(DataState.Finished)
            }

            is DataState.ErrorPrinter -> {
                val exception = dataState.exception
                val errorCode = exception.errCode
                println("$TAG: $errorCode")
                emit(DataState.Error(Exception(errorPrinterInfo(errorCode))))
                emit(DataState.Finished)
            }

            else -> Unit
        }
    }

    private fun errorPrinterInfo(errorCode: Int): String {
        return when (errorCode) {
            0 -> "Imprimiendo..."
            1 -> "La impresora está ocupada"
            2 -> "Impresora sin papel"
            3 -> "El formato del paquete de datos de impresión da error"
            4 -> "Mal funcionamiento de la impresora"
            8 -> "Impresora Calentada "
            9 -> "El voltaje de la impresora es demasiado bajo."
            240 -> "La impresora no ha terminado "
            252 -> "PrinterBody no ha instalado la biblioteca de fuentes"
            254 -> "El paquete de datos es demasiado largo. "
            else -> {
                "Error desconocido"
            }
        }
    }


}
