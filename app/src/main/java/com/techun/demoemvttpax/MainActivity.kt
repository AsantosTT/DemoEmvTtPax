package com.techun.demoemvttpax

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.WallpaperManager
import android.bluetooth.BluetoothAdapter
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pax.dal.IFingerprintReader
import com.pax.dal.entity.ETermInfoKey
import com.pax.dal.entity.FingerprintResult
import com.pax.dal.entity.PosMenu
import com.pax.dal.exceptions.FingerprintDevException
import com.pax.gl.page.IPage
import com.pax.gl.page.PaxGLPage
import com.pax.neptunelite.api.NeptuneLiteUser
import com.techun.demoemvttpax.databinding.ActivityMainBinding
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.base.BaseActivity
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.TagsTable
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.EOnlineResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.enums.TransResultEnum
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.AppDataUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.ConvertHelper
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DetectCardResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.Device
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EMVUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EReaderType
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.MiscUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.TrackUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.IConvert
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.printer.exception.PrinterException
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.EnumMap
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.math.round


private const val FEATURE_ANSI_INCITS_378_2004 = 1
private const val FEATURE_ISO_IEC_19794_2_2005 = 2
private const val FEATURE_ARATEK_BIONE = 3
private const val FEATURE_RESERVED_1 = 4
private const val FEATURE_RESERVED_2 = 5

private const val IMAGE_TYPE_RAW = 1
private const val IMAGE_TYPE_BMP = 2
private const val IMAGE_TYPEWSQ = 3
private const val IMAGE_ANSI_INCITS_381_2004 = 4
private const val IMAGE_ISO_IEC_19794_4_2005 = 5

private const val PERMISSION_REQUEST_CODE = 100

@AndroidEntryPoint
class MainActivity : BaseActivity(), View.OnClickListener {

    private val permissions = arrayOf(
        Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_SETTINGS,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.SET_WALLPAPER
    )

    //Pages
    lateinit var iPaxGLPage: PaxGLPage

    //SDK
    lateinit var sdkTTPax: TTPaxApi

    private lateinit var binding: ActivityMainBinding

    private lateinit var reader: IFingerprintReader
    private var test_time = 0
    private var feature_t1: ByteArray? = null
    private var feature_t2: ByteArray? = null
    private var time = 0
    private var time_ok = false


    lateinit var locationManager: LocationManager
    lateinit var gpsLocationListener: LocationListener
    lateinit var networkLocationListener: LocationListener

    //Variable to store brightness value
    private var brightness = 0

    //Content resolver used as a handle to the system's settings
    private var cResolver: ContentResolver? = null

