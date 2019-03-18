package com.example.webviewtest

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityHolder.activity = this

        manageWebView()
        startWidget()

        if (!isOverlayPermissionGranted()) {
            openOverlayPermissionWindow()
        }
    }

    private fun manageWebView() {
        myWebView.webViewClient = WebViewController(this, myWebView)
        myWebView.settings.javaScriptEnabled = true
        myWebView.loadUrl("http://www.youtube.com")
    }

    private fun startWidget() {
        val floatingPlayerServiceIntent = Intent(this, FloatingWidgetService::class.java)
        this.startService(floatingPlayerServiceIntent)
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
    }

    private fun openOverlayPermissionWindow() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 0)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action ?: 0 == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (myWebView.canGoBack()) {
                        //makeToast(myWebView.url)
                        myWebView.goBack()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun MainActivity.makeToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}
