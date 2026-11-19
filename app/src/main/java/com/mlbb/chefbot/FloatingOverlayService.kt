package com.mlbb.chefbot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingOverlayService : Service() {

    companion object {
        private const val TAG = "MLBB_OVERLAY"
        private const val CHANNEL_ID = "mlbb_bot_channel"
        private const val NOTIFICATION_ID = 101
        const val ACTION_STOP = "com.mlbb.chefbot.ACTION_STOP_OVERLAY"

        private val _isActiveFlow = MutableStateFlow(false)
        val isActiveFlow: StateFlow<Boolean> = _isActiveFlow.asStateFlow()

        val isActive: Boolean
            get() = _isActiveFlow.value
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayAttached = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundCompat()

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted; stopping overlay service")
            stopSelf()
            return
        }

        _isActiveFlow.value = true
        initOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            BotAccessibilityService.instance?.stopCooking()
            stopSelf()
            return START_NOT_STICKY
        }

        if (isOverlayAttached && !BotAccessibilityService.isRunning) {
            BotAccessibilityService.instance?.startCooking()
        }

        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initOverlay() {
        val wm = getSystemService(WindowManager::class.java)
        if (wm == null) {
            stopSelf()
            return
        }
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Position near top edge so it stays clear of Cola/Ice Cream dispensers (y ~ 0.22h).
            x = 24
            y = 16
        }

        val view = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null)
        overlayView = view

        val tvStatus = view.findViewById<TextView>(R.id.tvBubbleStatus)
        val btnPause = view.findViewById<Button>(R.id.btnBubblePause)
        val btnClose = view.findViewById<Button>(R.id.btnBubbleClose)

        fun updateBubbleUi(running: Boolean) {
            if (running) {
                tvStatus.text = getString(R.string.bubble_status_active)
                btnPause.text = "⏸"
            } else {
                tvStatus.text = getString(R.string.bubble_status_paused)
                btnPause.text = "▶"
            }
        }

        btnPause.setOnClickListener {
            val service = BotAccessibilityService.instance
            if (service == null) {
                Toast.makeText(
                    this,
                    "Служба доступности не включена",
                    Toast.LENGTH_SHORT
                ).show()
                updateBubbleUi(false)
                return@setOnClickListener
            }
            if (BotAccessibilityService.isRunning) {
                service.stopCooking()
            } else {
                service.startCooking()
            }
        }

        btnClose.setOnClickListener {
            BotAccessibilityService.instance?.stopCooking()
            stopSelf()
        }

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var moved = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                            moved = true
                        }
                        val (maxW, maxH) = getOverlayMaxBounds(wm, view)
                        params.x = (initialX + dx.toInt()).coerceIn(0, maxW)
                        params.y = (initialY + dy.toInt()).coerceIn(0, maxH)
                        if (isOverlayAttached) {
                            try {
                                wm.updateViewLayout(view, params)
                            } catch (e: IllegalArgumentException) {
                                Log.w(TAG, "Overlay view not attached during drag", e)
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            v?.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            wm.addView(view, params)
            isOverlayAttached = true
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "Failed to attach overlay window", e)
            stopSelf()
            return
        } catch (e: SecurityException) {
            Log.e(TAG, "Overlay permission denied", e)
            stopSelf()
            return
        }

        serviceScope.launch {
            BotAccessibilityService.isRunningFlow.collect { running ->
                updateBubbleUi(running)
            }
        }

        // Automatically start the bot cooking routine if accessibility service is ready
        val service = BotAccessibilityService.instance
        if (service != null) {
            service.startCooking()
        } else {
            updateBubbleUi(false)
        }
    }

    private fun getOverlayMaxBounds(wm: WindowManager, view: View): Pair<Int, Int> {
        val screenW: Int
        val screenH: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            screenW = bounds.width()
            screenH = bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay?.getRealMetrics(metrics)
            screenW = metrics.widthPixels
            screenH = metrics.heightPixels
        }
        val maxX = (screenW - view.width).coerceAtLeast(0)
        val maxY = (screenH - view.height).coerceAtLeast(0)
        return maxX to maxY
    }

    override fun onDestroy() {
        _isActiveFlow.value = false
        serviceScope.cancel()
        BotAccessibilityService.instance?.stopCooking()
        if (isOverlayAttached && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Overlay already detached", e)
            }
        }
        isOverlayAttached = false
        overlayView = null
        super.onDestroy()
    }
}
