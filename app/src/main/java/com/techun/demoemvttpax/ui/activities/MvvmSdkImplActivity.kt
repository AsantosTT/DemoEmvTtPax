package com.techun.demoemvttpax.ui.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pax.dal.entity.EReaderType
import com.techun.demoemvttpax.R
import com.techun.demoemvttpax.databinding.ActivityMvvmSdkImplBinding
import com.techun.demoemvttpax.ui.viewmodel.PaxViewModel
import com.techun.demoemvttpax.utils.DataState
import com.techun.demoemvttpax.utils.keyboard.currency.CurrencyConverter
import com.techun.demoemvttpax.utils.keyboard.text.EditorActionListener
import com.techun.demoemvttpax.utils.keyboard.text.EnterAmountTextWatcher
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.glStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MvvmSdkImplActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMvvmSdkImplBinding
    private val viewModel: PaxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMvvmSdkImplBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initObservers()
        initListener()
    }

    private fun initViews() {
        binding.mainAmountEditText.requestFocus()
        binding.mainAmountEditText.setText("")
        binding.mainAmountEditText.isKeepKeyBoardOn = true

        val amountWatcher = EnterAmountTextWatcher()
        binding.mainAmountEditText.addTextChangedListener(amountWatcher)
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
                }

                is DataState.Error -> {
                    logs(dataState.exception.message)
                }

                else -> Unit
            }
        }

        viewModel.getEmv.observe(this) { dataState ->
            when (dataState) {
                is DataState.Success -> {
                    val cardDetails = dataState.data
                    logs("\nonMagDetectOK: ${getString(R.string.prompt_swipe_card)}")
                    logs("Get Track2: ${glStatus.GetInstance().Track2}")
                    logs("Get PAN: ${glStatus.GetInstance().PAN}")
                    logs("Get ExpirationDate: ${glStatus.GetInstance().ExpirationDate}")
                    logs("Get currentReaderType: ${glStatus.GetInstance().currentReaderType}")
                }

                is DataState.Error -> {
                    logs(dataState.exception.message)
                }

                else -> Unit
            }

        }

        viewModel.sdkInit()
    }

    private fun initListener() {
        binding.button.setOnClickListener(this)

        binding.mainAmountEditText.setOnEditorActionListener(object : EditorActionListener() {
            override fun onKeyOk() {
                // do trans
                if (binding.mainAmountEditText.getText() != null && binding.mainAmountEditText.text!!.length !== 0) {
                    val amount: Long =
                        CurrencyConverter.parse(binding.mainAmountEditText.getText().toString())
                    if (amount > 0) {
                        println(amount)
                    }
                }
            }

            override fun onKeyCancel() {
                // clear
                binding.mainAmountEditText.setText("")
            }
        })
    }


    private fun logs(msg: String?) {
        Log.d("logs-demo-app", "$msg")
        binding.tvLogs.append("$msg \n")
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.button -> {
                viewModel.startEmvTrans(EReaderType.MAG)
            }
        }
    }
}