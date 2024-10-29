package com.techun.demoemvttpax.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pax.dal.entity.EReaderType
import com.techun.demoemvttpax.domain.models.DataCard
import com.techun.demoemvttpax.domain.usecases.GetBitmapVoucherUseCase
import com.techun.demoemvttpax.domain.usecases.GetEmvUseCase
import com.techun.demoemvttpax.domain.usecases.PaxUseCase
import com.techun.demoemvttpax.domain.usecases.PrintVoucherUseCase
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.enums.CvmResultEnum
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
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils.TXN_TYPE_ICC
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils.TXN_TYPE_PICC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaxViewModel @Inject constructor(
    private val sdkPax: PaxUseCase,
    private val detectCardContract: GetEmvUseCase,
    private val printVoucherUseCase: PrintVoucherUseCase,
    private val getBitmapVoucherUseCase: GetBitmapVoucherUseCase
) : ViewModel() {

    private val _emvTransParam: MutableLiveData<DataState<EmvTransParam>> = MutableLiveData()
    private val _sdkState: MutableLiveData<DataState<Boolean>> = MutableLiveData()
    private val _getEmvUseCase: MutableLiveData<DataState<DataCard>> = MutableLiveData()
    private val _checkTransResult: MutableLiveData<DataState<String>> = MutableLiveData()
    private val _printVocher: MutableLiveData<DataState<Boolean>> = MutableLiveData()
    private val _getBitmapVocher: MutableLiveData<DataState<Bitmap>> = MutableLiveData()

    val emvTransParam: LiveData<DataState<EmvTransParam>> = _emvTransParam
    val sdkState: LiveData<DataState<Boolean>> get() = _sdkState
    val getEmv: LiveData<DataState<DataCard>> get() = _getEmvUseCase
    val checkTransResult: LiveData<DataState<String>> get() = _checkTransResult
    val printVocher: LiveData<DataState<Boolean>> get() = _printVocher
    val getBitmapVocher: LiveData<DataState<Bitmap>> get() = _getBitmapVocher


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


    fun getBitmapVocher(
        voucher: String
    ) = viewModelScope.launch {
        getBitmapVoucherUseCase(voucher).onEach { dataState ->
            _getBitmapVocher.value = dataState
        }.launchIn(viewModelScope)
    }

    fun printVocher(voucher: Bitmap) = viewModelScope.launch {
        printVoucherUseCase(voucher).onEach { dataState ->
            _printVocher.value = dataState
        }.launchIn(viewModelScope)
    }

    fun processCvm(cardDetails: TransResult, currentTxnType: Int) = viewModelScope.launch {
        val currentTxnCVMResult = cardDetails.cvmResult
        if (currentTxnCVMResult === CvmResultEnum.CVM_NO_CVM) {
            //1.check trans result
            _checkTransResult.value = DataState.Success("CVM_NO_CVM")
            _checkTransResult.value = DataState.Finished
        } else if (currentTxnCVMResult === CvmResultEnum.CVM_SIG) {
            //1.signature process 2.check trans result
            _checkTransResult.value = DataState.Success("CVM_SIG")
            _checkTransResult.value = DataState.Finished
        } else if (currentTxnCVMResult === CvmResultEnum.CVM_ONLINE_PIN) {
            if (currentTxnType == TXN_TYPE_PICC) {
                //1.online pin process
                // 2.check trans result
                _checkTransResult.value = DataState.Success("PICC_CVM_ONLINE_PIN")
                _checkTransResult.value = DataState.Finished
            } else if (currentTxnType == TXN_TYPE_ICC) {
                //check result
                _checkTransResult.value = DataState.Success("ICC_CVM_ONLINE_PIN")
                _checkTransResult.value = DataState.Finished
            }
        } else if (currentTxnCVMResult === CvmResultEnum.CVM_ONLINE_PIN_SIG) {
            if (currentTxnType == TXN_TYPE_PICC) {
                //picc no this cvm
            } else if (currentTxnType == TXN_TYPE_ICC) {
                //1.signature process 2.check trans result
                _checkTransResult.value = DataState.Success("ICC_CVM_ONLINE_PIN_SIG")
                _checkTransResult.value = DataState.Finished
            }
        } else if (currentTxnCVMResult === CvmResultEnum.CVM_OFFLINE_PIN) { //contact trans
            //1.check trans result
            _checkTransResult.value = DataState.Success("CVM_OFFLINE_PIN")
            _checkTransResult.value = DataState.Finished
        } else if (currentTxnCVMResult === CvmResultEnum.CVM_CONSUMER_DEVICE) { //contactless trans
            //1.restart detect(tap) card and transaction
            _checkTransResult.value =
                DataState.Error(Exception("CVM_CONSUMER_DEVICE = See phone, Please tap phone"))
            _checkTransResult.value = DataState.Finished
        }
    }


}