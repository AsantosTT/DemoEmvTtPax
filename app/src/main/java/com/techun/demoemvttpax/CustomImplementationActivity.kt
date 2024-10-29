package com.techun.demoemvttpax

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
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
import com.pax.dal.entity.ECheckMode
import com.pax.dal.entity.EReaderType
import com.pax.gl.page.IPage
import com.pax.gl.page.PaxGLPage
import com.pax.jemv.clcommon.ByteArray
import com.pax.jemv.clcommon.RetCode
import com.techun.demoemvttpax.databinding.ActivityCustomImplementationBinding
import com.techun.demoemvttpax.utils.emvreader.TransProcessContractView
import com.techun.demoemvttpax.utils.emvreader.TransProcessPresenter
import com.techun.demoemvttpax.utils.pinutils.DUKPTResult
import com.techun.demoemvttpax.utils.writeKeys
import com.tecnologiatransaccional.ttpaxsdk.App
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
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
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.EnterPinResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.MiscUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.NeptunePollingPresenter
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.PedApiUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.ScreenUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.TimeRecordUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.TrackUtils
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.DetectCardContract
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.interfaces.IConvert
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayPassAid
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayWaveAid
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayWaveInterFloorLimit
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayWaveParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayWaveProgramId
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Capk
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkRevoke
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Config
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.contact.EmvAid
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.printer.exception.PrinterException
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CustomImplementationActivity"

