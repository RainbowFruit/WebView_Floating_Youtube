package com.example.webviewtest

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.webkit.*
import android.widget.Toast
import android.os.Looper
import java.lang.Exception


class WebViewController(private val context: Context, private val webView: WebView) : WebViewClient() {

    private var oldUrl: String = ""
    private var floatingPlayerServiceIntent: Intent? = null


    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        view?.loadUrl(url)
        return true
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (oldUrl != webView.url) {
                println("url changed from $oldUrl to ${url}, videoId: ${getVideoId(webView.url)}")
                playVideo(getVideoId(webView.url))
                oldUrl = webView.url
            }
        }
        return super.shouldInterceptRequest(view, url)
    }

    private fun showToast(text: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
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

    private fun playVideo(videoId: String) {
        if(videoId != "") {
            floatingPlayerServiceIntent = Intent(context, FloatingWidgetService::class.java)
            floatingPlayerServiceIntent?.putExtra("videoId", videoId)
            context.startService(floatingPlayerServiceIntent)
        }
    }
}