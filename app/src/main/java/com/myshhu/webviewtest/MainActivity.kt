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
import android.util.Base64
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
import java.io.InputStream
import java.io.InputStreamReader


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

    fun readHTMLFromUTF8File(inputStream: InputStream): String {
        try {
            val bufferedReader = BufferedReader(InputStreamReader(inputStream, "utf-8"))

            var currentLine: String? = bufferedReader.readLine()
            val sb = StringBuilder()

            while (currentLine != null) {
                sb.append(currentLine).append("\n")
                currentLine = bufferedReader.readLine()
            }

            return sb.toString()
        } catch (e: Exception) {
            throw RuntimeException("Can't parse HTML file.")
        } finally {
            inputStream.close()
        }
    }

    private fun manageWebView() {
        myWebView.webViewClient = WebViewController(this, myWebView)
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.webChromeClient = WebChromeClient()

        var htmlPage = readHTMLFromUTF8File(resources.openRawResource(R.raw.youtubeplayer))


        myWebView.loadDataWithBaseURL("https://www.youtube.com",
            htmlPage, "text/javascript", "utf-8", null)

        myWebView.webViewClient = object : WebViewClient() {

            var oldUrl: String = ""

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url);
                //makeToast("injected js")
                //view?.loadUrl("javascript:test()")
            }

            override fun shouldInterceptRequest(view: WebView, url: String?): WebResourceResponse? {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    var currentUrl: String = view.url ?: ""
                    currentUrl = clearUrl(currentUrl)

                    if (oldUrl != currentUrl) {
                        println("url changed from $oldUrl to $currentUrl, url: $url, videoId: ${getVideoId(currentUrl)}")
                        oldUrl = currentUrl
                        //injectJS()
                        //makeToast("injected js")
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

        myWebView.loadUrl("http://www.google.com")
    }

    public fun plsWorkClick(v: View) {
        //injectJS()
        //makeToast("injected js")
        myWebView?.loadUrl("javascript:test()")
    }

    private fun injectJS() {
        try {
            val inputStream = assets.open("style.js")
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            myWebView.loadUrl(
                "javascript:(function a() {" +
                "var parent = document.getElementsByTagName('head').item(0);" +
                "var script = document.createElement('script');" +
                "script.type = 'text/javascript';" +
                "script.innerHTML = window.atob('" + encoded + "');" +
                "parent.appendChild(script)" +
                "})()")
        }
        catch (e:Exception) {
            e.printStackTrace()
        }

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
