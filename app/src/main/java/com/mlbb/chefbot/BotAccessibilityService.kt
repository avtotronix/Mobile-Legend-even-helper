package com.mlbb.chefbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

class BotAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MLBB_BOT"
        private const val TAP_DURATION_MS = 45L
        private const val GESTURE_TIMEOUT_MS = 1000L
        private const val IDLE_WAIT_MS = 300L

        @Volatile
        var instance: BotAccessibilityService? = null
            private set

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        val isRunning: Boolean
            get() = _isRunningFlow.value
    }

    private val stateLock = Any()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Bot coroutine error", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)

    private var cookingEngine: CookingEngine? = null
    private var botJob: Job? = null

    @Volatile
    private var currentWindowPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val (width, height) = getRealScreenDimensions()
        cookingEngine = CookingEngine(width, height)
        Log.d(TAG, "Accessibility Service Connected: ${width}x${height}")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val (width, height) = getRealScreenDimensions()
        cookingEngine?.updateScreenSize(width, height)
        Log.d(TAG, "Configuration changed: ${width}x${height}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank()) {
                currentWindowPackage = pkg
            }
        }
    }

    override fun onInterrupt() {
        stopCooking()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopCooking()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopCooking()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    fun startCooking() {
        synchronized(stateLock) {
            if (_isRunningFlow.value && botJob?.isActive == true) return

            val (width, height) = getRealScreenDimensions()
            val engine = cookingEngine?.also { it.updateScreenSize(width, height) }
                ?: CookingEngine(width, height).also { cookingEngine = it }

            botJob?.cancel()
            _isRunningFlow.value = true
            Log.d(TAG, "Bot loop started (${width}x${height})")

            lateinit var launchedJob: Job
            launchedJob = scope.launch {
                try {
                    runCookingLoop(engine)
                } finally {
                    synchronized(stateLock) {
                        if (botJob === launchedJob) {
                            _isRunningFlow.value = false
                            botJob = null
                        }
                    }
                }
            }
            botJob = launchedJob
        }
    }

    fun stopCooking() {
        synchronized(stateLock) {
            val wasRunning = _isRunningFlow.value || botJob != null
            _isRunningFlow.value = false
            botJob?.cancel()
            botJob = null
            if (wasRunning) {
                Log.d(TAG, "Bot loop stopped")
            }
        }
    }

    private suspend fun runCookingLoop(engine: CookingEngine) {
        while (coroutineContext.isActive && _isRunningFlow.value) {
            if (!isReadyForLandscapeTaps()) {
                delay(IDLE_WAIT_MS)
                continue
            }

            val cycle = engine.getFullKitchenCycle()
            for ((tapPath, delayMs) in cycle) {
                if (!coroutineContext.isActive || !_isRunningFlow.value) break
                if (!isReadyForLandscapeTaps()) break

                dispatchTap(tapPath)
                delay(delayMs)
            }
        }
    }

    private fun isReadyForLandscapeTaps(): Boolean {
        val (width, height) = getRealScreenDimensions()
        if (width <= height) {
            // Game runs in landscape; avoid dispatching out-of-bounds taps while in portrait.
            return false
        }
        if (currentWindowPackage == packageName) {
            // Never tap our own control activity.
            return false
        }
        cookingEngine?.updateScreenSize(width, height)
        return true
    }

    private fun getRealScreenDimensions(): Pair<Int, Int> {
        val wm = getSystemService(WindowManager::class.java)
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.maximumWindowMetrics.bounds
                if (bounds.width() > 0 && bounds.height() > 0) {
                    return bounds.width() to bounds.height()
                }
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay?.getRealMetrics(metrics)
                if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
                    return metrics.widthPixels to metrics.heightPixels
                }
            }
        }
        val fallback = resources.displayMetrics
        return fallback.widthPixels.coerceAtLeast(1) to fallback.heightPixels.coerceAtLeast(1)
    }

    private suspend fun dispatchTap(path: Path): Boolean {
        if (path.isEmpty) return false

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, TAP_DURATION_MS))
            .build()

        return withTimeoutOrNull(GESTURE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val resumed = AtomicBoolean(false)
                val callback = object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (resumed.compareAndSet(false, true) && continuation.isActive) {
                            continuation.resume(true)
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (resumed.compareAndSet(false, true) && continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }

                val dispatched = dispatchGesture(gesture, callback, null)
                if (!dispatched && resumed.compareAndSet(false, true) && continuation.isActive) {
                    continuation.resume(false)
                }
            }
        } ?: false
    }
}
