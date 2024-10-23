package com.techun.demoemvttpax.utils.pinutils

import com.pax.dal.entity.DUKPTResult

data class DUKPTResult(
    var codeResult: Int = 0,
    var ksnUsed: ByteArray? = null,
    var pinblock: ByteArray? = null
) : DUKPTResult(ksnUsed, pinblock) {

}