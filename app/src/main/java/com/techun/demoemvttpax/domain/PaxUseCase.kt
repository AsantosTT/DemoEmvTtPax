package com.techun.demoemvttpax.domain

import com.techun.demoemvttpax.domain.repository.PaxRepository
import com.techun.demoemvttpax.utils.DataState
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayPassAid
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.clss.PayWaveParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.CapkParam
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.common.Config
import com.tecnologiatransaccional.ttpaxsdk.sdk_pax.module_emv.xmlparam.entity.contact.EmvAid
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PaxUseCase @Inject constructor(private val paxRepository: PaxRepository) {
    suspend operator fun invoke(
        capkParam: CapkParam,
        emvAidList: ArrayList<EmvAid>,
        emvConfig: Config,
        paywaveParams: PayWaveParam,
        paypassParam: ArrayList<PayPassAid>
    ): Flow<DataState<Boolean>> =
        paxRepository.initSdk(capkParam, emvAidList, emvConfig, paywaveParams, paypassParam)

}