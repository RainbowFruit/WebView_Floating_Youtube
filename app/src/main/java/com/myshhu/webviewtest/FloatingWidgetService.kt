package com.myshhu.webviewtest

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView


class FloatingWidgetService : Service() {

    private lateinit var floatingView: View
    private var layoutParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    private var youTubePlayer: YouTubePlayer? = null
    private var videosQueue: VideosQueue? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        createNotification()
        createFloatingView()
        createLayoutParams()
        createWindowManager()
        addFloatingViewToWindowManager()
        getYouTubePlayerAndSetListener()

        setViewItemsListeners()

        registerScreenStatusReceiver()
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createOwnNotificationChannel()
        } else {
            startForeground(1, Notification())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createOwnNotificationChannel() {
        val notificationChannelId: String = packageName
        val channelName = "YoutubeService"
        val channel =
            NotificationChannel(
                notificationChannelId, channelName,
                NotificationManager.IMPORTANCE_NONE
            )

        channel.lightColor = Color.RED
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        //Create intent for app resume
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            ActivityHolder.activity?.intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Youtube service is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(2, notification)
    }

    private fun createFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)
    }

    private fun createLayoutParams() {
        val layoutFlag: Int = createLayoutFlag()
        //Create view params
        val createdLayoutParams: WindowManager.LayoutParams

        createdLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, 0,
            PixelFormat.TRANSLUCENT
        )
        createdLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE// | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        //Specify the view position
        //Initially view will be added to top-left corner
        createdLayoutParams.gravity = Gravity.TOP or Gravity.START
        createdLayoutParams.x = 70
        createdLayoutParams.y = 250

        this.layoutParams = createdLayoutParams
    }

    private fun createLayoutFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun createWindowManager() {
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager?
    }

    private fun addFloatingViewToWindowManager() {
        mWindowManager?.addView(floatingView, layoutParams)
    }

    private fun getYouTubePlayerAndSetListener() {
        val youTubePlayerView: YouTubePlayerView = floatingView.findViewById(R.id.ytPlayerView)
        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@FloatingWidgetService.youTubePlayer = youTubePlayer
                setYoutubePlayerEventsListener()
            }
        })
    }

    private fun setViewItemsListeners() {
        setButtonResizeOnTouchListener()
        setButtonMoveOnTouchListener()
        setButtonGoToPlayerOnClickListener()
        setButtonCloseOnClickListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setButtonResizeOnTouchListener() {
        val imgBtnResize: ImageButton = floatingView.findViewById(R.id.imgBtnResize)

        imgBtnResize.setOnTouchListener(object : View.OnTouchListener {
            var initialTouchX: Float = 0.0f
            var initialTouchY: Float = 0.0f
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {

                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val xDiff: Int = (event.rawX - initialTouchX).toInt()
                        val yDiff: Int = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(xDiff) > 9 || Math.abs(yDiff) > 9) {
                            layoutParams?.width = floatingView.width + xDiff
                            if(layoutParams?.width ?: 0 < 150) {
                                layoutParams?.width = floatingView.width - xDiff //Reverse resize
                            }
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            mWindowManager?.updateViewLayout(floatingView, layoutParams)
                        }
                    }
                }
                return false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setButtonMoveOnTouchListener() {
        val btnMove: Button = floatingView.findViewById(R.id.btnMove)

        btnMove.setOnTouchListener(object : View.OnTouchListener {

            var initialX: Int = 0
            var initialY: Int = 0
            var initialTouchX: Float = 0.0f
            var initialTouchY: Float = 0.0f
            var timeAfterTouch: Double = 0.0
            var startTime: Double = 0.0
            var widgetMoved: Boolean = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        widgetMoved = false

                        //Remember the initial position
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0

                        startTime = System.currentTimeMillis().toDouble()

                        //Get the touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val xDiff: Int = (event.rawX - initialTouchX).toInt()
                        val yDiff: Int = (event.rawY - initialTouchY).toInt()

                        //Calculate the X and Y coordinates of the view.
                        layoutParams?.x = initialX + xDiff
                        layoutParams?.y = initialY + yDiff
                        //Update the layout with new X & Y coordinate
                        mWindowManager?.updateViewLayout(floatingView, layoutParams)

                        if (xDiff > 10 || yDiff > 10) {
                            widgetMoved = true
                        }
                    }
                }
                timeAfterTouch = System.currentTimeMillis() - startTime
                if (timeAfterTouch > 1000 && !widgetMoved) {
                    //stopSelf()
                }
                return false
            }
        })
    }

    private fun setButtonGoToPlayerOnClickListener() {
        val btnGoToPlayer: Button = floatingView.findViewById(R.id.btnGoToPlayer)
        btnGoToPlayer.setOnClickListener {
            startActivity(ActivityHolder.activity?.intent?.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    private fun setButtonCloseOnClickListener() {
        val btnClose: Button = floatingView.findViewById(R.id.btnClose)
        btnClose.setOnClickListener {
            stopSelf()
        }
    }

    private fun setYoutubePlayerEventsListener() {
        val youTubePlayerListener = object : YouTubePlayerListener {

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                println("State changed")
                if (state == PlayerConstants.PlayerState.ENDED) {
                    println("State ended")
                    val nextVideoId: String = videosQueue?.getNextVideo() ?: ""
                    youTubePlayer.loadVideo(nextVideoId, 0f)
                }
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
            }

            override fun onPlaybackQualityChange(
                youTubePlayer: YouTubePlayer,
                playbackQuality: PlayerConstants.PlaybackQuality
            ) {
            }

            override fun onPlaybackRateChange(
                youTubePlayer: YouTubePlayer,
                playbackRate: PlayerConstants.PlaybackRate
            ) {
            }

            override fun onReady(youTubePlayer: YouTubePlayer) {
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
            }

            override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {
            }

            override fun onVideoLoadedFraction(youTubePlayer: YouTubePlayer, loadedFraction: Float) {
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
            }

            override fun onApiChange(youTubePlayer: YouTubePlayer) {
            }
        }
        youTubePlayer?.addListener(youTubePlayerListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val extras = intent?.extras
        if (extras != null) { //Check if widget is started for video to play
            onWidgetStart(extras)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun onWidgetStart(extras: Bundle) {
        setWidgetVisibilityToVisible()
        loadVideoFromExtrasVideoId(extras)
        createNextVideosQueue(extras)
    }

    private fun setWidgetVisibilityToVisible() {
        val linearLayout: LinearLayout = floatingView.findViewById(R.id.linearLayout)
        linearLayout.visibility = View.VISIBLE
    }

    private fun loadVideoFromExtrasVideoId(extras: Bundle) {
        val videoId = extras.getString("videoId", "")
        if (youTubePlayer != null) {
            youTubePlayer?.loadVideo(videoId, 0f)
        } else {
            waitForPlayerToInitAndPlayVideo(videoId)
        }
    }

    private fun waitForPlayerToInitAndPlayVideo(videoId: String) {
        Thread {
            while (youTubePlayer == null);
            youTubePlayer?.loadVideo(videoId, 0f)
        }.start()
    }

    private fun createNextVideosQueue(extras: Bundle) {
        val videoId = extras.getString("videoId", "")
        val listId = extras.getString("listId", null)
        Thread {
            videosQueue = VideosQueue(videoId, listId, 9)
        }.start()
    }

    private fun registerScreenStatusReceiver() {
        val myScreenReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action.equals(Intent.ACTION_SCREEN_OFF)) {
                    youTubePlayer?.pause()
                } else if (intent?.action.equals(Intent.ACTION_USER_PRESENT)) {
                    youTubePlayer?.play()
                }
            }
        }
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        registerReceiver(myScreenReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        mWindowManager?.removeView(floatingView)
    }
}