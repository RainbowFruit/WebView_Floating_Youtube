package com.myshhu.webviewtest

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast


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
            var currentUrl: String = webView.url
            currentUrl = clearUrl(currentUrl)

            if (oldUrl != currentUrl) {
                println("url changed from $oldUrl to $currentUrl, url: $url, videoId: ${getVideoId(currentUrl)}")
                switchVideo(currentUrl)
            }
        }
        return super.shouldInterceptRequest(view, url)
    }

    private fun clearUrl(currentUrl: String): String {
        var clearedUrl: String = currentUrl
        if (currentUrl.contains("#searching")) {
            clearedUrl = currentUrl.replace("#searching", "")
        }
        return clearedUrl
    }

    private fun switchVideo(currentUrl: String) {
        if (currentUrl.contains("list")) {
            playVideo(getVideoId(currentUrl), getListId(currentUrl))
        } else {
            playVideo(getVideoId(currentUrl))
        }
        oldUrl = currentUrl
    }

    private fun getListId(currentUrl: String): String? {
        return if (currentUrl.contains("list")) {
            val tempString: List<String> = currentUrl.split("&")
            tempString.filter { it.contains("list") }[0]
                .replace("list=", "")
        } else {
            null
        }
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

    private fun playVideo(videoId: String, listId: String? = null) {
        if (videoId != "") {
            floatingPlayerServiceIntent = Intent(context, FloatingWidgetService::class.java)
            floatingPlayerServiceIntent?.putExtra("videoId", videoId)
            floatingPlayerServiceIntent?.putExtra("listId", listId)
            context.startService(floatingPlayerServiceIntent)
        }
    }
}