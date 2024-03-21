package com.techun.demoemvttpax.utils

import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.printer.exception.PrinterException

sealed class DataState<out R> {
    data class Success<out T>(val data: T) : DataState<T>()
    data class Error(val exception: Exception) : DataState<Nothing>()
    data class ErrorPrinter(val exception: PrinterException) : DataState<Nothing>()
    object Loading : DataState<Nothing>()
    object Finished : DataState<Nothing>()
}