class CustomImplementationActivity : AppCompatActivity(), DetectCardContract.View,
    TransProcessContractView, View.OnClickListener {

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

        configSdk()

        binding.btnTestPaxPrinter.setOnClickListener(this)
        binding.btnTestCustomPrinter.setOnClickListener(this)
        binding.btnTestEmv.setOnClickListener(this)
    }

    private fun configSdk() {
        val convert = ConvertHelper.getConvert()

        //CAPK
        val capkList: ArrayList<Capk> = arrayListOf(
            Capk(
                rid = convert.strToBcd("A000000003", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("92", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "176".toInt(),
                module = convert.strToBcd(
                    "996AF56F569187D09293C14810450ED8EE3357397B18A2458EFAA92DA3B6DF6514EC060195318FD43BE9B8F0CC669E3F844057CBDDF8BDA191BB64473BC8DC9A730DB8F6B4EDE3924186FFD9B8C7735789C23A36BA0B8AF65372EB57EA5D89E7D14E9C7B6B557460F10885DA16AC923F15AF3758F0F03EBD3C5C2C949CBA306DB44E6A2C076C5F67E281D7EF56785DC4D75945E491F01918800A9E2DC66F60080566CE0DAF8D17EAD46AD8E30A247C9F",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "429C954A3859CEF91295F663C963E582ED6EB253",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000003", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("94", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "248".toInt(),
                module = convert.strToBcd(
                    "ACD2B12302EE644F3F835ABD1FC7A6F62CCE48FFEC622AA8EF062BEF6FB8BA8BC68BBF6AB5870EED579BC3973E121303D34841A796D6DCBC41DBF9E52C4609795C0CCF7EE86FA1D5CB041071ED2C51D2202F63F1156C58A92D38BC60BDF424E1776E2BC9648078A03B36FB554375FC53D57C73F5160EA59F3AFC5398EC7B67758D65C9BFF7828B6B82D4BE124A416AB7301914311EA462C19F771F31B3B57336000DFF732D3B83DE07052D730354D297BEC72871DCCF0E193F171ABA27EE464C6A97690943D59BDABB2A27EB71CEEBDAFA1176046478FD62FEC452D5CA393296530AA3F41927ADFE434A2DF2AE3054F8840657A26E0FC617",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "C4A3C43CCF87327D136B804160E47D43B60E6E0F",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000003", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("95", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "144".toInt(),
                module = convert.strToBcd(
                    "BE9E1FA5E9A803852999C4AB432DB28600DCD9DAB76DFAAA47355A0FE37B1508AC6BF38860D3C6C2E5B12A3CAAF2A7005A7241EBAA7771112C74CF9A0634652FBCA0E5980C54A64761EA101A114E0F0B5572ADD57D010B7C9C887E104CA4EE1272DA66D997B9A90B5A6D624AB6C57E73C8F919000EB5F684898EF8C3DBEFB330C62660BED88EA78E909AFF05F6DA627B",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "EE1511CEC71020A9B90443B37B1D5F6E703030F6",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000003", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("99", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "128".toInt(),
                module = convert.strToBcd(
                    "AB79FCC9520896967E776E64444E5DCDD6E13611874F3985722520425295EEA4BD0C2781DE7F31CD3D041F565F747306EED62954B17EDABA3A6C5B85A1DE1BEB9A34141AF38FCF8279C9DEA0D5A6710D08DB4124F041945587E20359BAB47B7575AD94262D4B25F264AF33DEDCF28E09615E937DE32EDC03C54445FE7E382777",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "4ABFFD6B1C51212D05552E431C5B17007D2F5E6D",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "128".toInt(),
                module = convert.strToBcd(
                    "C2490747FE17EB0584C88D47B1602704150ADC88C5B998BD59CE043EDEBF0FFEE3093AC7956AD3B6AD4554C6DE19A178D6DA295BE15D5220645E3C8131666FA4BE5B84FE131EA44B039307638B9E74A8C42564F892A64DF1CB15712B736E3374F1BBB6819371602D8970E97B900793C7C2A89A4A1649A59BE680574DD0B60145",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("091231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "5ADDF21D09278661141179CBEFF272EA384B13BB",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("04", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "144".toInt(),
                module = convert.strToBcd(
                    "A6DA428387A502D7DDFB7A74D3F412BE762627197B25435B7A81716A700157DDD06F7CC99D6CA28C2470527E2C03616B9C59217357C2674F583B3BA5C7DCF2838692D023E3562420B4615C439CA97C44DC9A249CFCE7B3BFB22F68228C3AF13329AA4A613CF8DD853502373D62E49AB256D2BC17120E54AEDCED6D96A4287ACC5C04677D4A5A320DB8BEE2F775E5FEC5",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("121231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "381A035DA58B482EE2AF75F4C3F2CA469BA4AA6C",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("05", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "176".toInt(),
                module = convert.strToBcd(
                    "B8048ABC30C90D976336543E3FD7091C8FE4800DF820ED55E7E94813ED00555B573FECA3D84AF6131A651D66CFF4284FB13B635EDD0EE40176D8BF04B7FD1C7BACF9AC7327DFAA8AA72D10DB3B8E70B2DDD811CB4196525EA386ACC33C0D9D4575916469C4E4F53E8E1C912CC618CB22DDE7C3568E90022E6BBA770202E4522A2DD623D180E215BD1D1507FE3DC90CA310D27B3EFCCD8F83DE3052CAD1E48938C68D095AAC91B5F37E28BB49EC7ED597",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("141231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "EBFA0D5D06D8CE702DA3EAE890701D45E274C845",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("06", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "248".toInt(),
                module = convert.strToBcd(
                    "CB26FC830B43785B2BCE37C81ED334622F9622F4C89AAE641046B2353433883F307FB7C974162DA72F7A4EC75D9D657336865B8D3023D3D645667625C9A07A6B7A137CF0C64198AE38FC238006FB2603F41F4F3BB9DA1347270F2F5D8C606E420958C5F7D50A71DE30142F70DE468889B5E3A08695B938A50FC980393A9CBCE44AD2D64F630BB33AD3F5F5FD495D31F37818C1D94071342E07F1BEC2194F6035BA5DED3936500EB82DFDA6E8AFB655B1EF3D0D7EBF86B66DD9F29F6B1D324FE8B26CE38AB2013DD13F611E7A594D675C4432350EA244CC34F3873CBA06592987A1D7E852ADC22EF5A2EE28132031E48F74037E3B34AB747F",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("161231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "F910A1504D5FFB793D94F3B500765E1ABCAD72D9",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("EF", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "248".toInt(),
                module = convert.strToBcd(
                    "A191CB87473F29349B5D60A88B3EAEE0973AA6F1A082F358D849FDDFF9C091F899EDA9792CAF09EF28F5D22404B88A2293EEBBC1949C43BEA4D60CFD879A1539544E09E0F09F60F065B2BF2A13ECC705F3D468B9D33AE77AD9D3F19CA40F23DCF5EB7C04DC8F69EBA565B1EBCB4686CD274785530FF6F6E9EE43AA43FDB02CE00DAEC15C7B8FD6A9B394BABA419D3F6DC85E16569BE8E76989688EFEA2DF22FF7D35C043338DEAA982A02B866DE5328519EBBCD6F03CDD686673847F84DB651AB86C28CF1462562C577B853564A290C8556D818531268D25CC98A4CC6A0BDFFFDA2DCCA3A94C998559E307FDDF915006D9A987B07DDAEB3B",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "21766EBB0EE122AFB65D7845B73DB46BAB65427A",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("F0", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "208".toInt(),
                module = convert.strToBcd(
                    "999EA2D430D60614E100706C7DA213E1C77AD18C11BD70BC42CEBD80A3C94EC5E736D345EA7ADE2B9E0BC8816E567D39412EB728C2B2CCE73DEBC9FA25D4919BF5420C986083FBC0750895AFBA6B9DAA62B1B7D8439CF29E720D085D5D0962A9443B1F738E6560EF0EED7572815EA87A1B07570F119867DD6CC5D4DE06AA5373847D17A610ECF932FA2C94234E68AF84A9E0DAA18116B326016B70136F493482FEAE98E4AE682BF96C59279752248DEC915ED6F9BB73F9206155D961B50865E1CA6D47322FCE22DCF1957182B6E99CBB",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "B8EA49169B54F3B7FF0DF3A8B6388C82A1DBE730",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("F1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "176".toInt(),
                module = convert.strToBcd(
                    "A0DCF4BDE19C3546B4B6F0414D174DDE294AABBB828C5A834D73AAE27C99B0B053A90278007239B6459FF0BBCD7B4B9C6C50AC02CE91368DA1BD21AAEADBC65347337D89B68F5C99A09D05BE02DD1F8C5BA20E2F13FB2A27C41D3F85CAD5CF6668E75851EC66EDBF98851FD4E42C44C1D59F5984703B27D5B9F21B8FA0D93279FBBF69E090642909C9EA27F898959541AA6757F5F624104F6E1D3A9532F2A6E51515AEAD1B43B3D7835088A2FAFA7BE7",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "D8E68DA167AB5A85D8C3D55ECB9B0517A1A5B4BB",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("F3", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "144".toInt(),
                module = convert.strToBcd(
                    "98F0C770F23864C2E766DF02D1E833DFF4FFE92D696E1642F0A88C5694C6479D16DB1537BFE29E4FDC6E6E8AFD1B0EB7EA0124723C333179BF19E93F10658B2F776E829E87DAEDA9C94A8B3382199A350C077977C97AFF08FD11310AC950A72C3CA5002EF513FCCC286E646E3C5387535D509514B3B326E1234F9CB48C36DDD44B416D23654034A66F403BA511C5EFA3",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("091231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "A69AC7603DAF566E972DEDC2CB433E07E8B01A9A",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("F4", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "176".toInt(),
                module = convert.strToBcd(
                    "9E2F74BF4AB521019735BFC7E4CBC56B6F64AFF1ED7B79998EE5B3DFFE23DFC8E2DD0025575AF94DE814264528AF6F8005A538B3D6AE881B350F89595588E51F7423E711109DEC169FDD560602D80EF46E582C8C546C8930394BD534412A88CC9FF4DFC08AE716A595EF1AF7C32EDFCF996433EB3C36BCE093E44E0BDE228E0299A0E358BF28308DB4739815DD09F1E89654CC7CC193E2AC17C4DA335D904B8EC06ACFBDE083F76933C969672E9AFEA3",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("091231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "BF6B5B9C47134E494571732A4903C935874682B9",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("F5", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "248".toInt(),
                module = convert.strToBcd(
                    "A6E6FB72179506F860CCCA8C27F99CECD94C7D4F3191D303BBEE37481C7AA15F233BA755E9E4376345A9A67E7994BDC1C680BB3522D8C93EB0CCC91AD31AD450DA30D337662D19AC03E2B4EF5F6EC18282D491E19767D7B24542DFDEFF6F62185503532069BBB369E3BB9FB19AC6F1C30B97D249EEE764E0BAC97F25C873D973953E5153A42064BBFABFD06A4BB486860BF6637406C9FC36813A4A75F75C31CCA9F69F8DE59ADECEF6BDE7E07800FCBE035D3176AF8473E23E9AA3DFEE221196D1148302677C720CFE2544A03DB553E7F1B8427BA1CC72B0F29B12DFEF4C081D076D353E71880AADFF386352AF0AB7B28ED49E1E672D11F9",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("3", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("010001", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("091231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "C2239804C8098170BE52D6D5D4159E81CE8466BF",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("F8", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "128".toInt(),
                module = convert.strToBcd(
                    "A1F5E1C9BD8650BD43AB6EE56B891EF7459C0A24FA84F9127D1A6C79D4930F6DB1852E2510F18B61CD354DB83A356BD190B88AB8DF04284D02A4204A7B6CB7C5551977A9B36379CA3DE1A08E69F301C95CC1C20506959275F41723DD5D2925290579E5A95B0DF6323FC8E9273D6F849198C4996209166D9BFC973C361CC826E1",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "F06ECC6D2AAEBF259B7E755A38D9A9B24E2FF3DD",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            ), Capk(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("FA", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                hashArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                rsaArithmeticIndex = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                moduleLength = "144".toInt(),
                module = convert.strToBcd(
                    "A90FCD55AA2D5D9963E35ED0F440177699832F49C6BAB15CDAE5794BE93F934D4462D5D12762E48C38BA83D8445DEAA74195A301A102B2F114EADA0D180EE5E7A5C73E0C4E11F67A43DDAB5D55683B1474CC0627F44B8D3088A492FFAADAD4F42422D0E7013536C3C49AD3D0FAE96459B0F6B1B6056538A3D6D44640F94467B108867DEC40FAAECD740C00E2B7A8852D",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                exponentLength = convert.strToBcd("1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                exponent = convert.strToBcd("03", IConvert.EPaddingPosition.PADDING_RIGHT),
                expireDate = convert.strToBcd("151231", IConvert.EPaddingPosition.PADDING_RIGHT),
                checkSum = convert.strToBcd(
                    "5BED4068D96EA16D2D77E03D6036FC7A160EA99C",
                    IConvert.EPaddingPosition.PADDING_RIGHT
                )
            )
        )

        val revokedCertificationList: ArrayList<CapkRevoke> = arrayListOf(
            CapkRevoke(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("F0", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                certificateSN = convert.strToBcd("000711", IConvert.EPaddingPosition.PADDING_RIGHT)
            ), CapkRevoke(
                rid = convert.strToBcd("A000000004", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("F1", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                certificateSN = convert.strToBcd("000711", IConvert.EPaddingPosition.PADDING_RIGHT)
            ), CapkRevoke(
                rid = convert.strToBcd("A000000025", IConvert.EPaddingPosition.PADDING_RIGHT),
                keyId = convert.strToBcd("C8", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                certificateSN = convert.strToBcd("000711", IConvert.EPaddingPosition.PADDING_RIGHT)
            )
        )

        val capkParam = CapkParam(
            capkList = capkList, capkRevokeList = revokedCertificationList
        )

        println("Capk List: ${capkParam.capkList}\nCapk Revoke List: ${capkParam.capkRevokeList}")

        //EMV CONTACT
        val emvAidList: ArrayList<EmvAid> = arrayListOf(
            EmvAid(
                partialAIDSelection = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                applicationID = convert.strToBcd(
                    "A0000000031010", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                localAIDName = "VISA Debito/Credito",
                tacDenial = convert.strToBcd("0010000000", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacOnline = convert.strToBcd("D84004F800", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacDefault = convert.strToBcd(
                    "D84000A800", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalAIDVersion = convert.strToBcd(
                    "008c", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                floorLimit = "000000".toLong(),
                threshold = "0".toLong(),
                targetPercentage = convert.strToBcd("0", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                maxTargetPercentage = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                terminalDefaultTDOL = convert.strToBcd(
                    "0F9F02065F2A029A039C0195059F3704", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalDefaultDDOL = convert.strToBcd(
                    "039F3704", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalRiskManagementData = convert.strToBcd(
                    "", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalType = convert.strToBcd("22", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                cardDataInputCapability = convert.strToBcd(
                    "E0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cvmCapability = convert.strToBcd("B8", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                securityCapability = convert.strToBcd(
                    "C8", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                additionalTerminalCapabilities = convert.strToBcd(
                    "FF80F0A001", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                getDataForPINTryCounter = "1".toInt(),
                bypassPINEntry = "1".toInt(),
                subsequentBypassPINEntry = "0".toInt(),
                forcedOnlineCapability = "0".toInt()
            ), EmvAid(
                partialAIDSelection = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                applicationID = convert.strToBcd(
                    "A0000000032010", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                localAIDName = "VISA Electron",
                tacDenial = convert.strToBcd("0010000000", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacOnline = convert.strToBcd("D84004F800", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacDefault = convert.strToBcd(
                    "D84000A800", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalAIDVersion = convert.strToBcd(
                    "008c", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                floorLimit = "000000".toLong(),
                threshold = "0".toLong(),
                targetPercentage = convert.strToBcd("0", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                maxTargetPercentage = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                terminalDefaultTDOL = convert.strToBcd(
                    "0F9F02065F2A029A039C0195059F3704", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalDefaultDDOL = convert.strToBcd(
                    "039F3704", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalRiskManagementData = convert.strToBcd(
                    "", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalType = convert.strToBcd("22", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                cardDataInputCapability = convert.strToBcd(
                    "E0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cvmCapability = convert.strToBcd("B8", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                securityCapability = convert.strToBcd(
                    "C8", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                additionalTerminalCapabilities = convert.strToBcd(
                    "FF80F0A001", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                getDataForPINTryCounter = "1".toInt(),
                bypassPINEntry = "1".toInt(),
                subsequentBypassPINEntry = "0".toInt(),
                forcedOnlineCapability = "0".toInt()
            ), EmvAid(
                partialAIDSelection = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                applicationID = convert.strToBcd(
                    "A000000004", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                localAIDName = "MasterCard",
                tacDenial = convert.strToBcd("0010000000", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacOnline = convert.strToBcd("D84004F800", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacDefault = convert.strToBcd(
                    "D84000A800", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalAIDVersion = convert.strToBcd(
                    "0002", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                floorLimit = "000000".toLong(),
                threshold = "0".toLong(),
                targetPercentage = convert.strToBcd("0", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                maxTargetPercentage = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                terminalDefaultTDOL = convert.strToBcd(
                    "0F9F02065F2A029A039C0195059F3704", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalDefaultDDOL = convert.strToBcd(
                    "039F3704", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalRiskManagementData = convert.strToBcd(
                    "", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalType = convert.strToBcd("22", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                cardDataInputCapability = convert.strToBcd(
                    "E0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cvmCapability = convert.strToBcd("B8", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                securityCapability = convert.strToBcd(
                    "C8", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                additionalTerminalCapabilities = convert.strToBcd(
                    "FF80F0A001", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                getDataForPINTryCounter = "1".toInt(),
                bypassPINEntry = "1".toInt(),
                subsequentBypassPINEntry = "0".toInt(),
                forcedOnlineCapability = "0".toInt()
            ), EmvAid(
                partialAIDSelection = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                applicationID = convert.strToBcd(
                    "A0000000041010", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                localAIDName = "MasterCard Credito/Debito",
                tacDenial = convert.strToBcd("0400000000", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacOnline = convert.strToBcd("F850ACF800", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacDefault = convert.strToBcd(
                    "FC50ACA000", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalAIDVersion = convert.strToBcd(
                    "0002", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                floorLimit = "000000".toLong(),
                threshold = "0".toLong(),
                targetPercentage = convert.strToBcd("0", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                maxTargetPercentage = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                terminalDefaultTDOL = convert.strToBcd(
                    "0F9F02065F2A029A039C0195059F3704", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalDefaultDDOL = convert.strToBcd(
                    "039F3704", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalRiskManagementData = convert.strToBcd(
                    "", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalType = convert.strToBcd("22", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                cardDataInputCapability = convert.strToBcd(
                    "E0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cvmCapability = convert.strToBcd("B8", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                securityCapability = convert.strToBcd(
                    "C8", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                additionalTerminalCapabilities = convert.strToBcd(
                    "FF80F0A001", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                getDataForPINTryCounter = "1".toInt(),
                bypassPINEntry = "1".toInt(),
                subsequentBypassPINEntry = "0".toInt(),
                forcedOnlineCapability = "0".toInt()
            )
        )

        println("EMV Aids Param: $emvAidList")

        //COMMON CONFIG
        val emvConfig = Config(
            merchantId = "TecnologiaTransaccionalTestMerchant",
            merchantCategoryCode = convert.strToBcd(
                "5399", IConvert.EPaddingPosition.PADDING_RIGHT
            ),
            merchantNameAndLocation = "TT-APP,Guatemala City",
            terminalCountryCode = convert.strToBcd("0320", IConvert.EPaddingPosition.PADDING_RIGHT),
            terminalCurrencySymbol = "Q",
            transReferenceCurrencyCode = convert.strToBcd(
                "0320", IConvert.EPaddingPosition.PADDING_RIGHT
            ),
            transReferenceCurrencyExponent = convert.strToBcd(
                "2", IConvert.EPaddingPosition.PADDING_LEFT
            )[0],
            conversionRatio = "1".toLong()
        )

        println("Emv Configs: $emvConfig")

        //PAYWAVE
        val paywaveAidList = arrayListOf(
            PayWaveAid(
                localAidName = "VSDC",
                applicationId = convert.strToBcd(
                    "A0000000031010", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                partialAidSelection = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cryptogramVersion17Supported = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                zeroAmountNoAllowed = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                statusCheckSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                readerTtq = convert.strToBcd("B600C000", IConvert.EPaddingPosition.PADDING_RIGHT),
                securityCapability = convert.strToBcd(
                    "C8", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                termType = convert.strToBcd("22", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                arrayListOf(
                    PayWaveInterFloorLimit(
                        transactionType = convert.strToBcd(
                            "00", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessCvmLimit = "999999999".toLong(),
                        contactlessTransactionLimit = "999999999".toLong(),
                        contactlessFloorLimit = "0000".toLong(),
                        contactlessTransactionLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        cvmLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessFloorLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0]
                    ), PayWaveInterFloorLimit(
                        transactionType = convert.strToBcd(
                            "01", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessCvmLimit = "999999999".toLong(),
                        contactlessTransactionLimit = "999999999".toLong(),
                        contactlessFloorLimit = "0000".toLong(),
                        contactlessTransactionLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        cvmLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessFloorLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0]
                    ), PayWaveInterFloorLimit(
                        transactionType = convert.strToBcd(
                            "09", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessCvmLimit = "999999999".toLong(),
                        contactlessTransactionLimit = "999999999".toLong(),
                        contactlessFloorLimit = "0000".toLong(),
                        contactlessTransactionLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        cvmLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessFloorLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0]
                    ), PayWaveInterFloorLimit(
                        transactionType = convert.strToBcd(
                            "20", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessCvmLimit = "999999999".toLong(),
                        contactlessTransactionLimit = "999999999".toLong(),
                        contactlessFloorLimit = "0000".toLong(),
                        contactlessTransactionLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        cvmLimitSupported = convert.strToBcd(
                            "0", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessFloorLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0]
                    )
                )
            ), PayWaveAid(
                localAidName = "ELECTRON",
                applicationId = convert.strToBcd(
                    "A0000000032010", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                partialAidSelection = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cryptogramVersion17Supported = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                zeroAmountNoAllowed = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                statusCheckSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                readerTtq = convert.strToBcd("36008000", IConvert.EPaddingPosition.PADDING_RIGHT),
                securityCapability = convert.strToBcd(
                    "C8", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                termType = convert.strToBcd("22", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                arrayListOf(
                    PayWaveInterFloorLimit(
                        transactionType = convert.strToBcd(
                            "00", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessCvmLimit = "999999999".toLong(),
                        contactlessTransactionLimit = "999999999".toLong(),
                        contactlessFloorLimit = "0000".toLong(),
                        contactlessTransactionLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        cvmLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessFloorLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0]
                    ), PayWaveInterFloorLimit(
                        transactionType = convert.strToBcd(
                            "01", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessCvmLimit = "999999999".toLong(),
                        contactlessTransactionLimit = "999999999".toLong(),
                        contactlessFloorLimit = "0000".toLong(),
                        contactlessTransactionLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        cvmLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessFloorLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0]
                    ), PayWaveInterFloorLimit(
                        transactionType = convert.strToBcd(
                            "09", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessCvmLimit = "999999999".toLong(),
                        contactlessTransactionLimit = "999999999".toLong(),
                        contactlessFloorLimit = "0".toLong(),
                        contactlessTransactionLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        cvmLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessFloorLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0]
                    ), PayWaveInterFloorLimit(
                        transactionType = convert.strToBcd(
                            "20", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessCvmLimit = "999999999".toLong(),
                        contactlessTransactionLimit = "999999999".toLong(),
                        contactlessFloorLimit = "0".toLong(),
                        contactlessTransactionLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        cvmLimitSupported = convert.strToBcd(
                            "0", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0],
                        contactlessFloorLimitSupported = convert.strToBcd(
                            "1", IConvert.EPaddingPosition.PADDING_LEFT
                        )[0]
                    )
                )
            )
        )

        val programidList = arrayListOf(
            PayWaveProgramId(
                programId = convert.strToBcd(
                    "60098008040000000000000000000000", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                contactlessCvmLimit = "999999999".toLong(),
                contactlessTransactionLimit = "999999999".toLong(),
                contactlessFloorLimit = "0000".toLong(),
                contactlessTransactionLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cvmLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                contactlessFloorLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cryptogramVersion17Supported = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                zeroAmountNoAllowed = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                statusCheckSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                readerTtq = convert.strToBcd("36008000", IConvert.EPaddingPosition.PADDING_RIGHT)
            ), PayWaveProgramId(
                programId = convert.strToBcd("000001", IConvert.EPaddingPosition.PADDING_RIGHT),
                contactlessCvmLimit = "999999999".toLong(),
                contactlessTransactionLimit = "999999999".toLong(),
                contactlessFloorLimit = "0000".toLong(),
                contactlessTransactionLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cvmLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                contactlessFloorLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cryptogramVersion17Supported = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                zeroAmountNoAllowed = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                statusCheckSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                readerTtq = convert.strToBcd("36008000", IConvert.EPaddingPosition.PADDING_RIGHT)
            ), PayWaveProgramId(
                programId = convert.strToBcd("000002", IConvert.EPaddingPosition.PADDING_RIGHT),
                contactlessCvmLimit = "999999999".toLong(),
                contactlessTransactionLimit = "999999999".toLong(),
                contactlessFloorLimit = "0000".toLong(),
                contactlessTransactionLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cvmLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                contactlessFloorLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cryptogramVersion17Supported = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                zeroAmountNoAllowed = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                statusCheckSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                readerTtq = convert.strToBcd("36008000", IConvert.EPaddingPosition.PADDING_RIGHT)
            )
        )

        val paywaveParams = PayWaveParam(
            payWaveAidArrayList = paywaveAidList, waveProgramIdArrayList = programidList
        )

        println("Paywave Aid: ${paywaveParams.payWaveAidArrayList}, Paywave ProgramID: ${paywaveParams.waveProgramIdArrayList}")

        //MC
        val paypassParam = arrayListOf(
            PayPassAid(
                localAidName = "MCHIP",
                applicationId = convert.strToBcd(
                    "A0000000041010", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                partialAIDSelection = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                terminalAidVersion = convert.strToBcd(
                    "0002", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                tacDenial = convert.strToBcd("0400000000", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacOnline = convert.strToBcd("F850ACF800", IConvert.EPaddingPosition.PADDING_RIGHT),
                tacDefault = convert.strToBcd(
                    "FC50ACA000", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                terminalRisk = convert.strToBcd(
                    "08F8800000000000", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                contactlessCvmLimit = "20000".toLong(),
                contactlessTransactionLimitNoOnDevice = "999999999".toLong(),
                contactlessTransactionLimitOnDevice = "999999999".toLong(),
                contactlessFloorLimit = "0000".toLong(),
                contactlessTransactionLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                contactlessCvmLimitSupported = convert.strToBcd(
                    "1", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                contactlessFloorLimitSupported = convert.strToBcd(
                    "0", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                kernelConfiguration = convert.strToBcd(
                    "16", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                tornLeftTime = convert.strToBcd("06", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                maximumTornNumber = convert.strToBcd(
                    "2", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                cardDataInput = convert.strToBcd("E0", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                magneticCvm = convert.strToBcd("60", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                mageticNoCvm = convert.strToBcd("00", IConvert.EPaddingPosition.PADDING_LEFT)[0],

                cvmCapabilityCvmRequired = convert.strToBcd(
                    "48", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],//48
                cvmCapabilityNoCvmRequired = convert.strToBcd(
                    "08", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                securityCapability = convert.strToBcd(
                    "08", IConvert.EPaddingPosition.PADDING_LEFT
                )[0],
                additionalTerminalCapability = convert.strToBcd(
                    "E000F0A001", IConvert.EPaddingPosition.PADDING_RIGHT
                ),
                kernelId = convert.strToBcd("02", IConvert.EPaddingPosition.PADDING_LEFT)[0],
                terminalType = convert.strToBcd("22", IConvert.EPaddingPosition.PADDING_LEFT)[0],
            )
        )

        println("Paypass Param: $paypassParam")

//Init SDK
        sdkTTPax.initPaxSdk(capkParam = capkParam,
            emvAidList = emvAidList,
            configParam = emvConfig,
            paramPayWave = paywaveParams,
            paramPayPassAids = paypassParam,
            onSuccess = {
                val tik16Clr = byteArrayOf(
                    0x6A.toByte(),
                    0xC2.toByte(),
                    0x92.toByte(),
                    0xFA.toByte(),
                    0xA1.toByte(),
                    0x31.toByte(),
                    0x5B.toByte(),
                    0x4D.toByte(),
                    0x85.toByte(),
                    0x8A.toByte(),
                    0xB3.toByte(),
                    0xA3.toByte(),
                    0xD7.toByte(),
                    0xD5.toByte(),
                    0x93.toByte(),
                    0x3A.toByte()
                )
                val ksn = byteArrayOf(
                    0xff.toByte(),
                    0xff.toByte(),
                    0x98.toByte(),
                    0x76.toByte(),
                    0x54.toByte(),
                    0x32.toByte(),
                    0x10.toByte(),
                    0xE0.toByte(),
                    0x00.toByte(),
                    0x00.toByte()
                )

                PedApiUtils.eraseKey()

                val status = PedApiUtils().writeKeys(
                    groupIndex = 1.toByte(),
                    srcKeyIndex = 0.toByte(),
                    keyValue = tik16Clr,
                    ksn = ksn,
                    checkMode = ECheckMode.KCV_NONE,
                    checkBuf = null as kotlin.ByteArray?
                )

                Log.i(TAG, "initSdk: writeKeys status $status")
            },
            onFeature = {
                println("Error: $it")
            })
    }

    //Config Trans
    private fun initEmvTransaction(amount: Long, readType: EReaderType = EReaderType.MAG_ICC_PICC) {
        transType = 0x00.toByte()
        transAmt = amount
        val otherAmount = ("0".toDouble() * 100).toLong()
        otherAmt = otherAmount

        glStatus.GetInstance().tranEMVTags.Clear()
        glStatus.GetInstance().tranEMVResponseTags.Clear()


        if (transProcessPresenter == null) {
            transProcessPresenter = TransProcessPresenter()
            transProcessPresenter?.attachView(this)
        }

        transPreProcess(true)

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

        val codAuth: String = "072745"
        glStatus.GetInstance().AuthCode = codAuth

        if (glStatus.GetInstance().tranEMVResponseTags.ContainsTag(0x91)) {
            val authData = glStatus.GetInstance().tranEMVResponseTags.GetTag(0x91).Value()
            glStatus.GetInstance().issuerRspData.authData = authData //TAG:91
        }

        if (codAuth.isNotEmpty()) {
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
//        transProcessPresenter!!.startOnlinePin()

        if (transProcessPresenter != null) {
            if (isSecondTap) { //visa card and other card(not contain master card) 2nd detect card
                isSecondTap = false
                //transProcessPresenter?.completeClssTrans(glStatus.GetInstance().issuerRspData)
                processCvm()
                transProcessPresenter!!.startOnlinePin()

            } else {
                transProcessPresenter?.startClssTrans() // first time detect card finish
            }
        }
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

        val popView = layoutInflater.inflate(R.layout.dlg_enter_pin, null)
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
                    //FRS 2020-08-23 Se agrego el runOnUiThread para evitar errores por threads
                    runOnUiThread {
                        Toast.makeText(
                            this, "getString pinblock err: $pinResult", Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    checkTransResult()
                }
            }
        }

        mEnterPinPopWindow!!.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        mEnterPinPopWindow!!.showAtLocation(
            binding.viewBottom, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0
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
                val amount = 50000L
                initEmvTransaction(amount)
            }
        }
    }

    override fun onEnterDUKPTPinFinish(dukptResult: DUKPTResult) {
        Log.i(
            TAG, "onEnterDUKPTPinFinish: Pinblock(${
                ConvertHelper.getConvert().bcdToStr(dukptResult.pinblock)
            })"
        )
        Log.i(
            TAG, "onEnterDUKPTPinFinish: KSN(${
                ConvertHelper.getConvert().bcdToStr(dukptResult.ksnUsed)
            })"
        )
        App.instance.runOnUiThread {
            if (mEnterPinPopWindow != null && mEnterPinPopWindow!!.isShowing) {
                mEnterPinPopWindow?.dismiss()
            }
            if (dukptResult.codeResult == EnterPinResult.RET_SUCC || dukptResult.codeResult == EnterPinResult.RET_CANCEL || pinResult == EnterPinResult.RET_TIMEOUT || pinResult == EnterPinResult.RET_PIN_BY_PASS || pinResult == EnterPinResult.RET_OFFLINE_PIN_READY || pinResult == EnterPinResult.RET_NO_KEY) {
                Utils.logsUtils("to do nothing", 0)
            } else {
                Utils.logsUtils(pinResult.toString())
            }
        }
    }

    override fun onUpdatePinLen(pin: String?) {
        Utils.logsUtils("onUpdatePinLen: $pin")
        App.instance.runOnUiThread {
            if (pinText != null) {
                pinText!!.text = pin
            }
        }
    }

    override fun getEnteredPin() = pinText?.text?.toString() ?: ""

    override fun onEnterPinFinish(pinResult: Int) {
        Utils.logsUtils("onEnterPinFinish: $pinResult")
        this.pinResult = pinResult
        App.instance.runOnUiThread {
            if (mEnterPinPopWindow != null && mEnterPinPopWindow!!.isShowing) {
                mEnterPinPopWindow?.dismiss()
            }
            if (pinResult == EnterPinResult.RET_SUCC || pinResult == EnterPinResult.RET_CANCEL || pinResult == EnterPinResult.RET_TIMEOUT || pinResult == EnterPinResult.RET_PIN_BY_PASS || pinResult == EnterPinResult.RET_OFFLINE_PIN_READY || pinResult == EnterPinResult.RET_NO_KEY) {
                Utils.logsUtils("to do nothing", 0)
            } else {
                Utils.logsUtils(pinResult.toString())
            }
        }
    }

    override fun onStartEnterPin(prompt: String) {
        Utils.logsUtils("onStartEnterPin: $prompt", 0)
        App.instance.runOnUiThread { displayEnterPinDlg(prompt) }
    }

    override fun onTransFinish(transResult: TransResult?) {
        currTransResultEnum = transResult!!.transResult
        currentTxnCVMResult = transResult.cvmResult
        currTransResultCode = transResult.resultCode

        Utils.logsUtils(
            "onTransFinish,retCode: $currTransResultCode, transResult: $currTransResultEnum, cvm result: ${transResult.cvmResult}"
        )

        getFirstGACTag()

        if (transResult.resultCode == RetCode.EMV_OK) {
            processCvm()
        } else {
            processTransResult(transResult)
        }
    }

    override fun onCompleteTrans(p0: TransResult?) {
        TODO("Not yet implemented")
    }

    override fun onRemoveCard() {
        Log.i(
            TAG, "onRemoveCard: POR FAVOR RETIRE SU TARJETA"
        )
    }

    override fun onReadCardOK() {
        Log.i(TAG, "onReadCardOK")
    }
}