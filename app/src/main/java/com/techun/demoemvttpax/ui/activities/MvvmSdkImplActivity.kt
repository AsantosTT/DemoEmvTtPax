package com.techun.demoemvttpax.ui.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pax.dal.entity.EReaderType
import com.pax.jemv.clcommon.RetCode
import com.techun.demoemvttpax.R
import com.techun.demoemvttpax.databinding.ActivityMvvmSdkImplBinding
import com.techun.demoemvttpax.ui.viewmodel.PaxViewModel
import com.techun.demoemvttpax.utils.DataState
import com.techun.demoemvttpax.utils.keyboard.currency.CurrencyConverter
import com.techun.demoemvttpax.utils.keyboard.text.EditorActionListener
import com.techun.demoemvttpax.utils.keyboard.text.EnterAmountTextWatcher
import com.techun.demoemvttpax.utils.toast
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk.Companion.instance
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.EmvProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.ConvertHelper
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
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
import com.tecnologiatransaccional.ttpaxsdk.utils.Utils.TXN_TYPE_PICC
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MvvmSdkImplActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMvvmSdkImplBinding
    private val viewModel: PaxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMvvmSdkImplBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initObservers()
        initListener()
        configSdk()
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

        viewModel.sdkInit(capkParam, emvAidList, emvConfig, paywaveParams, paypassParam)
    }

    private fun initViews() {
        binding.mainAmountEditText.requestFocus()
        binding.mainAmountEditText.setText("")
        binding.mainAmountEditText.isKeepKeyBoardOn = true

        val amountWatcher = EnterAmountTextWatcher()
        binding.mainAmountEditText.addTextChangedListener(amountWatcher)
    }

    private fun initListener() {
        binding.mainAmountEditText.setOnEditorActionListener(object : EditorActionListener() {
            override fun onKeyOk() {
                // do trans
                if (binding.mainAmountEditText.getText() != null && binding.mainAmountEditText.text!!.isNotEmpty()) {
                    val amount: Long =
                        CurrencyConverter.parse(binding.mainAmountEditText.getText().toString())
                    if (amount > 0) {
                        println(amount)
                        viewModel.loadEmvTransParam(transAmt = amount)
                    }
                }
            }

            override fun onKeyCancel() {
                // clear
                binding.mainAmountEditText.setText("")
            }
        })
    }

    private fun initObservers() {
        viewModel.sdkState.observe(this) { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    val ped = Sdk.isPaxDevice
                    logs("PAX PED: $ped")
                    logs("PAX DISPLAY: ${Build.DISPLAY}")
                    logs("PAX DEVICE: ${Build.DEVICE}")
                    logs("PAX MODEL: ${Build.MODEL}")
                    logs("PAX PRODUCT: ${Build.PRODUCT}")
                    logs("PAX MANUFACTURER: ${Build.MANUFACTURER}")

                    //Exitoso
                    println("PAX is working correctly")

                    val capk = instance!!.paramManager!!.capkParam
                    val emvAids = instance!!.paramManager!!.emvAidList
                    val configs = instance!!.paramManager!!.configParam
                    val paywaveParams = instance!!.paramManager!!.payWaveParam
                    val paypassParams = instance!!.paramManager!!.payPassAidList

                    println("CONFIGS")

                    println("Init Capk: $capk")
                    println("Init EmvAids: $emvAids")
                    println("Init Configs: $configs")
                    println("Init Paywave Aids: ${paywaveParams.payWaveAidArrayList}, Init Paywave ProgramId: ${paywaveParams.waveProgramIdArrayList}")
                    println("Init Paypass Params: $paypassParams")
                }

                is DataState.Error -> {
                    logs(dataState.exception.message)
                }

                else -> Unit
            }
        }

        viewModel.emvTransParam.observe(this) { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    val evmConfigs = dataState.data
                    viewModel.startEmvTrans(EReaderType.MAG_ICC_PICC, evmConfigs)
                }

                is DataState.Error -> {
                    println("Error: ${dataState.exception}")
                }

                else -> Unit
            }
        }

        viewModel.getEmv.observe(this) { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    val emvData = dataState.data
                    when (emvData.readerType) {
                        EReaderType.MAG -> {
                            logs("Get Track2: ${glStatus.GetInstance().Track2}")
                            logs("Get PAN: ${glStatus.GetInstance().PAN}")
                            logs("Get ExpirationDate: ${glStatus.GetInstance().ExpirationDate}")
                            logs("Get currentReaderType: ${glStatus.GetInstance().currentReaderType}")
                        }

                        EReaderType.ICC -> {
                            val cardDetails = dataState.data.dataPiccIcc
                            logs("onTransFinish,retCode: ${cardDetails?.resultCode}, transResult: ${cardDetails?.transResult}, cvm result: ${cardDetails?.cvmResult}")

                            if (cardDetails?.resultCode == RetCode.EMV_OK) {
                                logs("OK")


                            } else {
                                logs("processTransResult")
                            }
                        }

                        EReaderType.PICC -> {
                            val cardDetails = dataState.data.dataPiccIcc
                            logs("onTransFinish,retCode: ${cardDetails?.resultCode}, transResult: ${cardDetails?.transResult}, cvm result: ${cardDetails?.cvmResult}")

                            if (cardDetails?.resultCode == RetCode.EMV_OK) {
                                viewModel.processCvm(cardDetails, TXN_TYPE_PICC)
                            } else {
                                logs("processTransResult")
                            }
                        }

                        else -> Unit
                    }
                }

                is DataState.RemoveCard -> {
                    toast(getString(R.string.prompt_remove_card))
                }

                is DataState.ReadCardOK -> {
                    //DO Something
                }

                is DataState.Error -> {
                    logs(dataState.exception.message)
                }

                is DataState.Loading -> binding.fragmentProgressBar.visibility = VISIBLE

                is DataState.Finished -> binding.fragmentProgressBar.visibility = GONE

                else -> Unit
            }

        }

        viewModel.checkTransResult.observe(this) { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    val msgCvm = dataState.data
                    println("Cvm Status: $msgCvm")

                    viewModel.getBitmapVocher("TEST DE IMPRESION|VOUCHER TEST: Q.300.00")
                }

                is DataState.Error -> {
                    println("Error: ${dataState.exception}")
                }

                else -> Unit
            }
        }

        viewModel.getBitmapVocher.observe(this) { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    val img = dataState.data
                    viewModel.printVocher(img)
                }

                is DataState.Error -> {
                    toast(dataState.exception.message!!)
                }

                else -> Unit
            }
        }

        viewModel.printVocher.observe(this) { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    toast("Impresion Finalizada")
                }

                is DataState.Error -> {
                    toast(dataState.exception.message!!)
                }

                else -> Unit
            }
        }

    }

    private fun getFirstGACTag() {
        var data = com.pax.jemv.clcommon.ByteArray()
        var ret: Int = EmvProcess.getInstance().getTlv(0x95, data)
        if (ret == RetCode.EMV_OK) {
            val dataArr = ByteArray(data.length)
            System.arraycopy(data.data, 0, dataArr, 0, data.length)
            val firstGacTVR = ConvertHelper.getConvert().bcdToStr(dataArr)
        }

        data = com.pax.jemv.clcommon.ByteArray()
        ret = EmvProcess.getInstance().getTlv(0x9B, data)
        if (ret == RetCode.EMV_OK) {
            val dataArr = ByteArray(data.length)
            System.arraycopy(data.data, 0, dataArr, 0, data.length)
            val firstGacTSI = ConvertHelper.getConvert().bcdToStr(dataArr)
        }

        data = com.pax.jemv.clcommon.ByteArray()
        ret = EmvProcess.getInstance().getTlv(0x9F27, data)
        if (ret == RetCode.EMV_OK) {
            val dataArr = ByteArray(data.length)
            System.arraycopy(data.data, 0, dataArr, 0, data.length)
            val firstGacCID = ConvertHelper.getConvert().bcdToStr(dataArr)
        }
    }

    private fun logs(msg: String?) {
        Log.d("logs-demo-app", "$msg")
    }

}