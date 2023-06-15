package com.techun.demoemvttpax

import java.util.*

fun toHexString(byteArray: ByteArray?): String? {
    val hexString = StringBuilder()
    if (byteArray == null || byteArray.isEmpty()) return null
    for (i in byteArray.indices) {
        val v = byteArray[i].toInt() and 0xFF
        val hv = Integer.toHexString(v)
        if (hv.length < 2) {
            hexString.append(0)
        }
        hexString.append(hv)
    }
    return hexString.toString().lowercase(Locale.getDefault())
}

fun bcd2Str(bytes: ByteArray): String? {
    val temp = StringBuffer(bytes.size * 2)
    for (i in bytes.indices) {
        temp.append((bytes[i].toInt() and 0xf0 ushr 4).toByte().toInt())
        temp.append((bytes[i].toInt() and 0x0f).toByte().toInt())
    }
    return if (temp.toString().substring(0, 1).equals("0", ignoreCase = true)) temp.toString()
        .substring(1) else temp.toString()
}