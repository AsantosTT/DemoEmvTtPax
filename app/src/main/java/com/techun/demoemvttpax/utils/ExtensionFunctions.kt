package com.techun.demoemvttpax.utils

import android.app.Activity
import android.content.Intent
import android.widget.Toast


inline fun <reified T : Activity> Activity.goToActivity(
    noinline init: Intent.() -> Unit = {}, finish: Boolean = false
) {
    val intent = Intent(this, T::class.java)
    intent.init()
    startActivity(intent)
    if (finish) finish()
}


fun Activity.toast(text: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, length).show()
}