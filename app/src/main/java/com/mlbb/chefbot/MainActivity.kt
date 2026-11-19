package com.mlbb.chefbot

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mlbb.chefbot.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var uiScope: CoroutineScope? = null
    private var hasAskedNotificationPermission = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        startBotOverlayService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateButtonState(
            isCooking = BotAccessibilityService.isRunning,
            isOverlayActive = FloatingOverlayService.isActive
        )

        binding.btnToggleBot.setOnClickListener {
            handleToggleBot()
        }
    }

    override fun onStart() {
        super.onStart()
        uiScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        uiScope = scope
        scope.launch {
            combine(
                BotAccessibilityService.isRunningFlow,
                FloatingOverlayService.isActiveFlow
            ) { isCooking, isOverlayActive ->
                isCooking to isOverlayActive
            }.collect { (isCooking, isOverlayActive) ->
                updateButtonState(isCooking, isOverlayActive)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonState(
            isCooking = BotAccessibilityService.isRunning,
            isOverlayActive = FloatingOverlayService.isActive
        )
    }

    override fun onStop() {
        uiScope?.cancel()
        uiScope = null
        super.onStop()
    }

    private fun handleToggleBot() {
        // 1. Check Overlay Permission
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Пожалуйста, включите отображение поверх других окон",
                Toast.LENGTH_LONG
            ).show()
            openOverlaySettings()
            return
        }

        // 2. Check Accessibility Service Permission
        if (BotAccessibilityService.instance == null) {
            Toast.makeText(
                this,
                "Включите службу 'MLBB Ресторан Бот' в Спец. возможностях",
                Toast.LENGTH_LONG
            ).show()
            openAccessibilitySettings()
            return
        }

        // 3. Stop if running or overlay is active; otherwise request notifications (API 33+) and start
        if (BotAccessibilityService.isRunning || FloatingOverlayService.isActive) {
            BotAccessibilityService.instance?.stopCooking()
            stopService(Intent(this, FloatingOverlayService::class.java))
            Toast.makeText(this, "Бот остановлен", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasAskedNotificationPermission &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                hasAskedNotificationPermission = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            startBotOverlayService()
        }
    }

    private fun startBotOverlayService() {
        val serviceIntent = Intent(this, FloatingOverlayService::class.java)
        try {
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(
                this,
                "Бот запущен! Откройте Mobile Legends",
                Toast.LENGTH_SHORT
            ).show()
            // Minimize app to let user enter MLBB
            moveTaskToBack(true)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Не удалось запустить службу виджета: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openOverlaySettings() {
        val packageIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        try {
            startActivity(packageIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                    this,
                    "Откройте настройки разрешений вручную",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Откройте настройки Спец. возможностей вручную",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateButtonState(isCooking: Boolean, isOverlayActive: Boolean) {
        val active = isCooking || isOverlayActive
        if (active) {
            binding.btnToggleBot.text = getString(R.string.btn_stop_bot)
            binding.btnToggleBot.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_stop))
            binding.btnToggleBot.setTextColor(ContextCompat.getColor(this, R.color.text_white))
            binding.tvStatusTitle.text = getString(
                if (isCooking) R.string.status_running else R.string.status_paused
            )
            binding.tvStatusTitle.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isCooking) R.color.primary else R.color.accent
                )
            )
        } else {
            binding.btnToggleBot.text = getString(R.string.btn_start_bot)
            binding.btnToggleBot.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))
            binding.btnToggleBot.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
            binding.tvStatusTitle.text = getString(R.string.status_ready)
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.text_white))
        }
    }
}