    //Window object, that will store a reference to the current window
    private var window: Window? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
    }

    @SuppressLint("SetTextI18n")
    private fun initComponents() {
        checkAndRequestPermissions()

        sdkTTPax = TTPaxApi(this)

        //Init PaxGLPage Libary
        iPaxGLPage = PaxGLPage.getInstance(this)

        val ped = Sdk.isPaxDevice
        logs("Is PAX Device: $ped")
        logs("Is PAX Device: ${Build.DISPLAY}")
        logs("Is PAX Device: ${Build.DEVICE}")
        logs("Is PAX Device: ${Build.MODEL}")
        logs("Is PAX Device: ${Build.PRODUCT}")
        logs("Is PAX Device: ${Build.MANUFACTURER}")

        //Init SDK
        sdkTTPax.initPaxSdk({
            //Exitoso
            Utils.logsUtils("PAX is working correctly")
            try {
                reader = NeptuneLiteUser.getInstance().getDal(this).fingerprintReader
                Utils.logsUtils("reader is working correctly")

                Timer("time").scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            if (time_ok) {
                                if (time < 6) {
                                    time++
                                    logs(time.toString() + "S")
                                    if (time == 6) {
                                        logs("timeout")
                                    }
                                }
                            }
                        }
                    }
                }, 0, 1000L)
            } catch (e: java.lang.Exception) {
                Utils.logsUtils(e.printStackTrace().toString())
            }
        }, {
            println("Error: $it")
        })

        binding.layoutUi.btnTestPaxPrinter.setOnClickListener(this)
        binding.layoutUi.btnTestCustomPrinter.setOnClickListener(this)
        binding.layoutUi.btnTestEmv.setOnClickListener(this)

        binding.layoutUi.btnFingerPrintOpen.setOnClickListener(this)
        binding.layoutUi.btnFingerPrintExtracImage.setOnClickListener(this)
        binding.layoutUi.btnFingerPrintExtracFeature.setOnClickListener(this)
        binding.layoutUi.btnFingerPrintCompareFeature.setOnClickListener(this)
        binding.layoutUi.btnFingerPrintClose.setOnClickListener(this)
        binding.layoutUi.btnFingerPrintStop.setOnClickListener(this)
        binding.layoutUi.btnSysInfo.setOnClickListener(this)
        binding.layoutUi.btnSetWallpaper.setOnClickListener(this)
        binding.layoutUi.btnReboot.setOnClickListener(this)
        binding.layoutUi.btnShutdown.setOnClickListener(this)

        //Brightness
        cResolver = contentResolver
        window = getWindow()
        binding.layoutUi.sbBrightness.max = 255
        binding.layoutUi.sbBrightness.keyProgressIncrement = 1
        try {
            brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            logs("Error brightness - Cannot access system brightness")
        }

        binding.layoutUi.sbBrightness.progress = brightness
        val perc: Float = brightness / 255f * 100
        binding.layoutUi.tvBrightnessPorcent.text = perc.toInt().toString() + " %"

        binding.layoutUi.sbBrightness.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setScreenBrightness((brightness / 255f * 100).toInt())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //Nothing handled here

            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                //Set the minimal brightness level
                brightness = progress
                //Calculate the brightness percentage
                val perc: Float = brightness / 255f * 100
                //Set the brightness percentage
                binding.layoutUi.tvBrightnessPorcent.text = perc.toInt().toString() + " %"
            }
        })

        //Congis

        //Init Sw
        // Declaring Bluetooth adapter
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        binding.layoutUi.swBt.isChecked = mBluetoothAdapter.isEnabled

        //Declaring Wifi
        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        binding.layoutUi.swWifi.isChecked = wifi.isWifiEnabled


        binding.layoutUi.swEnableStatusBar.setOnCheckedChangeListener { _, isChecked ->
            val status = !isChecked
            val systemConfig = sdkTTPax.getDal(applicationContext)!!.sys
            systemConfig.enableStatusBar(status) // Con esto bloquemos la statusbar
        }

        binding.layoutUi.swSettingsNeedPassword.setOnCheckedChangeListener { _, isChecked ->
            val status = !isChecked
            val systemConfig = sdkTTPax.getDal(applicationContext)!!.sys
            systemConfig.setSettingsNeedPassword(!status) // Con esto bloquemos la statusbar
        }

        binding.layoutUi.swBt.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                mBluetoothAdapter.enable()
            } else {
                mBluetoothAdapter.disable()
            }

            Toast.makeText(applicationContext, "$isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.layoutUi.swWifi.setOnCheckedChangeListener { _, isChecked ->
            /*updateSettings(PosMenu.WIFI, isChecked)
            updateSettings(PosMenu.QS_WIFI, isChecked)*/
            wifi.isWifiEnabled = isChecked
            Toast.makeText(applicationContext, "$isChecked", Toast.LENGTH_SHORT).show()
        }

        val langage = Locale.getDefault().language.toString()
    }

    private fun updateSettings(posMenu: PosMenu, status: Boolean) {
        val systemConfig = sdkTTPax.getDal(applicationContext)!!.sys
        val configuraciones: MutableMap<PosMenu, Boolean> =
            EnumMap(PosMenu::class.java)
        configuraciones[posMenu] = status
        systemConfig.disablePosMenu(configuraciones)
    }

    fun configuracionesPOS() {
        try {
            val configuraciones: MutableMap<PosMenu, Boolean> =
                EnumMap(PosMenu::class.java)
            configuraciones[PosMenu.BT] = true
            configuraciones[PosMenu.QS_BT] = true
            configuraciones[PosMenu.WIFI] = true
            configuraciones[PosMenu.QS_WIFI] = true
            configuraciones[PosMenu.GL_AIRPLANE] = true
            configuraciones[PosMenu.QS_AIRPLANE] = true
            val systemConfig = sdkTTPax.getDal(applicationContext)!!.sys
            systemConfig.disablePosMenu(configuraciones) //Cargamos las configuraciones que queremos que sean visibles


            systemConfig.enableStatusBar(true) // Con esto bloquemos la statusbar
            systemConfig.setSettingsNeedPassword(false) // Pedir contraseña de configuraciones


            logs("Settings Applied!")
        } catch (ex: java.lang.Exception) {
            logs("Error ${ex.message}")
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // El permiso no fue concedido, puedes manejar esta situación según tus necesidades
                    Toast.makeText(this, "Access denied for ${grantResults[i]}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onDetectError(errorCode: DetectCardResult.ERetCode?) {
        runOnUiThread {
            progressbar(false)
            logs(errorCode!!.name)
            if (errorCode === DetectCardResult.ERetCode.FALLBACK) {
                Utils.logsUtils(getString(R.string.prompt_fallback_insert_card))
            } else Toast.makeText(applicationContext, errorCode.toString(), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onReadCardOK() {
        //Realizar accion al momentor de detectar una tarjeta
        Utils.logsUtils("onReadCardOk")
    }

    override fun onRemoveCard() {
        binding.layoutUi.tvPrinter.text = getString(R.string.prompt_remove_card)
    }

    /**
     *====online process =====
     *1.get TAG value with getTlv API
     *2.pack message, such as ISO8583
     *3.send message to acquirer host
     *4.get response of acquirer host
     *5.set value of acquirer result code and script, such as TAG:
     * - 71(Issuer Script Data 1)
     * - 72(Issuer Script Data 2)
     * - 91(Issuer Authentication Data)
     * - 8A(Response Code)
     * - 89(Authorization Code) and so on.
     */
    override fun onlineProcess() {
        Utils.logsUtils(" >>>  onlineProcess <<<< ")
        Utils.logsUtils(" >>>  PROCESSING_TYPE_ONLINE <<<< ")
        var bSuccess = false
        if (glStatus.GetInstance().currentReaderType == EReaderType.MAG.eReaderType.toInt()) {
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
                        glStatus.GetInstance().PANSeqNo = panseqno.get(0)
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
                    glStatus.GetInstance().currentReaderType = EReaderType.PICC.eReaderType.toInt()
                }

                Utils.TXN_TYPE_MAG -> {
                    Utils.logsUtils("TXN_TYPE_MAG")
                    glStatus.GetInstance().currentReaderType = EReaderType.MAG.eReaderType.toInt()
                }

                Utils.TXN_TYPE_ICC -> {
                    Utils.logsUtils("TXN_TYPE_ICC")
                    glStatus.GetInstance().currentReaderType = EReaderType.ICC.eReaderType.toInt()
                }
            }
            processReaderEVMCompleted()
        } else {
            onDetectError(DetectCardResult.ERetCode.ERR_OTHER)
        }
    }

    override fun proccessEmvComplete() {
        //Si la transaccion tanto ICC o PICC se realiza con exito, a este metodo se estara accediendo,
        //si hay un error se notificara al metodo onDetectError
        runOnUiThread {
            logs("TRANSACCION FINALIZADA")
            Utils.logsUtils("TRANSACCION FINALIZADA")
            Toast.makeText(this, "TRANSACCION FINALIZADA", Toast.LENGTH_SHORT).show()
        }
    }

    override fun processReaderEVMCompleted() {
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

        binding.layoutUi.tvTags.text = shorted
        glStatus.GetInstance().tranEMVTags.Clear()


        //Aqui se tendria que enviar o procesar la lista de tags obteniedos
        val piccResponse = "9F36020001910A664D533DDC35C8223030"

        //Al llamar este metodo, se validan la respuesta de los tags obteniedos del procesamiento.
        responseOnline(piccResponse)
    }

    override fun responseOnline(icc: String) {
        if (icc.isNotEmpty()) {
            val emvTagsResp: String = icc
            val result = glStatus.GetInstance().tranEMVResponseTags.ParseTLV(false, emvTagsResp)
            Utils.logsUtils("emvTagsResp: $result")
        }

        val codAuth = "072745"
        glStatus.GetInstance().AuthCode = codAuth

        if (glStatus.GetInstance().tranEMVResponseTags.ContainsTag(0x91)) {
            val authData = glStatus.GetInstance().tranEMVResponseTags.GetTag(0x91).Value()
            glStatus.GetInstance().issuerRspData.authData = authData //TAG:91
        }

        if (codAuth.isNotEmpty()) {
            val authCode: ByteArray = ConvertHelper.getConvert().strToByteArray(codAuth)
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

        when (EReaderType.fromInt(glStatus.GetInstance().currentReaderType)) {
            EReaderType.PICC -> {
                glStatus.GetInstance().TransactionResult = TransResultEnum.RESULT_ONLINE_APPROVED
                //Si se quiere procesar el segundo criptograma en contactless 2ndGAC PICC poner en true;
                glStatus.GetInstance().Need2ndGAC = false
            }

            EReaderType.ICC -> glStatus.GetInstance().Need2ndGAC = true
            EReaderType.MAG -> {
            }

            else -> throw IllegalStateException("Unexpected value: " + glStatus.GetInstance().currentReaderType)
        }

        proccessOnlineResponse()
    }

    private fun logs(msg: String?) {
        Log.d("logs-demo-app", "$msg")
        binding.layoutUi.tvLogs.append("$msg \n")
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onClick(v: View?) {
        val id = v?.id
        binding.layoutUi.tvTags.text = ""
        when (id) {
            //Printer
            R.id.btnTestPaxPrinter -> {
                progressbar(true, getString(R.string.printing_voucher_please_wait))
                GlobalScope.launch {
                    try {
                        val bitmap = generateVoucher()
                        val status = sdkTTPax.printer(bitmap)
                        Utils.logsUtils("Printer Status: $status")
                        withContext(Dispatchers.Main) {
                            logs(getString(R.string.msg_printer_status_ok))
                            progressbar(false)
                        }
                    } catch (e: PrinterException) {
                        val code = e.errCode
                        val msg = e.errMsg
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                            logs("Code: $code, Msg: $msg")
                            progressbar(false)
                        }
                    }
                }
            }

            R.id.btnTestCustomPrinter -> {
                progressbar(true, getString(R.string.printing_voucher_please_wait))
                GlobalScope.launch {
                    try {
                        val voucher = getString(R.string.voucher_bi)
                        val bitmap = generateVoucherVisanet(voucher)
                        val status = sdkTTPax.printer(bitmap)
                        Utils.logsUtils("Printer Status: $status")
                        withContext(Dispatchers.Main) {
                            logs(getString(R.string.msg_printer_status_ok))
                            progressbar(false)
                        }
                    } catch (e: PrinterException) {
                        val code = e.errCode
                        val msg = e.errMsg
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                            logs("Code: $code, Msg: $msg")
                            progressbar(false)
                        }
                    }
                }
            }

            //EMV
            R.id.btnTestEmv -> {
                binding.layoutUi.tvPrinter.text = getString(R.string.insert_tap_swipe_card)
                progressbar(true, getString(R.string.emv))
                val amount = 5000L
                initEmvTransaction(amount)
            }

            //fingerprint
            // 1. call the fingerOpen function to open the fingerprint function;
            // 2. call the setTimeOut function to set the timeout according to the test data;.
            // 3. Put the fingerprint in the induction area, call fingerStart function to get the fingerprint information;
            // 4. call fingerStop function to stop acquiring fingerprint information and move the fingerprint away from the sensing area;
            // 5. call fingerStop function to stop acquiring fingerprint information and move the fingerprint away from the sensing area
            // 6. call fingerClose function to close the fingerprint module.
            // The fingerprint leaves the sensing area, repeat steps 1-5 cnt times
            R.id.btnFingerPrintOpen -> {
                try {
                    reader.open()
                } catch (e: FingerprintDevException) {
                    logs("App" + e.message)
                    e.printStackTrace()
                }
            }

            R.id.btnFingerPrintExtracImage -> {
                logs("Extract Image")
                time = 0
                time_ok = true
                progressbar(true, getString(R.string.press_fingerprint))


                try {
                    reader.extractImage(IMAGE_TYPE_RAW, 10, object :
                        IFingerprintReader.FingerprintListener {
                        override fun onError(i: Int) {
                            logs("Error: $i")
                        }

                        override fun onSuccess(fingerprintResult: FingerprintResult) {
                            val img = fingerprintResult.captureImage
                            GlobalScope.launch {
                                withContext(Dispatchers.Main) {
                                    if (img != null) {
                                        val code = toHexString(img)
                                        logs("img size: ${img.size}")
                                        logs(code)
                                        progressbar(false)
                                    } else {
                                        logs("img is null")
                                        progressbar(false)
                                    }
                                }
                            }
                        }
                    })
                } catch (e: java.lang.Exception) {
                    logs("App" + e.message)
                }
            }

            R.id.btnFingerPrintExtracFeature -> {
                try {
                    reader.extractFeature(
                        FEATURE_ANSI_INCITS_378_2004,
                        object : IFingerprintReader.FingerprintListener {
                            override fun onError(i: Int) {}
                            override fun onSuccess(fingerprintResult: FingerprintResult) {
                                val feature = fingerprintResult.featureCode
                                runOnUiThread { logs(toHexString(feature)) }
                            }
                        })
                } catch (e: FingerprintDevException) {
                    e.printStackTrace()
                }
            }

            R.id.btnFingerPrintCompareFeature -> {
                if (test_time == 0) {
                    logs("Press fingerprints")
                }

                try {
                    reader.extractFeature(2, listener)
                } catch (e: FingerprintDevException) {
                    logs(e.printStackTrace().toString())
                    e.printStackTrace()
                }
            }

            R.id.btnFingerPrintClose -> {
                try {
                    reader.close()
                } catch (e: java.lang.Exception) {
                    Log.d("PaxFingerService", "App" + e.message)
                    logs("App" + e.message)
                }
            }

            R.id.btnFingerPrintStop -> {
                try {
                    reader.stop()
                } catch (e: FingerprintDevException) {
                    e.printStackTrace()
                }
            }

            //Sys
            R.id.btnSysInfo -> {
                val param: Map<ETermInfoKey, String> = sdkTTPax.getDal(this)!!.sys.termInfo
                param.forEach { entry ->
                    if (entry.key.toString() == "SN" || entry.key.toString() == "MODEL" || entry.key.toString() == "AP_VER") {
                        print("${entry.key} : ${entry.value}")
                        logs(("${entry.key} : ${entry.value}"))
                    }
                }

                //Android and API version
                val versionAPI = Build.VERSION.SDK_INT
                val versionRelease = Build.VERSION.RELEASE
                logs("API Version : $versionAPI")
                logs("Android Version : Android $versionRelease")

                // Screen Resolutions
                val display = windowManager.defaultDisplay
                val size = Point()
                display.getRealSize(size)
                val width = size.x
                val height = size.y
                logs("Screen Resolution : $width x $height")

                //IMEI
                val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                val imeiNo = tm.deviceId
                logs("IMEI : $imeiNo")

                logs("PN : ${sdkTTPax.getDal(this)!!.sys.pn}")
                logs("Sys Language : ${sdkTTPax.getDal(this)!!.sys.systemLanguage}")
                logs("Date : ${sdkTTPax.getDal(this)!!.sys.date}")

                //RAM
                logs("RAM(%) : ${obtenerPorcentajeRAMUtilizada(this)}")

                //CPU
//                logs("CPU(%) : ${obtenerPorcentajeCPU()}")
                logs("PAX CPU(%) : ${getCpuInfo()[0]}")

                //Battery
                val bm = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
                val batLevel: Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                logs("Batter(%) : $batLevel%")

                //IP
                logs("IP : ${getIpAddress()}")

                //MAC
                logs("MAC : ${getMACAddress()}")

                //Password
                sdkTTPax.getDal(this)!!.sys.setSettingsNeedPassword(false)

                logs("\n\nModule Supported :\n")
                val moduleSupported = sdkTTPax.getDal(this)!!.deviceInfo.moduleSupported
                moduleSupported.forEach { entry ->
                    logs("${entry.key} : ${entry.value}")
                }

                //Installed Apps
                logs("\n\nInstalled Apps :\n")

                val packageManager: PackageManager = packageManager
                val installedApps: List<ApplicationInfo> =
                    packageManager.getInstalledApplications(0)

                for (appInfo in installedApps) {
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                        val appName = appInfo.loadLabel(packageManager).toString()
                        val packageName = appInfo.packageName
                        val icon = appInfo.loadIcon(packageManager)
                        logs(
                            "$appName : \n$packageName\n$icon\n${
                                getInstallationDate(
                                    this, packageName
                                )
                            }\n\n"
                        )
                    }
                }

                //Location
                GlobalScope.launch {
                    logs("\n\nLocation :\n")

                    var locationByNetwork: Location
                    var locationByGps: Location
                    locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                    val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val hasNetwork =
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//
                    /*logs("locationManager hasGps: $hasGps\n")
                    logs("locationManager hasNetwork: $hasNetwork\n")*/

                    gpsLocationListener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            locationByGps = location
                            val date: Date = Calendar.getInstance().time
                            val sdf = SimpleDateFormat("hh:mm:ss a")
                            Log.e("SPLASH", "Updated at : " + sdf.format(date))
//                            logs("gpsLocationListener onLocationChanged Updated at : " + sdf.format(date) + "\n")
                            val latitude = locationByGps.latitude.toString()
                            val longitude = locationByGps.longitude.toString()
                            Log.e("GPSL", "onLocationChanged: latitude : $latitude ")
//                            logs("gpsLocationListener onLocationChanged latitude : $latitude\n")
//                            logs("gpsLocationListener onLocationChanged longitude : $longitude\n")
                            // You can now create a LatLng Object for use with maps
                            locationManager.removeUpdates(gpsLocationListener)
                        }


                        override fun onStatusChanged(
                            provider: String, status: Int, extras: Bundle
                        ) {
                        }

                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {
                            Log.e("onProviderDisabled", "onProviderDisabled: $provider")
                        }
                    }

                    networkLocationListener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            locationByNetwork = location
                            val date: Date = Calendar.getInstance().time
                            val sdf = SimpleDateFormat("hh:mm:ss a")
                            Log.e("SPLASH", "Updated at : " + sdf.format(date))
//                            logs(
//                                "networkLocationListener onLocationChanged Updated at : " + sdf.format(
//                                    date
//                                ) + "\n"
//                            )
                            val latitude = locationByNetwork.latitude.toString()
                            val longitude = locationByNetwork.longitude.toString()
                            Log.e("GPSL", "onLocationChanged: latitude : $latitude ")
//                            logs("networkLocationListener onLocationChanged latitude : $latitude\n")
//                            logs("networkLocationListener onLocationChanged longitude : $longitude\n")
                            // You can now create a LatLng Object for use with maps
                            locationManager.removeUpdates(networkLocationListener)
                        }


                        override fun onStatusChanged(
                            provider: String, status: Int, extras: Bundle
                        ) {
                        }

                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }

                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.

                    } else {
                        withContext(Dispatchers.Main) {
                            logs("locationManager hasNetwork: $hasNetwork\n")

                            if (hasGps) {
                                locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER, 1000, 0F, gpsLocationListener
                                )
                            }


                            if (hasNetwork) {
                                locationManager.requestLocationUpdates(
                                    LocationManager.NETWORK_PROVIDER,
                                    1000,
                                    0F,
                                    networkLocationListener
                                )
                            }
                        }
                    }
                }
            }

            //Set Wallpaper
            R.id.btnSetWallpaper -> {
                // creating the instance of the WallpaperManager
                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                try {
                    // set the wallpaper by calling the setResource function and
                    // passing the drawable file
                    wallpaperManager.setResource(R.drawable.wallpaper)
                    logs("Settings Applied!")
                } catch (e: IOException) {
                    // here the errors can be logged instead of printStackTrace
                    logs("Error set wallpapaer - ${e.printStackTrace()}")
                }
            }

            R.id.btnReboot -> {
                rebootTerminal()
            }

            R.id.btnShutdown -> {
                shutdownTerminal()
            }
        }
    }

    private fun rebootTerminal() {
        val systemConfig = sdkTTPax.getDal(applicationContext)!!.sys
        systemConfig.reboot()
    }

    private fun shutdownTerminal() {
        val systemConfig = sdkTTPax.getDal(applicationContext)!!.sys
        systemConfig.shutdown()
    }

    private fun setScreenBrightness(level: Int) {
        val systemConfig = sdkTTPax.getDal(applicationContext)!!.sys
        systemConfig.setScreenBrightness(level)
    }


    private fun getInstallationDate(context: Context, packageName: String): Date? {
        val packageManager: PackageManager = context.packageManager
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val installTime = packageInfo.firstInstallTime
            return Date(installTime)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getCpuInfo(): Array<String> {
        val str1 = "/proc/cpuinfo"
        var str2 = ""
        val cpuInfo = arrayOf("", "")
        var arrayOfString: Array<String>
        try {
            val fr = FileReader(str1)
            val localBufferedReader = BufferedReader(fr, 8192)
            str2 = localBufferedReader.readLine()
            arrayOfString =
                str2.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in 2 until arrayOfString.size) {
                cpuInfo[0] = cpuInfo[0] + arrayOfString[i] + " "
            }
            str2 = localBufferedReader.readLine()
            arrayOfString =
                str2.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            cpuInfo[1] += arrayOfString[2]
            localBufferedReader.close()
        } catch (e: IOException) {
            onDetectError(DetectCardResult.ERetCode.ERR_OTHER)
        }
        return cpuInfo
    }

    private fun obtenerPorcentajeRAMUtilizada(context: Context): String {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val memoriaUtilizada = memoryInfo.totalMem - memoryInfo.availMem
        val porcentajeRAM = (memoriaUtilizada.toDouble() / memoryInfo.totalMem.toDouble()) * 100.0
        val porcentajeRedondeado = round(porcentajeRAM).toInt()

        return "$porcentajeRedondeado%"
    }

    private fun getIpAddress(): String {
        try {
            val infos: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (info in infos) {
                if (info.name != "wlan0") {
                    continue
                }
                var ipAddr = "0.0.0.0"
                val addrs = info.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr.toString().length <= 16) {
                        ipAddr = addr.toString().subSequence(1, addr.toString().length).toString()
                    }
                }

                return ipAddr
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return "0.0.0.0"
    }

    private fun getMACAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                    val macBytes = networkInterface.hardwareAddress
                    if (macBytes != null) {
                        val stringBuilder = StringBuilder()
                        for (b in macBytes) {
                            stringBuilder.append(String.format("%02X:", b))
                        }
                        if (stringBuilder.isNotEmpty()) {
                            stringBuilder.deleteCharAt(stringBuilder.length - 1)
                        }
                        return stringBuilder.toString()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    private var listener: IFingerprintReader.FingerprintListener = object :
        IFingerprintReader.FingerprintListener {
        override fun onError(i: Int) {
            runOnUiThread { logs("Err$i") }
        }

        override fun onSuccess(fingerprintResult: FingerprintResult) {
            if (test_time == 0) {
                feature_t1 = fingerprintResult.featureCode
                test_time = 1
                runOnUiThread { logs("Press the fingerprint again") }
                SystemClock.sleep(1500)
                try {
                    reader.extractFeature(2, this)
                } catch (e: FingerprintDevException) {
                    e.printStackTrace()
                }
            } else if (test_time == 1) {
                feature_t2 = fingerprintResult.featureCode
                test_time = 0
                runOnUiThread { logs("Compare...") }
                try {
                    val key = reader.compareFeature(4, feature_t1, feature_t2)
                    runOnUiThread { logs("Compare result :$key") }
                } catch (e: FingerprintDevException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun progressbar(visibility: Boolean, msg: String? = "") {
        binding.layoutUi.clDialog.visibility = if (visibility) View.VISIBLE else View.GONE
        binding.layoutUi.tvPrinter.text = msg
    }

    /**
     * Pax Printer Demo
     */
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
        val icon = BitmapFactory.decodeResource(resources, R.drawable.bilogo)
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
}