package com.myshhu.webviewtest

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse


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
        myWebView.webChromeClient = WebChromeClient()

       // myWebView.loadUrl("file:///android_asset/script.html")

        myWebView.webViewClient = object : WebViewClient() {

            var oldUrl: String = ""

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url);
                //injectJS()
                //makeToast("injected js")
            }

            override fun shouldInterceptRequest(view: WebView, url: String?): WebResourceResponse? {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    var currentUrl: String = view.url ?: ""
                    currentUrl = clearUrl(currentUrl)

                    if (oldUrl != currentUrl) {
                        println("url changed from $oldUrl to $currentUrl, url: $url, videoId: ${getVideoId(currentUrl)}")
                        oldUrl = currentUrl
                        injectJS()
                        makeToast("injected js")
                    }
                    //injectJS()
                    //makeToast("injected js")
                    //view.loadUrl("javascript:stopVideo();")
                }
                return super.shouldInterceptRequest(view, url)
            }

            private fun getVideoId(url: String): String {
                return try {
                    if (url.contains("watch")) {
                        url.split("&")[0].substring(30)
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }

            private fun clearUrl(currentUrl: String): String {
                var clearedUrl: String = currentUrl
                if (currentUrl.contains("#searching")) {
                    clearedUrl = currentUrl.replace("#searching", "")
                }
                return clearedUrl
            }
        }
        myWebView.loadUrl("http://www.youtube.com")
    }

    public fun plsWorkClick(v: View) {
        injectJS()
        makeToast("injected js")
    }

    private fun injectJS() {
        try {
            println("before opening")
            try {
                val inputStream = assets.open("style.js")

                println("inputStream: " + inputStream.toString())
                inputStream.bufferedReader().use(BufferedReader::readText)
            } catch (p: java.lang.Exception) {
                p.printStackTrace()
            }
        } catch (e: Exception) {
            null
        }?.let { myWebView.loadUrl("javascript:($it)()")  }
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
