package com.techun.demoemvttpax.data

import android.content.Context
import com.techun.demoemvttpax.domain.repository.TransProcessContractRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvProcessParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.EmvProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.ClssProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.IssuerRspData
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TransProcessContractImpl @Inject constructor(
    @ApplicationContext private val context: Context, private val sdk: TTPaxApi
) : TransProcessContractRepository {
    override suspend fun preTrans(transParam: EmvTransParam?, needContact: Boolean): Flow<DataState<Int>> = flow {
            try {
                var ret: Int
                val configParam: Config = Sdk.instance!!.paramManager!!.configParam
                val capkParam: CapkParam = Sdk.instance!!.paramManager!!.capkParam

                if (needContact) {
                    ret = EmvProcess.getInstance().preTransProcess(
                        EmvProcessParam.Builder(transParam, configParam, capkParam)
                            .setEmvAidList(Sdk.instance!!.paramManager!!.emvAidList).create()
                    )
                    println("$TAG: transPreProcess, emv ret:$ret")
                }

                //Aqui se cargan los parametros
                ret = ClssProcess.getInstance().preTransProcess(
                    EmvProcessParam.Builder(transParam, configParam, capkParam)
                        .setPayPassAidList(Sdk.instance!!.paramManager!!.payPassAidList)
                        .setPayWaveParam(Sdk.instance!!.paramManager!!.payWaveParam).create()
                )

                println("$TAG: transPreProcess, clss ret:$ret")
                DataState.Success(ret)
            } catch (e: Exception) {
                DataState.Error(e)
            }
        }

    override suspend fun startEmvTrans() {
        TODO("Not yet implemented")
    }

    override suspend fun startClssTrans() {
        TODO("Not yet implemented")
    }

    override suspend fun startMagTrans() {
        TODO("Not yet implemented")
    }

    override suspend fun startOnlinePin() {
        TODO("Not yet implemented")
    }

    override suspend fun completeEmvTrans(issuerRspData: IssuerRspData?) {
        TODO("Not yet implemented")
    }

    override suspend fun completeClssTrans(issuerRspData: IssuerRspData?) {
        TODO("Not yet implemented")
    }
}