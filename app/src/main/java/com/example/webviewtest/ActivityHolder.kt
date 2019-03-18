package com.example.webviewtest

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.WebView
import androidx.lifecycle.Lifecycle

@SuppressLint("StaticFieldLeak")
object ActivityHolder {
    var activity: Activity? = null
}