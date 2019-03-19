package com.example.webviewtest

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList

class VideosQueue(private val chosenVideoId: String, private val maxAmountOfVideosInQueue: Int) {

    private lateinit var chosenVideoTitle: String
    private val apiKeysArray =
        arrayOf(
            com.example.webviewtest.APIKEYS.KEY,
            com.example.webviewtest.APIKEYS.KEY1
        )
    private var currentVideoPlaying: Int = 0
    private lateinit var videoIdsInQueue: MutableList<String>
    private var currentAPIKEY = 0
    private var apiKey = apiKeysArray[0]
    private var context: Context? = ActivityHolder.activity?.applicationContext

    init {
        createVideoQueue()
    }

    private fun createVideoQueue() {
        chosenVideoTitle = getChosenVideoTitle(chosenVideoId)
        val query = createQueryFromTitle()
        videoIdsInQueue = getVideosIdFromQuery(query)
        videoIdsInQueue.add(0, chosenVideoId)
    }

    private fun getChosenVideoTitle(chosenVideoId: String): String {
        val videoInformationObject = getVideoInformationObject(chosenVideoId, 1)

        return videoInformationObject?.getJSONArray("items")?.getJSONObject(0)?.getJSONObject("snippet")?.getString("title")
            ?: ""
    }

    private fun createQueryFromTitle(): String {
        return if (chosenVideoTitle.length > 15) {
            chosenVideoTitle.substring(0 until 15)
        } else {
            chosenVideoTitle
        }
    }

    private fun getVideoInformationObject(videoId: String, maxResults: Int = maxAmountOfVideosInQueue): JSONObject? {
        var videoInformationObject: JSONObject? = null
        var availableSpareAPIKeys = true

        while (availableSpareAPIKeys) {

            val connection = createConnectionWithQuery(videoId, maxResults)

            if (connection?.responseCode == HttpURLConnection.HTTP_OK) {
                videoInformationObject = createJSONObjectFromConnectionInputStream(connection)
                break
            } else if (connection?.responseCode == HttpURLConnection.HTTP_FORBIDDEN) { //FORBIDDEN = API Key limit reached
                availableSpareAPIKeys = isSpareAPIKeyAvailable()
            }
        }
        return videoInformationObject
    }

    private fun createConnectionWithQuery(searchQuery: String, maxResults: Int): HttpURLConnection? {
        try {
            val connection: HttpURLConnection
            val url = URL(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=" +
                        searchQuery + "&maxResults=" + maxResults + "&type=video&key=" + apiKey
            )
            connection = url.openConnection() as HttpURLConnection
            connection.connect()
            return connection
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun createJSONObjectFromConnectionInputStream(connection: HttpURLConnection): JSONObject {
        val connectionInputStream = connection.inputStream
        val bufferedReader = BufferedReader(InputStreamReader(connectionInputStream))
        val result = StringBuilder()
        bufferedReader.forEachLine {
            result.append(it).append("\n")
        }
        return JSONObject(result.toString())
    }

    private fun isSpareAPIKeyAvailable(): Boolean {
        return if (isCurrentKeyTheLastAvailableKey()) {
            createToast("API limits exceeded, try again later")
            changeCurrentAPIKEYToFirst()
            false
        } else {
            changeCurrentAPIKEYToNext()
            true
        }
    }

    private fun isCurrentKeyTheLastAvailableKey() = currentAPIKEY == apiKeysArray.size - 1

    private fun createToast(text: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun changeCurrentAPIKEYToFirst() {
        currentAPIKEY = 0
        apiKey = apiKeysArray[currentAPIKEY]
    }

    private fun changeCurrentAPIKEYToNext() {
        apiKey = apiKeysArray[++currentAPIKEY]
        Log.d("AppInfo", "Changed key to $currentAPIKEY")
    }

    private fun getVideosIdFromQuery(query: String): MutableList<String> {
        val videoInformationObject = getVideoInformationObject(query)
        val videoInformationArray = videoInformationObject?.getJSONArray("items")
        val videoIdsList: MutableList<String> = ArrayList()
        for (i in 0 until (videoInformationArray?.length() ?: 0)) {
            val id: String =
                videoInformationArray?.getJSONObject(i)?.getJSONObject("id")?.getString("videoId") ?: ""
            videoIdsList.add(id)
        }
        return videoIdsList
    }

    fun getNextVideo(): String {
        if (isNowPlayingLastVideoInQueue()) {
            currentVideoPlaying = 0
        } else {
            currentVideoPlaying++
        }
        return videoIdsInQueue[currentVideoPlaying]
    }

    private fun isNowPlayingLastVideoInQueue() = currentVideoPlaying + 1 >= videoIdsInQueue.size
}