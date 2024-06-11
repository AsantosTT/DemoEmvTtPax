package com.techun.demoemvttpax.data

import android.content.Context
import android.os.SystemClock
import com.pax.dal.entity.EBeepMode
import com.pax.dal.entity.EPiccRemoveMode
import com.pax.dal.entity.EPiccType
import com.pax.dal.exceptions.PiccDevException
import com.pax.jemv.device.DeviceManager
import com.techun.demoemvttpax.domain.repository.TransProcessContractRepository
import com.techun.demoemvttpax.utils.DataState
import com.techun.demoemvttpax.utils.EmvDataState
import com.tecnologiatransaccional.ttpaxsdk.TTPaxApi
import com.tecnologiatransaccional.ttpaxsdk.neptune.Sdk
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvProcessParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.param.EmvTransParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.IStatusListener
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.EmvProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contact.IEmvTransProcessListener
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.ClssProcess
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.contactless.IClssStatusListener
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.IssuerRspData
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.process.entity.TransResult
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.utils.DeviceImplNeptune
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransProcessContractImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdk: TTPaxApi,
    private val iEmvTransProcessListener: IEmvTransProcessListener
) : TransProcessContractRepository {

    private var needShowRemoveCard: Boolean = true


    private fun isCardRemove(): Boolean {
        return try {
            sdk.getDal(context)?.getPicc(EPiccType.INTERNAL)
                ?.remove(EPiccRemoveMode.REMOVE, 0.toByte())
            true
        } catch (e: PiccDevException) {
            println("isCardRemove : ${e.message}")
            false
        }
    }


    override suspend fun preTrans(
        transParam: EmvTransParam?, needContact: Boolean
    )/*: DataState<Unit> */ {
        return withContext(Dispatchers.Default) {
            try {

                val clssStatusListener = IClssStatusListener {
                    while (!isCardRemove()) {
                        if (needShowRemoveCard) {
                            //Remove Card
                            needShowRemoveCard = false
//                            DataState.RemoveCard()
                        }
                    }
                }

                val statusListener = IStatusListener {
                    //        mView.onReadCardOK()
//                    DataState.ReadCardOK
                    sdk.getDal(context)?.sys?.beep(EBeepMode.FREQUENCE_LEVEL_5, 100)
                    SystemClock.sleep(750) //blue yellow green clss light remain lit for a minimum of approximately 750ms
                }

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

                ret = ClssProcess.getInstance().preTransProcess(
                    EmvProcessParam.Builder(transParam, configParam, capkParam)
                        .setPayPassAidList(Sdk.instance!!.paramManager!!.payPassAidList)
                        .setPayWaveParam(Sdk.instance!!.paramManager!!.payWaveParam).create()
                )

                println("$TAG: transPreProcess, clss ret:$ret")

                needShowRemoveCard = true

                ClssProcess.getInstance().registerClssStatusListener(clssStatusListener)
                ClssProcess.getInstance().registerStatusListener(statusListener)

                DataState.RemoveCard

            } catch (e: Exception) {
                DataState.Error(e)
            }
        }
    }

    override suspend fun startEmvTrans(): DataState<TransResult> {
        return withContext(Dispatchers.Default) {
            try {
                EmvProcess.getInstance().registerEmvProcessListener(iEmvTransProcessListener)
                val deviceImplNeptune = DeviceImplNeptune.getInstance()
                DeviceManager.getInstance().setIDevice(deviceImplNeptune)
                val transResult = EmvProcess.getInstance().startTransProcess()
                DataState.Success(transResult)
            } catch (e: Exception) {
                DataState.Error(e)
            }
        }
    }

    override suspend fun startClssTrans(): DataState<TransResult> {
        return withContext(Dispatchers.Default) {
            try {
                val deviceImplNeptune: DeviceImplNeptune = DeviceImplNeptune.getInstance()
                DeviceManager.getInstance().setIDevice(deviceImplNeptune)
                val transResult: TransResult = ClssProcess.getInstance().startTransProcess()
                DataState.Success(transResult)
            } catch (e: Exception) {
                DataState.Error(e)
            }
        }
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