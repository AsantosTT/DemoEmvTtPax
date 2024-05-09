package com.techun.demoemvttpax.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pax.dal.entity.EReaderType
import com.techun.demoemvttpax.domain.GetEmvUseCase
import com.techun.demoemvttpax.domain.PaxUseCase
import com.techun.demoemvttpax.domain.models.DataCard
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.AppDataUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.ConvertHelper
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.MiscUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.IConvert
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayPassAid
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayWaveParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Config
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.contact.EmvAid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaxViewModel @Inject constructor(
    private val sdkPax: PaxUseCase, private val detectCardContract: GetEmvUseCase
) : ViewModel() {

    private val _emvTransParam: MutableLiveData<DataState<EmvTransParam>> = MutableLiveData()
    private val _sdkState: MutableLiveData<DataState<Boolean>> = MutableLiveData()
    private val _getEmvUseCase: MutableLiveData<DataState<DataCard>> = MutableLiveData()

    val emvTransParam: LiveData<DataState<EmvTransParam>> = _emvTransParam

    val sdkState: LiveData<DataState<Boolean>> get() = _sdkState
    val getEmv: LiveData<DataState<DataCard>> get() = _getEmvUseCase


    fun loadEmvTransParam(transType: Byte = 0x00.toByte(), transAmt: Long = 0, otherAmt: Long = 0) {
        val transParam = EmvTransParam()
        try {
            transParam.transType = transType
            transParam.amount = transAmt.toString()
            transParam.amountOther = otherAmt.toString()
            transParam.terminalID = AppDataUtils.getSN()
            transParam.transCurrencyCode = ConvertHelper.getConvert().strToBcd(
                glStatus.GetInstance().CurrencyCode, IConvert.EPaddingPosition.PADDING_LEFT
            )
            transParam.transCurrencyExponent = glStatus.GetInstance().CurrencyExponent
            transParam.transDate = AppDataUtils.getCurrDate()
            transParam.transTime = AppDataUtils.getCurrTime()
            transParam.transTraceNo = MiscUtils.padLeft("1", 8, "0")
            println("Trans: $transParam")
            _emvTransParam.value = DataState.Success(transParam)
        } catch (e: Exception) {
            _emvTransParam.value = DataState.Error(e)
        }

    }

    fun startEmvTrans(readerType: EReaderType, data: EmvTransParam) = viewModelScope.launch {
        detectCardContract.invoke(readerType, data).onEach { dataState ->
            _getEmvUseCase.value = dataState
        }.launchIn(viewModelScope)
    }

    fun sdkInit(
        capkParam: CapkParam,
        emvAidList: ArrayList<EmvAid>,
        emvConfig: Config,
        paywaveParams: PayWaveParam,
        paypassParam: ArrayList<PayPassAid>
    ) = viewModelScope.launch {
        sdkPax(capkParam, emvAidList, emvConfig, paywaveParams, paypassParam).onEach { dataState ->
            _sdkState.value = dataState
        }.launchIn(viewModelScope)
    }
}