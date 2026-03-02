package com.example.lowbat

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class BatteryMonitorService : Service() {

    private val CHANNEL_ID = "battery_monitor_channel"
    private val NOTIFICATION_ID = 1

    private var batteryReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var lastShownLevel: Int? = null
    private var lastPopupTime: Long = 0
    private val POPUP_COOLDOWN = 5000L
    private val BEEP_INTERVAL_MS = 60000L // 1 minute

    private lateinit var prefs: SharedPreferences
    private var isScreenOn = true
    private var beepHandler: Handler? = null
    private var beepRunnable: Runnable? = null
    private var toneGenerator: ToneGenerator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(100))
        registerBatteryReceiver()
        registerScreenReceiver()
        
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerScreenReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        stopBeeping()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        startBeepingIfNeeded()
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        isScreenOn = true
                        stopBeeping()
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun isScreenLocked(): Boolean {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isKeyguardLocked
    }

    private fun startBeepingIfNeeded() {
        if (beepHandler != null) return
        
        beepHandler = Handler(Looper.getMainLooper())
        beepRunnable = object : Runnable {
            override fun run() {
                playDoubleBeep()
                beepHandler?.postDelayed(this, BEEP_INTERVAL_MS)
            }
        }
        beepHandler?.post(beepRunnable!!)
    }

    private fun stopBeeping() {
        beepRunnable?.let { beepHandler?.removeCallbacks(it) }
        beepRunnable = null
        beepHandler = null
    }

    private fun playDoubleBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            Handler(Looper.getMainLooper()).postDelayed({
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            }, 300)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    "Battery Monitor",
                                    NotificationManager.IMPORTANCE_MIN
                            )
                            .apply {
                                description = "Monitors battery status"
                                setShowBadge(false)
                                enableVibration(false)
                                setSound(null, null)
                            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(batteryPct: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) CHANNEL_ID else ""

        return NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Battery $batteryPct%")
                .setProgress(100, batteryPct, false)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build()
    }

    private fun registerBatteryReceiver() {
        batteryReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                            val level = intent.getIntExtra("level", -1)
                            val scale = intent.getIntExtra("scale", -1)
                            val status =
                                    intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                            val isCharging =
                                    status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                                            status == android.os.BatteryManager.BATTERY_STATUS_FULL

                            val batteryPct =
                                    if (level >= 0 && scale > 0) {
                                        (level * 100) / scale
                                    } else {
                                        -1
                                    }

                            // If charging, close any existing popup, resume alerts, and update notification
                            if (isCharging) {
                                LowBatteryActivity.currentInstance?.finishAndRemoveTask()
                                lastShownLevel = null
                                prefs.edit().putBoolean(MainActivity.KEY_PAUSED, false).apply()
                                val notificationManager =
                                        getSystemService(NotificationManager::class.java)
                                notificationManager.notify(
                                        NOTIFICATION_ID,
                                        createNotification(batteryPct)
                                )
                                return
                            }

                            // Check if alerts are paused
                            if (prefs.getBoolean(MainActivity.KEY_PAUSED, false)) {
                                return
                            }

                            val notificationManager =
                                    getSystemService(NotificationManager::class.java)
                            notificationManager.notify(
                                    NOTIFICATION_ID,
                                    createNotification(batteryPct)
                            )

                            when {
                                batteryPct <= 2 && lastShownLevel != 2 -> {
                                    if (System.currentTimeMillis() - lastPopupTime > POPUP_COOLDOWN) {
                                        lastShownLevel = 2
                                        lastPopupTime = System.currentTimeMillis()
                                        showLowBatteryPopup(batteryPct)
                                    }
                                }
                                batteryPct <= 4 && batteryPct > 2 && lastShownLevel != 4 -> {
                                    if (System.currentTimeMillis() - lastPopupTime > POPUP_COOLDOWN) {
                                        lastShownLevel = 4
                                        lastPopupTime = System.currentTimeMillis()
                                        showLowBatteryPopup(batteryPct)
                                    }
                                }
                                batteryPct <= 6 && batteryPct > 4 && lastShownLevel != 6 -> {
                                    if (System.currentTimeMillis() - lastPopupTime > POPUP_COOLDOWN) {
                                        lastShownLevel = 6
                                        lastPopupTime = System.currentTimeMillis()
                                        showLowBatteryPopup(batteryPct)
                                    }
                                }
                                batteryPct <= 8 && batteryPct > 6 && lastShownLevel != 8 -> {
                                    if (System.currentTimeMillis() - lastPopupTime > POPUP_COOLDOWN) {
                                        lastShownLevel = 8
                                        lastPopupTime = System.currentTimeMillis()
                                        showLowBatteryPopup(batteryPct)
                                    }
                                }
                                batteryPct <= 10 && batteryPct > 8 && lastShownLevel != 10 -> {
                                    if (System.currentTimeMillis() - lastPopupTime > POPUP_COOLDOWN) {
                                        lastShownLevel = 10
                                        lastPopupTime = System.currentTimeMillis()
                                        showLowBatteryPopup(batteryPct)
                                    }
                                }
                                batteryPct <= 15 && batteryPct > 10 && lastShownLevel != 15 -> {
                                    if (System.currentTimeMillis() - lastPopupTime > POPUP_COOLDOWN) {
                                        lastShownLevel = 15
                                        lastPopupTime = System.currentTimeMillis()
                                        showLowBatteryPopup(batteryPct)
                                    }
                                }
                                batteryPct <= 20 && batteryPct > 15 && lastShownLevel != 20 -> {
                                    if (System.currentTimeMillis() - lastPopupTime > POPUP_COOLDOWN) {
                                        lastShownLevel = 20
                                        lastPopupTime = System.currentTimeMillis()
                                        showLowBatteryPopup(batteryPct)
                                    }
                                }
                                batteryPct > 20 -> {
                                    lastShownLevel = null
                                }
                            }
                        }
                    }
                }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(batteryReceiver, filter)
        }
    }

    private fun showLowBatteryPopup(batteryPct: Int) {
        if (LowBatteryActivity.currentInstance != null) {
            return
        }

        if (isScreenLocked() || !isScreenOn) {
            startBeepingIfNeeded()
            return
        }

        stopBeeping()
        val intent =
                Intent(this, LowBatteryActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("battery_level", batteryPct)
                }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
        stopBeeping()
        toneGenerator?.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(this, BatteryMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
        super.onTaskRemoved(rootIntent)
    }
}
