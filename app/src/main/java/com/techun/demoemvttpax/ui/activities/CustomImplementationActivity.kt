package com.techun.demoemvttpax.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pax.dal.entity.EReaderType
import com.pax.gl.page.IPage
import com.pax.gl.page.PaxGLPage
import com.pax.jemv.clcommon.ByteArray
import com.pax.jemv.clcommon.RetCode
import com.techun.demoemvttpax.R
import com.techun.demoemvttpax.databinding.ActivityCustomImplementationBinding
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.emv_reader.TransProcessPresenter
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.TagsTable
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.EmvProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.ClssProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.EOnlineResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.enums.CvmResultEnum
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.enums.TransResultEnum
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.AppDataUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.ConvertHelper
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.Device
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EMVUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.MiscUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.NeptunePollingPresenter
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.ScreenUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.TimeRecordUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.TrackUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.DetectCardContract
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.IConvert
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.printer.exception.PrinterException
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomImplementationActivity : AppCompatActivity(), DetectCardContract.View,
    View.OnClickListener {

    //Printer
    lateinit var iPaxGLPage: PaxGLPage

    //SDK
    lateinit var sdkTTPax: TTPaxApi

    //Emv
    private var pinResult: Int = 0
    private var pinText: TextView? = null
    private var mEnterPinPopWindow: PopupWindow? = null
    private var currTransResultEnum: TransResultEnum? = null
    private var currentTxnCVMResult: CvmResultEnum? = null
    private var currTransResultCode = RetCode.EMV_OK
    private val bottomView: View? = null
    private val readMSRCardOnly = 0
    private var isSecondTap = false
    private var convert: IConvert = ConvertHelper.getConvert()
    private var currentTxnType: Int = Utils.TXN_TYPE_ICC
    private var transType: Byte = 0
    private var transAmt: Long = 0
    private var otherAmt: Long = 0
    private var readerType: EReaderType? = null
    private var transProcessPresenter: TransProcessPresenter? = null
    private var transParam = EmvTransParam()
    private var detectPresenter: NeptunePollingPresenter? = null

    private lateinit var binding: ActivityCustomImplementationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomImplementationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sdkTTPax = TTPaxApi(this)

        //Init PaxGLPage Libary
        iPaxGLPage = PaxGLPage.getInstance(this)

        //Init SDK
        sdkTTPax.initPaxSdk(onSuccess = {
            //Exitoso
            Utils.logsUtils("PAX is working correctly")
        }, onFeature = {
            println("Error: $it")
        })

        binding.btnTestPaxPrinter.setOnClickListener(this)
        binding.btnTestCustomPrinter.setOnClickListener(this)
        binding.btnTestEmv.setOnClickListener(this)


    }

    //Config Trans
    private fun initEmvTransaction(amount: Long, readType: EReaderType = EReaderType.MAG) {
        transType = 0x00.toByte()
        transAmt = amount
        val otherAmount = ("0".toDouble() * 100).toLong()
        otherAmt = otherAmount

        glStatus.GetInstance().tranEMVTags.Clear()
        glStatus.GetInstance().tranEMVResponseTags.Clear()

        var tmp = readType.toString()

        if (tmp.isEmpty()) tmp = EReaderType.MAG.toString()

        readerType = EReaderType.valueOf(tmp)

        startDetectCard(readerType!!)
    }

    private fun startDetectCard(readType: EReaderType) {
        TimeRecordUtils.clearTimeRecordList()

        if (detectPresenter != null) {
            detectPresenter!!.stopDetectCard()
            detectPresenter!!.detachView()
            detectPresenter!!.closeReader()
            detectPresenter = null
        }

        detectPresenter = NeptunePollingPresenter()
        detectPresenter!!.attachView(this)
        detectPresenter!!.startDetectCard(readType)
    }


    private fun transPreProcess(isNeedContact: Boolean) {
        try {
            transParam = EmvTransParam()
            Utils.logsUtils(
                "transType: ${
                    ConvertHelper.getConvert().bcdToStr(byteArrayOf(transType))
                },int val: $transType"
            )
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
            transProcessPresenter!!.preTrans(transParam, isNeedContact)
        } catch (e: IllegalArgumentException) {
            Utils.logsUtils(e.message!!, 1)
        }
    }

    private fun startTransaction() {
        detectPresenter!!.closeReader()
        transPreProcess(false)
        startDetectCard(EReaderType.PICC)
    }

    private fun stopDetectCard() {
        if (detectPresenter != null) {
            detectPresenter!!.stopDetectCard()
            detectPresenter!!.detachView()
            detectPresenter!!.closeReader()
            detectPresenter = null
        }

        if (transProcessPresenter != null) {
            transProcessPresenter!!.detachView()
            transProcessPresenter = null
        }
    }

    private fun processTransResult(transResult: TransResult) {
        when {
            (currTransResultEnum === TransResultEnum.RESULT_FALLBACK) -> {
                //contact
                Device.beepErr()
                Utils.logsUtils(getString(R.string.prompt_fallback_swipe_card))
                startDetectCard(EReaderType.MAG)
                // onMagDetectOk will callback
            }

            (currTransResultEnum === TransResultEnum.RESULT_CLSS_SEE_PHONE) -> {
                //contactless
                //PICC return  USE_CONTACT 1.restart detect(insert/swipe) card and transaction
                Device.beepPrompt()
                startClssTransAgain()
            }

            (currTransResultEnum === TransResultEnum.RESULT_CLSS_TRY_ANOTHER_INTERFACE || transResult.resultCode == RetCode.CLSS_USE_CONTACT) -> {
                //contactless
                Device.beepErr()
                Utils.logsUtils(getString(R.string.prompt_try_another_interface))
                startDetectCard(EReaderType.ICC)
            }

            (currTransResultEnum === TransResultEnum.RESULT_TRY_AGAIN) -> {
                //contactless
                //PICC return  USE_CONTACT 1.restart detect card and transaction
                Device.beepErr()
                startClssTransAgain()
            }

            (transResult.resultCode == RetCode.ICC_CMD_ERR) -> {
                //Se dat cuando se desliza una tarjeta Contactless
                Device.beepErr()
                Utils.logsUtils(getString(R.string.error_reading_card))
                startDetectCard(readerType!!)
            }

            (transResult.resultCode == RetCode.EMV_DENIAL || transResult.resultCode == RetCode.CLSS_DECLINE) -> {
                //to result page to get tag95 and tag 9b to find the reason of deciline
                processFinishTransaction()
            }

            else -> {
                onDetectError(DetectCardResult.ERetCode.ERR_OTHER)
//                errorFinishedTransaction("Error code: ${transResult.resultCode}")
            }
        }
    }

    private fun processFinishTransaction() {
        GlobalScope.launch {
            when (glStatus.GetInstance().TransactionResult) {
                TransResultEnum.RESULT_OFFLINE_APPROVED -> offlineProccess(
                    "Title: ${
                        getString(
                            R.string.Declined
                        )
                    }\n${getString(R.string.CardDeclinedTransaction)}"
                )

                TransResultEnum.RESULT_ONLINE_DENIED -> offlineProccess(
                    "Title: ${
                        getString(
                            R.string.Declined
                        )
                    }\n${getString(R.string.HostDeclinedTransaction)}, ${glStatus.GetInstance().ResponseCode}"
                )

                TransResultEnum.RESULT_ONLINE_CARD_DENIED, TransResultEnum.RESULT_OFFLINE_DENIED -> offlineProccess(
                    "Title: ${getString(R.string.Declined)}\n${getString(R.string.CardDeclinedTransaction)}"
                )

                TransResultEnum.RESULT_ONLINE_APPROVED -> {
                    //TODO: Imprimir Voucher, guardar la transaccion en el Log
                    // glStatus.GetIntance() contiene todos los datos de la transaccion.
                    // De acuerdo al glStatus.GetInstance().currentTxnCVMResult solicitar la firma
                    val sCVM: String = when (glStatus.GetInstance().currentTxnCVMResult) {
                        CvmResultEnum.CVM_NO_CVM -> "No requiere Firma"
                        CvmResultEnum.CVM_OFFLINE_PIN -> "Pin Offline Verificado\nNo requiere firma"
                        CvmResultEnum.CVM_ONLINE_PIN -> "Pin Online Verificado\nNo requiere firma"
                        CvmResultEnum.CVM_SIG -> "Se Requiere Firmar"
                        CvmResultEnum.CVM_ONLINE_PIN_SIG -> "Pin Online Verificado\nSe Requiere Firmar"
                        CvmResultEnum.CVM_CONSUMER_DEVICE -> "Verificado en Dispositivo\nNo requiere firma"
                        else -> ""
                    }
                    Utils.logsUtils("sCVM: $sCVM")
                    offlineProccess(
                        "Title: ${getString(R.string.Approved)}\nAuth:${
                            getString(
                                R.string.Auth
                            )
                        }\nAuthCode:${glStatus.GetInstance().AuthCode}"
                    )
                }

                TransResultEnum.RESULT_EMV_FAIL -> offlineProccess(
                    "Title: ${getString(R.string.EMVProcessFail)}\n${
                        getString(
                            R.string.FinishingEMVTran
                        )
                    }"
                )

                TransResultEnum.RESULT_UNABLE_ONLINE -> offlineProccess(
                    "Title: ${getString(R.string.comms_fail)}\n${
                        getString(
                            R.string.no_response_from_server
                        )
                    }"
                )

                else -> offlineProccess(
                    "Title: ${getString(R.string.TranFailed)}\n${
                        getString(
                            R.string.UnknownError
                        )
                    }"
                )
            }
        }
    }

    private fun proccessOnlineResponse() {
        if (transProcessPresenter != null) {
            when (currentTxnType) {
                Utils.TXN_TYPE_ICC -> {
                    transProcessPresenter!!.completeEmvTrans(glStatus.GetInstance().issuerRspData)
                }

                Utils.TXN_TYPE_PICC or Utils.TXN_TYPE_MAG -> {
                    proccessEmvComplete()
                }
            }
        } else {
            onDetectError(DetectCardResult.ERetCode.ERR_OTHER)
        }
    }

    private fun extractTag(Tag: Int, AddToEMVTags: Boolean): kotlin.ByteArray? {
        var tagdata: kotlin.ByteArray? = null
        val byteArray = ByteArray()
        var ret = 0
        when (currentTxnType) {
            Utils.TXN_TYPE_ICC -> {
                ret = EmvProcess.getInstance().getTlv(Tag, byteArray)
            }

            Utils.TXN_TYPE_PICC -> {
                ret = ClssProcess.getInstance().getTlv(Tag, byteArray)
            }
        }
        Utils.logsUtils(
            "extractTag: ret: $ret, T: ${MiscUtils.decimalToHex(Tag)},L: ${byteArray.length}, V:${
                ConvertHelper.getConvert().bcdToStr(byteArray.data)
            }"
        )
        if (ret == 0) {
            tagdata = kotlin.ByteArray(byteArray.length)
            System.arraycopy(byteArray.data, 0, tagdata, 0, byteArray.length)
            if (AddToEMVTags) {
                glStatus.GetInstance().tranEMVTags.AddTag(Tag, tagdata)
            }
        }
        return tagdata
    }

    //Response
    private fun processReaderEVMCompleted() {
        progressbar(false)
        //Se agrega la funcionalidad para ordenar el campo 55 que se estara enviando

        val aid = EMVUtils.GetCardBrandByAID(glStatus.GetInstance().AID)
        when (glStatus.GetInstance().currentReaderType) {
            EReaderType.ICC.eReaderType.toInt() -> {
                val visaIcc = resources.getIntArray(R.array.visa_icc)
                val mcIcc = resources.getIntArray(R.array.mc_icc)

                sortTags(if (aid == EMVUtils.CardBrand.VISA) visaIcc else mcIcc)
            }

            EReaderType.PICC.eReaderType.toInt() -> {
                val visaPicc = resources.getIntArray(R.array.visa_picc)
                val mcPicc = resources.getIntArray(R.array.mc_picc)

                sortTags(if (aid == EMVUtils.CardBrand.VISA) visaPicc else mcPicc)
            }
        }
    }

    private fun responseOnline(icc: String) {

        if (icc.isNotEmpty()) {
            val emvTagsResp: String = icc
            val result = glStatus.GetInstance().tranEMVResponseTags.ParseTLV(false, emvTagsResp)
            Utils.logsUtils("emvTagsResp: $result")
        }

        val codAuth: String? = "072745"
        glStatus.GetInstance().AuthCode = codAuth

        if (glStatus.GetInstance().tranEMVResponseTags.ContainsTag(0x91)) {
            val authData = glStatus.GetInstance().tranEMVResponseTags.GetTag(0x91).Value()
            glStatus.GetInstance().issuerRspData.authData = authData //TAG:91
        }

        if (codAuth!!.isNotEmpty()) {
            val authCode: kotlin.ByteArray = ConvertHelper.getConvert().strToByteArray(codAuth)
            glStatus.GetInstance().issuerRspData.authCode = authCode //TAG:89
        }

        val responseCode = ConvertHelper.getConvert().strToByteArray("00")
        glStatus.GetInstance().issuerRspData.respCode = responseCode //TAG:8A

        //ARS 2021-11-11 MCC-MTIP-06 Ingresar los Issuer Scripts
        if (glStatus.GetInstance().tranEMVResponseTags.ContainsTag(0x71)) {
            val scriptData = glStatus.GetInstance().tranEMVResponseTags.GetTag(0x71).asTLV()
            glStatus.GetInstance().issuerRspData.script = scriptData
        } else {
            if (glStatus.GetInstance().tranEMVResponseTags.ContainsTag(0x72)) {
                val scriptData = glStatus.GetInstance().tranEMVResponseTags.GetTag(0x72).asTLV()
                glStatus.GetInstance().issuerRspData.script = scriptData
            }
        }

        val bAprobadoOnline = true
        glStatus.GetInstance().TransactionResult = TransResultEnum.RESULT_ONLINE_DENIED
        var onlineResult: EOnlineResult = EOnlineResult.DENIAL

        if (bAprobadoOnline) {
            onlineResult = EOnlineResult.APPROVE
            if (glStatus.GetInstance().currentReaderType == EReaderType.MAG.eReaderType.toInt()) {
                glStatus.GetInstance().TransactionResult = TransResultEnum.RESULT_ONLINE_APPROVED
            }
        } else {
            glStatus.GetInstance().TransactionResult = TransResultEnum.RESULT_ONLINE_DENIED
        }

        glStatus.GetInstance().issuerRspData.onlineResult = onlineResult.ordinal.toByte()

        when (com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType.fromInt(
            glStatus.GetInstance().currentReaderType
        )) {
            com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType.PICC -> {
                glStatus.GetInstance().TransactionResult = TransResultEnum.RESULT_ONLINE_APPROVED
                //Si se quiere procesar el segundo criptograma en contactless 2ndGAC PICC poner en true;
                glStatus.GetInstance().Need2ndGAC = false
            }

            com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType.ICC -> glStatus.GetInstance().Need2ndGAC =
                true

            com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType.MAG -> {
            }

            else -> throw IllegalStateException("Unexpected value: " + glStatus.GetInstance().currentReaderType)
        }

        proccessOnlineResponse()

    }

    private fun proccessEmvComplete() {
        //Si la transaccion tanto ICC o PICC se realiza con exito, a este metodo se estara accediendo,
        //si hay un error se notificara al metodo onDetectError
        runOnUiThread {
            binding.tvLogs.text = "TRANSACCION FINALIZADA"
            Utils.logsUtils("TRANSACCION FINALIZADA")
            Toast.makeText(this, "TRANSACCION FINALIZADA", Toast.LENGTH_SHORT).show()
        }
    }

    //Metodo para la primera extraccion de los TAGs que se obtienen tanto de la tarjeta como del POS
    private fun onlineProcess() {
        Utils.logsUtils(" >>>  onlineProcess <<<< ")
        Utils.logsUtils(" >>>  PROCESSING_TYPE_ONLINE <<<< ")
        var bSuccess = false
        if (glStatus.GetInstance().currentReaderType == com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType.MAG.eReaderType.toInt()) {
            bSuccess = true
        } else {
            try {
                val pan = extractTag(TagsTable.T_5A_PAN, false)
                if (pan != null) glStatus.GetInstance().PAN = MiscUtils.bcd2Str(pan)
                val expirationdate = extractTag(TagsTable.T_5F24_APP_EXPIRATION_DATE, false)
                if (expirationdate != null) glStatus.GetInstance().ExpirationDate =
                    MiscUtils.bcd2Str(expirationdate)
                val track2 = extractTag(TagsTable.T_57_TRACK2, false)
                if (track2 != null) {
                    glStatus.GetInstance().Track2 = MiscUtils.bcd2Str(track2)
                    if (glStatus.GetInstance().PAN.isEmpty()) {
                        TrackUtils.getPan(glStatus.GetInstance().Track2)
                        TrackUtils.getExpDate(glStatus.GetInstance().Track2)
                    }
                } else {
                    val track1 = extractTag(TagsTable.T_56_TRACK1, false)
                    if (track1 != null) {
                        glStatus.GetInstance().Track1 = MiscUtils.bcd2Str(track1)
                    }
                }
                val dfname = extractTag(TagsTable.T_84_DEDICATED_FILENAME, true)
                if (dfname != null) {
                    glStatus.GetInstance().AID = MiscUtils.bytes2String(dfname)
                    if (EMVUtils.GetCardBrandByAID(glStatus.GetInstance().AID) === EMVUtils.CardBrand.MASTERCARD) {
                        //Generalmente solo se envía para tarjetas MC
                        glStatus.GetInstance().tranEMVTags.AddTag(
                            TagsTable.T_84_DEDICATED_FILENAME, dfname
                        )
                    }
                } else {
                    Utils.logsUtils("onlineProcess: TAG - 84 is null")
                }
                val panseqno = extractTag(TagsTable.T_5F34_PAN_SEQ_NO, false)

                //El pan sequence number es opcional, si viene hay que enviarlo , si no no debe enviarse.
                if (panseqno != null) {
                    if (panseqno.isNotEmpty()) {
                        glStatus.GetInstance().PANSeqNo = panseqno[0]
                        glStatus.GetInstance().tranEMVTags.AddTag(
                            TagsTable.T_5F34_PAN_SEQ_NO, panseqno
                        )
                    }
                }

                //AESC 2023-02-03 Al utilizar Visa En contactless el Kernel no esta regresando el TAG 9F35
                var tagdata = extractTag(TagsTable.T_9F35_L2_KERNEL, true)
                if (tagdata == null) {
                    val T9F35 = convert.strToBcd("22", IConvert.EPaddingPosition.PADDING_LEFT)
                    Utils.addManualTag(TagsTable.T_9F35_L2_KERNEL, T9F35)
                }

                tagdata = extractTag(TagsTable.T_5F2A_CURRENCY_CODE, true)
                Utils.logsUtils(
                    "KEY= ${TagsTable.T_5F2A_CURRENCY_CODE} VALUE= ${
                        MiscUtils.bytes2HexStr(
                            tagdata
                        )
                    }"
                )


                //2020-08-23 En contactless el Kernel no esta regresando el TAG 5F2A de MC
                if (tagdata == null) {
                    val T5F2A = convert.strToBcd("0320", IConvert.EPaddingPosition.PADDING_LEFT)
                    Utils.addManualTag(TagsTable.T_5F2A_CURRENCY_CODE, T5F2A)
                }

                tagdata = extractTag(TagsTable.T_82_AIP, true)
                tagdata = extractTag(TagsTable.T_95_TVR, true)
                if (tagdata != null) glStatus.GetInstance().TVR = MiscUtils.bcd2Str(tagdata)
                tagdata = extractTag(TagsTable.T_9A_TRANS_DATE, true)
                tagdata = extractTag(TagsTable.T_9C_TRANS_TYPE, true)
                tagdata = extractTag(TagsTable.T_9F02_AMOUNT, true)
                tagdata = extractTag(TagsTable.T_9F03_AMOUNT_OTHER, true)

                //2020-08-23 En contactless el Kernel no esta regresando el TAG 9F03 . Si es mandatorio se envia en ceros
                if (tagdata == null) {
                    val T9F03 =
                        convert.strToBcd("000000000000", IConvert.EPaddingPosition.PADDING_LEFT)
                    Utils.addManualTag(TagsTable.T_9F03_AMOUNT_OTHER, T9F03)
                }
                tagdata = extractTag(TagsTable.T_9F10_ISSUER_APP_DATA, true)
                tagdata = extractTag(TagsTable.T_9F1A_COUNTRY_CODE, true)
                //2020-08-23 En contactless el Kernel no esta regresando el TAG 9F1A
                if (tagdata == null) {
                    val T9F1A = convert.strToBcd("0320", IConvert.EPaddingPosition.PADDING_LEFT)
                    Utils.addManualTag(TagsTable.T_9F1A_COUNTRY_CODE, T9F1A)
                }
                tagdata = extractTag(TagsTable.T_9F66_TTQ, true)
                tagdata = extractTag(TagsTable.T_9F1E_INTER_DEV_NUM, true)

                //2020-08-23 En contactless el Kernel no esta regresando el TAG 9F1E //Numero de serie del POS
                //2021-04-29 En CHIP regresa la palabra Terminal
                //2021-04-29 En CONTACTLESS trae la expresion ....C q @
                if (tagdata == null) {
                    val inforPOS = Device.paramInternos()
                    val snPOS: String = AppDataUtils.getSN()
                    val snHexa: String = MiscUtils.str2Hex(snPOS)
                    val T9F1E = convert.strToBcd(snHexa, IConvert.EPaddingPosition.PADDING_LEFT)
                    Utils.addManualTag(TagsTable.T_9F1E_INTER_DEV_NUM, T9F1E)
                }
                tagdata = extractTag(TagsTable.T_9F26_APP_CRYPTO, true)
                if (tagdata != null) glStatus.GetInstance().Crypto = MiscUtils.bcd2Str(tagdata)
                tagdata = extractTag(TagsTable.T_9F27_CRYPTO, true)
                tagdata = extractTag(TagsTable.T_9F33_TERMINAL_CAPABILITY, true)
                tagdata = extractTag(TagsTable.T_9F36_ATC, true)
                tagdata = extractTag(TagsTable.T_9F37_UNPREDICTABLE_NUMBER, true)
                tagdata = extractTag(TagsTable.T_9F41_TRAN_SEQUENCE_COUNTER, true)

                //2020-08-23 En contactless el Kernel no esta regresando el TAG 9F41
                if (tagdata == null) {
                    val T9F41 = convert.strToBcd(
                        MiscUtils.padLeft("5", 8, "0"), IConvert.EPaddingPosition.PADDING_LEFT
                    )
                    Utils.addManualTag(TagsTable.T_9F41_TRAN_SEQUENCE_COUNTER, T9F41)
                }

                //TODO: 4F
                val T4F = extractTag(TagsTable.T_4F_CAPK_RID, false)
                if (T4F != null) {
                    glStatus.GetInstance().AID = MiscUtils.bytesToString(T4F)
                    Utils.logsUtils("AID: ${glStatus.GetInstance().AID}")
                }
                val mTag9F34 = extractTag(TagsTable.T_9F34_CVM_RESULTS, true)
                if (mTag9F34 != null) {
                    val cardType: EMVUtils.CardBrand =
                        EMVUtils.GetCardBrandByAID(glStatus.GetInstance().AID)
                    if (cardType === EMVUtils.CardBrand.MASTERCARD) {
                        val amount9f34: Long = (1500 * 100).toLong()
                        val T9F34 = convert.strToBcd(
                            if (amount9f34 > 36999) "010002" else "3F0002",
                            IConvert.EPaddingPosition.PADDING_LEFT
                        )
                        Utils.addManualTag(TagsTable.T_9F34_CVM_RESULTS, T9F34)
                    }
                }
                tagdata = extractTag(TagsTable.T_4F_CAPK_RID, true)
                tagdata = extractTag(TagsTable.T_9F6E_FORM_FACTOR_INDICATOR, true)

                //TODO: T_8E_CVM VALIDAR SI UTILIZA PIN
                val T8E = extractTag(TagsTable.T_8E_CVM, false)
                if (T8E != null) {
                    val temp = MiscUtils.bytes2HexStr(T8E)
                    val validacion = temp.substring(16, 20)
                    val isPin = validacion == "0201"
                    glStatus.GetInstance().TERMINAL_CAPABILITY = if (isPin) "0" else "1"
                }

                //TODO: 9F10
                val t9f10 = extractTag(TagsTable.T_9F10_ISSUER_APP_DATA, false)
                if (t9f10 != null) glStatus.GetInstance().PINCARD = MiscUtils.bytesToString(t9f10)

                //TODO: 5F20
                val chname = extractTag(TagsTable.T_5F20_CARDHOLDER_NAME, false)
                if (chname != null) glStatus.GetInstance().CardHolderName =
                    MiscUtils.bytes2String(chname)

                //TODO: 50
                val appLabel = extractTag(TagsTable.T_50_APP_LABEL, false)
                if (appLabel != null) glStatus.GetInstance().ApplicationLabel =
                    MiscUtils.bytes2String(appLabel)

                //TODO: 77
                val resMessage = extractTag(0x77, true)
                if (resMessage == null) {
                    val T77 = convert.strToBcd(
                        "Your_data", IConvert.EPaddingPosition.PADDING_LEFT
                    )
                    Utils.addManualTag(0x77, T77)
                }

                Utils.logsUtils("AID: ${glStatus.GetInstance().AID}\nPINCARD: ${glStatus.GetInstance().PINCARD}\nCardHolderName${glStatus.GetInstance().CardHolderName}\nApplicationLabel${glStatus.GetInstance().ApplicationLabel}")

                bSuccess = true
            } catch (e: Exception) {
                Utils.logsUtils("onlineProcess: Error Obteniendo TAGS EMV" + e.message)
                onDetectError(DetectCardResult.ERetCode.ERR_OTHER)
            }
        }
        if (bSuccess) {
            when (currentTxnType) {
                Utils.TXN_TYPE_PICC -> {
                    Utils.logsUtils("TXN_TYPE_PICC")
                    glStatus.GetInstance().currentReaderType =
                        com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType.PICC.eReaderType.toInt()
                }

                Utils.TXN_TYPE_MAG -> {
                    Utils.logsUtils("TXN_TYPE_MAG")
                    glStatus.GetInstance().currentReaderType =
                        com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType.MAG.eReaderType.toInt()
                }

                Utils.TXN_TYPE_ICC -> {
                    Utils.logsUtils("TXN_TYPE_ICC")
                    glStatus.GetInstance().currentReaderType =
                        com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType.ICC.eReaderType.toInt()
                }
            }
            processReaderEVMCompleted()
        } else {
            onDetectError(DetectCardResult.ERetCode.ERR_OTHER)
        }
    }

    private fun offlineProccess(msg: String) {
        Utils.logsUtils("offlineProccess: $msg")
        onDetectError(DetectCardResult.ERetCode.ERR_OTHER)
    }


    override fun onMagDetectOK(
        pan: String?, expiryDate: String?, Track1: String?, Track2: String?
    ) {
        Utils.logsUtils("onMagDetectOK: ${getString(R.string.prompt_swipe_card)}", 0)

        currentTxnType = Utils.TXN_TYPE_MAG

        //add CVM process, such as enter pin or signature and so on.
        glStatus.GetInstance().PAN = pan
        glStatus.GetInstance().ExpirationDate = expiryDate
        glStatus.GetInstance().Track1 = Track1
        glStatus.GetInstance().Track2 = Track2
        glStatus.GetInstance().currentReaderType = EReaderType.MAG.eReaderType.toInt()

        if (readMSRCardOnly == 0) {
            onlineProcess()
        } else {
            offlineProccess("Mag OK")
        }
    }

    override fun onIccDetectOK() {
        Utils.logsUtils("onIccDetectOK: ${getString(R.string.prompt_dont_remove_card)}", 0)
        currentTxnType = Utils.TXN_TYPE_ICC
        transProcessPresenter?.startEmvTrans()
    }

    override fun onPiccDetectOK() {
        Utils.logsUtils("onPiccDetectOK", 0)
        currentTxnType = Utils.TXN_TYPE_PICC
        transProcessPresenter!!.startOnlinePin()

       /* if (transProcessPresenter != null) {
            if (isSecondTap) { //visa card and other card(not contain master card) 2nd detect card
                isSecondTap = false
                //transProcessPresenter?.completeClssTrans(glStatus.GetInstance().issuerRspData)
//                processCvm()
                transProcessPresenter!!.startOnlinePin()

            } else {
                transProcessPresenter?.startClssTrans() // first time detect card finish
            }
        }*/
    }

    override fun onDetectError(errorCode: DetectCardResult.ERetCode?) {
        runOnUiThread {
            progressbar(false)
            binding.tvLogs.text = errorCode!!.name
            if (errorCode === DetectCardResult.ERetCode.FALLBACK) {
                Utils.logsUtils(getString(R.string.prompt_fallback_insert_card))
            } else Toast.makeText(applicationContext, errorCode.toString(), Toast.LENGTH_SHORT)
                .show()
        }
    }

    //Broadcast
    override fun onResume() {
        super.onResume()
        val mIntentFilter = IntentFilter(Utils.BROADCAST_PROCESS_TRANSACTION_ONLINE_RESULT)
        registerReceiver(mBroadcastReceiverOnlineResult, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mBroadcastReceiverOnlineResult)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDetectCard()
    }

    private val mBroadcastReceiverOnlineResult: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Utils.logsUtils("mBroadcastReceiverOnlineResult", 0)
        }
    }

    /**
     * Utils
     */
    private fun progressbar(visibility: Boolean) {
        binding.progressBarCircular.visibility = if (visibility) View.VISIBLE else View.GONE
        binding.tvPrinter.visibility = if (visibility) View.VISIBLE else View.GONE
    }

    private fun getFirstGACTag() {
        var data = ByteArray()
        var ret: Int = EmvProcess.getInstance().getTlv(0x95, data)
        if (ret == RetCode.EMV_OK) {
            val dataArr = kotlin.ByteArray(data.length)
            System.arraycopy(data.data, 0, dataArr, 0, data.length)
            val firstGacTVR = ConvertHelper.getConvert().bcdToStr(dataArr)
        }
        data = ByteArray()
        ret = EmvProcess.getInstance().getTlv(0x9B, data)
        if (ret == RetCode.EMV_OK) {
            val dataArr = kotlin.ByteArray(data.length)
            System.arraycopy(data.data, 0, dataArr, 0, data.length)
            val firstGacTSI = ConvertHelper.getConvert().bcdToStr(dataArr)
        }
        data = ByteArray()
        ret = EmvProcess.getInstance().getTlv(0x9F27, data)
        if (ret == RetCode.EMV_OK) {
            val dataArr = kotlin.ByteArray(data.length)
            System.arraycopy(data.data, 0, dataArr, 0, data.length)
            val firstGacCID = ConvertHelper.getConvert().bcdToStr(dataArr)
        }
    }

    private fun processCvm() {
        //get TransResult
        glStatus.GetInstance().currentTxnCVMResult = currentTxnCVMResult
        when (currentTxnCVMResult) {
            CvmResultEnum.CVM_NO_CVM -> {
                //1.check trans result
                checkTransResult()
            }

            CvmResultEnum.CVM_SIG -> {
                //1.signature process 2.check trans result
                signatureProcess()
            }

            CvmResultEnum.CVM_ONLINE_PIN -> {
                when (currentTxnType) {
                    Utils.TXN_TYPE_PICC -> {
                        //1.online pin process
                        // 2.check trans result
                        transProcessPresenter!!.startOnlinePin()
                    }

                    Utils.TXN_TYPE_ICC -> {
                        //check result
                        checkTransResult()
                    }
                }
            }

            CvmResultEnum.CVM_ONLINE_PIN_SIG -> {
                //AESC 17-02-2023 PICC no this cvm
                if (currentTxnType == Utils.TXN_TYPE_ICC) {
                    //1.signature process 2.check trans result
                    signatureProcess()
                }
            }

            CvmResultEnum.CVM_OFFLINE_PIN -> {
                //1.check trans result
                checkTransResult()
            }

            CvmResultEnum.CVM_CONSUMER_DEVICE -> {
                //1.restart detect(tap) card and transaction
                startClssTransAgain()
            }

            else -> {
                //Error
                onDetectError(DetectCardResult.ERetCode.ERR_OTHER)
            }
        }
    }

    private fun signatureProcess() {
        //There is a time-consuming wait dialog to simulate the signature process
        //displayProcessDlg(PROCESSING_TYPE_SIGNATURE, "Signature Processing...");
        checkTransResult()
    }

    private fun checkTransResult() {
        Utils.logsUtils("checkTransResult:$currTransResultEnum")
        when (currTransResultEnum) {
            TransResultEnum.RESULT_REQ_ONLINE -> {
                // 1.online process 2.to result page
                onlineProcess()
            }

            TransResultEnum.RESULT_OFFLINE_APPROVED -> {
                //1.to result page
                processFinishTransaction()
            }

            TransResultEnum.RESULT_OFFLINE_DENIED -> {
                // 1.to result page
                processFinishTransaction()
            }

            else -> {
                Utils.logsUtils("unexpected result,$currTransResultEnum", 1)
                processFinishTransaction()
            }
        }
    }

    private fun startClssTransAgain() {
        isSecondTap = false
        startTransaction()
    }

    private fun sortTags(shortedTagsList: IntArray) {
        val aid = EMVUtils.GetCardBrandByAID(glStatus.GetInstance().AID)
        var shorted: String? = null
        when (aid) {
            EMVUtils.CardBrand.UNKNOWN -> {}
            EMVUtils.CardBrand.VISA -> {
                shorted = glStatus.GetInstance().tranEMVTags.GetTLVasHexString(shortedTagsList)
            }

            EMVUtils.CardBrand.MASTERCARD -> {
                shorted = glStatus.GetInstance().tranEMVTags.GetTLVasHexString(shortedTagsList)
            }

            EMVUtils.CardBrand.AMEX -> {}
            EMVUtils.CardBrand.JCB -> {}
            EMVUtils.CardBrand.DISCOVER -> {}
        }

        binding.tvTags.text = shorted
        Utils.logsUtils("TLV: $shorted")
        glStatus.GetInstance().tranEMVTags.Clear()


        //Aqui se tendria que enviar o procesar la lista de tags obteniedos
        val piccResponse = "9F36020001910A664D533DDC35C8223030"

        //Al llamar este metodo, se validan la respuesta de los tags obteniedos del procesamiento.
        responseOnline(piccResponse)
    }

    /**
     *====online process =====
     *1.get TAG value with getTlv API
     *2.pack message, such as ISO8583
     *3.send message to acquirer host
     *4.get response of acquirer host
     *5.set value of acquirer result code and script, such as TAG 71(Issuer Script Data 1),72(Issuer Script Data 2),91(Issuer Authentication Data),8A(Response Code),89(Authorization Code) and so on.
     */
    private fun displayEnterPinDlg(title: String) {
        if (isFinishing) {
            return
        }

        if (mEnterPinPopWindow != null) {
            if (mEnterPinPopWindow!!.isShowing) {
                mEnterPinPopWindow!!.dismiss()
            }
            mEnterPinPopWindow = null
        }

        val popView: View = layoutInflater.inflate(R.layout.dlg_enter_pin, null)
        mEnterPinPopWindow = PopupWindow(
            popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        pinText = popView.findViewById(R.id.tv_pin)
        val titleTxt = popView.findViewById<TextView>(R.id.tv_title)
        titleTxt.text = title
        mEnterPinPopWindow!!.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.white)))
        mEnterPinPopWindow!!.isFocusable = true
        mEnterPinPopWindow!!.isOutsideTouchable = false
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT,
            0f,
            Animation.RELATIVE_TO_PARENT,
            0f,
            Animation.RELATIVE_TO_PARENT,
            1f,
            Animation.RELATIVE_TO_PARENT,
            0f
        )
        animation.interpolator = AccelerateInterpolator()
        animation.duration = 200
        mEnterPinPopWindow!!.setOnDismissListener {
            ScreenUtils.lightOn(this)
            if (currentTxnType == Utils.TXN_TYPE_PICC) {
                if (pinResult != 0) {
                    Utils.logsUtils("getString pinblock err: $pinResult")
                } else {
                    checkTransResult()
                }
            }
        }
        mEnterPinPopWindow!!.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        mEnterPinPopWindow!!.showAtLocation(
            bottomView, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0
        )
        popView.startAnimation(animation)
        ScreenUtils.lightOff(this)
    }


    //Pax Printer
    private fun generateVoucher(): Bitmap {
        val page: IPage = iPaxGLPage.createPage()
        page.typeFace = "/cache/data/public/neptune/Fangsong.ttf"
        val unit = page.createUnit()
        unit.align = IPage.EAlign.CENTER
        unit.text = "GLiPaxGlPage"
        page.addLine().addUnit().addUnit(unit)
            .addUnit(page.createUnit().setText("Test").setAlign(IPage.EAlign.RIGHT))
        page.addLine().addUnit(
            "商户存根", Utils.FONT_NORMAL, IPage.EAlign.RIGHT, IPage.ILine.IUnit.TEXT_STYLE_BOLD
        )
        page.addLine().addUnit(
            "商户存根",
            Utils.FONT_NORMAL,
            IPage.EAlign.RIGHT,
            IPage.ILine.IUnit.TEXT_STYLE_UNDERLINE
        )
        page.addLine().addUnit(
            "商户存根",
            Utils.FONT_NORMAL,
            IPage.EAlign.RIGHT,
            IPage.ILine.IUnit.TEXT_STYLE_BOLD or IPage.ILine.IUnit.TEXT_STYLE_UNDERLINE
        )
        page.addLine().addUnit(
            "商户存根", Utils.FONT_NORMAL, IPage.EAlign.RIGHT, IPage.ILine.IUnit.TEXT_STYLE_NORMAL
        )
        page.addLine().addUnit(
            "商户存根",
            Utils.FONT_NORMAL,
            IPage.EAlign.RIGHT,
            IPage.ILine.IUnit.TEXT_STYLE_BOLD or IPage.ILine.IUnit.TEXT_STYLE_UNDERLINE,
            1f
        )
        page.addLine().addUnit(
            "商户存根",
            Utils.FONT_NORMAL,
            IPage.EAlign.RIGHT,
            IPage.ILine.IUnit.TEXT_STYLE_NORMAL,
            1f
        )
        page.addLine().addUnit("-----------------------------------------", Utils.FONT_NORMAL)
        page.addLine().addUnit("商户名称: " + "百富计算机技术", Utils.FONT_NORMAL)
        page.addLine().addUnit("商户编号: " + "111111111111111", Utils.FONT_NORMAL)

        page.addLine().addUnit("终端编号:", 40).addUnit("操作员号:", 10, IPage.EAlign.RIGHT)
        page.addLine().addUnit("22222222", Utils.FONT_NORMAL)
            .addUnit("01", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)

        page.addLine().addUnit("卡号：", Utils.FONT_NORMAL)
        page.addLine().addUnit("5454545454545454", Utils.FONT_BIG)

        page.addLine().addUnit("交易类型: " + "消费", Utils.FONT_BIG)

        page.addLine().addUnit("流水号:", Utils.FONT_NORMAL)
            .addUnit("批次号:", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
        page.addLine().addUnit("123456", Utils.FONT_NORMAL)
            .addUnit("000001", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)

        page.addLine().addUnit(
            "授权码:", Utils.FONT_NORMAL, IPage.EAlign.LEFT, IPage.ILine.IUnit.TEXT_STYLE_NORMAL, 1f
        ).addUnit(
            "参考号:",
            Utils.FONT_NORMAL,
            IPage.EAlign.RIGHT,
            IPage.ILine.IUnit.TEXT_STYLE_NORMAL,
            1f
        )
        page.addLine().addUnit(
            "987654", Utils.FONT_BIGEST, IPage.EAlign.LEFT, IPage.ILine.IUnit.TEXT_STYLE_NORMAL, 1f
        ).addUnit(
            "012345678912",
            Utils.FONT_NORMAL,
            IPage.EAlign.RIGHT,
            IPage.ILine.IUnit.TEXT_STYLE_NORMAL
        )

        page.addLine().addUnit("日期/时间:" + "2016/06/13 12:12:12", Utils.FONT_NORMAL)
        page.addLine().addUnit("金额:", Utils.FONT_BIG)
        page.addLine().addUnit(
            "RMB 1.00", Utils.FONT_BIG, IPage.EAlign.RIGHT, IPage.ILine.IUnit.TEXT_STYLE_BOLD
        )

        page.addLine().addUnit("备注:", Utils.FONT_NORMAL)
        page.addLine().addUnit("----------------持卡人签名---------------", Utils.FONT_NORMAL)
        //        page.addLine().addUnit(getImageFromAssetsFile("pt.bmp"))
        page.addLine().addUnit("-----------------------------------------", Utils.FONT_NORMAL)
        page.addLine().addUnit(
            "本人确认已上交易, 同意将其计入本卡账户\n\n\n\n\n",
            Utils.FONT_NORMAL,
            IPage.EAlign.CENTER,
            IPage.ILine.IUnit.TEXT_STYLE_UNDERLINE
        )

        page.addLine().addUnit("交易类型", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("笔数", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("金额", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
        page.addLine().addUnit("-----------------------------------------", Utils.FONT_NORMAL)
        page.addLine().addUnit("消费", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("20", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("12345678901234.00", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
        page.addLine().addUnit("退货", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("40", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("123.00", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
        page.addLine().addUnit("激活", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("80", Utils.FONT_NORMAL, IPage.EAlign.LEFT).addUnit()
        page.addLine().addUnit("预授权完成请求撤销", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
            .addUnit("120", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("80.00", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
        page.addLine().addUnit("预授权完成请求", Utils.FONT_NORMAL, IPage.EAlign.CENTER)
            .addUnit("120", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("80.00", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
        page.addLine().addUnit("--------------------------------------\n\n", Utils.FONT_NORMAL)

        page.addLine().addUnit("TEST 1", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("TEST 2", Utils.FONT_NORMAL, IPage.EAlign.CENTER)
            .addUnit("TEST 3", Utils.FONT_NORMAL, IPage.EAlign.CENTER)
            .addUnit("TEST 4", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)

        page.addLine().addUnit("TEST 5", Utils.FONT_NORMAL, IPage.EAlign.LEFT)
            .addUnit("TEST 6", Utils.FONT_NORMAL, IPage.EAlign.CENTER)
            .addUnit("TEST 7", Utils.FONT_NORMAL, IPage.EAlign.CENTER)
            .addUnit("TEST 8", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
            .addUnit("TEST 9", Utils.FONT_NORMAL, IPage.EAlign.RIGHT)
        page.addLine().addUnit("\n\n\n\n", Utils.FONT_NORMAL)
        val width = 384

        return iPaxGLPage.pageToBitmap(page, width)
    }

    /**
     * Visanet Printer Demo
     */
    private fun generateVoucherVisanet(voucher: String): Bitmap {
        val page = iPaxGLPage.createPage()
        val unit = page.createUnit()
        unit.align = IPage.EAlign.CENTER
        unit.text = "GLiPaxGlPage"
        val icon = BitmapFactory.decodeResource(resources, R.drawable.visalogousar)
        val width = icon.width
        val height = icon.height
        val scaleWidth = (380f / width)
        val scaleHeight = (110f / height)
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        val resizedBitmap = Bitmap.createBitmap(icon, 0, 0, width, height, matrix, false)

        page.addLine().addUnit(resizedBitmap, IPage.EAlign.CENTER)

        //Parser
        val vComercio = voucher.split("|").toTypedArray()

        //AESC 2021-11-18 Se analiza la primera posicion del array para detectar que no sea 0 o 1
        for (i in vComercio.indices) {
            val currentLine = vComercio[i]

            if (i == 0 && (currentLine == "1" || currentLine == "0")) {
                //No hacer nada dado que trae un 1 o 0
                println("No hacer nada dado que trae un 1 o 0")
            } else {
                if (currentLine.trim { it <= ' ' } == "") page.addLine().addUnit("\n", 10)
                else page.addLine()
                    .addUnit(currentLine.trim { it <= ' ' }, Utils.FONT_NORMAL, IPage.EAlign.CENTER)
            }
        }
        page.addLine().addUnit("\n\n", Utils.FONT_NORMAL)
        val widthFinal = 384
        return iPaxGLPage.pageToBitmap(page, widthFinal)
    }

    override fun onClick(v: View?) {
        val id = v?.id
        binding.tvTags.text = ""
        binding.tvLogs.text = ""
        when (id) {
            R.id.btnTestPaxPrinter -> {
                progressbar(true)
                GlobalScope.launch {
                    try {
                        val bitmap = generateVoucher()
                        val status = sdkTTPax.printer(bitmap)
                        Utils.logsUtils("Printer Status: $status")
                        withContext(Dispatchers.Main) {
                            binding.tvLogs.text = getString(R.string.msg_printer_status_ok)
                            progressbar(false)
                        }
                    } catch (e: PrinterException) {
                        val code = e.errCode
                        val msg = e.errMsg
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                            binding.tvLogs.text = "Code: $code, Msg: $msg"
                            progressbar(false)
                        }
                    }
                }
            }

            R.id.btnTestCustomPrinter -> {
                progressbar(true)
                GlobalScope.launch {
                    try {
                        val voucher = getString(R.string.voucher_visanet)
                        val bitmap = generateVoucherVisanet(voucher)
                        val status = sdkTTPax.printer(bitmap)
                        Utils.logsUtils("Printer Status: $status")
                        withContext(Dispatchers.Main) {
                            binding.tvLogs.text = getString(R.string.msg_printer_status_ok)
                            progressbar(false)
                        }
                    } catch (e: PrinterException) {
                        val code = e.errCode
                        val msg = e.errMsg
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                            binding.tvLogs.text = "Code: $code, Msg: $msg"
                            progressbar(false)
                        }
                    }
                }
            }

            R.id.btnTestEmv -> {
                binding.tvPrinter.text = getString(R.string.insert_tap_swipe_card)
                progressbar(true)
                val amount = 5000L
                initEmvTransaction(amount)
            }
        }
    }
}