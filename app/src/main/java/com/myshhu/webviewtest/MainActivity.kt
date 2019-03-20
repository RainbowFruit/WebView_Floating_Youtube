package com.myshhu.webviewtest

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityHolder.activity = this

        manageWebView()


        if (isOverlayPermissionDenied()) {
            showOverlayPermissionDialog()
        } else {
            startWidget()
        }
    }

    private fun manageWebView() {
        myWebView.webViewClient = WebViewController(this, myWebView)
        myWebView.settings.javaScriptEnabled = true
        myWebView.loadDataWithBaseURL(
            "https://www.youtube.com/player_api", ConstantStrings.getVideoHTML(),
            "text/html", null, null
        )
    }

    private fun startWidget() {
        val floatingPlayerServiceIntent = Intent(this, FloatingWidgetService::class.java)
        this.startService(floatingPlayerServiceIntent)
    }

    private fun isOverlayPermissionDenied() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)

    private fun openOverlayPermissionWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            if (isOverlayPermissionDenied()) {
                showOverlayPermissionDialog()
            } else {
                startWidget()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showOverlayPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Give permissions")
        builder.setMessage("Application don't have permissions to draw overlays that is needed for Floating Player, from where "
                + "you can take photos. Click 'Ok' and in next window give this application needed permissions.")

        builder.setPositiveButton("Ok") { _, _ ->
            openOverlayPermissionWindow()
        }

        builder.setOnCancelListener {
            if(isOverlayPermissionDenied()) {
                createOverlayNotAvailableToast()
                finish()
            }
        }

        builder.show()
    }

    private fun createOverlayNotAvailableToast() {
        makeToast("Draw over other app permission not available. Please give needed permissions for app to work.")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action ?: 0 == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (myWebView.canGoBack()) {
                        myWebView.goBack()
                    } else {
                        finish()
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
