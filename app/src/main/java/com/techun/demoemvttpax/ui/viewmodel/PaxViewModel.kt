package com.techun.demoemvttpax.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pax.dal.entity.EReaderType
import com.techun.demoemvttpax.domain.GetEmvUseCase
import com.techun.demoemvttpax.domain.PaxUseCase
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaxViewModel @Inject constructor(
    private val sdkPax: PaxUseCase, private val detectCardContract: GetEmvUseCase
) : ViewModel() {

    private val _sdkState: MutableLiveData<DataState<Boolean>> = MutableLiveData()
    private val _getEmvUseCase: MutableLiveData<DataState<DetectCardResult>> = MutableLiveData()

    val sdkState: LiveData<DataState<Boolean>> get() = _sdkState
    val getEmv: LiveData<DataState<DetectCardResult>> get() = _getEmvUseCase

    fun startEmvTrans(readerType: EReaderType) = viewModelScope.launch {
        detectCardContract(readerType).onEach { dataState ->
            _getEmvUseCase.value = dataState
        }.launchIn(viewModelScope)
    }

    fun sdkInit() = viewModelScope.launch {
        sdkPax().onEach { dataState ->
            _sdkState.value = dataState
        }.launchIn(viewModelScope)
    }
}