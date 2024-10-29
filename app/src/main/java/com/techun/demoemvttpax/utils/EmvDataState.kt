package com.techun.demoemvttpax.utils

sealed class EmvDataState<out R> {
    data class Success<out T>(val data: T) : EmvDataState<T>()
    data class Error(val exception: Exception) : EmvDataState<Nothing>()
    object Loading : EmvDataState<Nothing>()
    object StartEnterPin : EmvDataState<Nothing>()
    object RemoveCard : EmvDataState<Nothing>()
    object ReadCardOK : EmvDataState<Nothing>()
    object Finished : EmvDataState<Nothing>()
}