package com.example.lowbat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BatteryMonitorService : Service() {

    private val CHANNEL_ID = "battery_monitor_channel"
    private val NOTIFICATION_ID = 1

    private var batteryReceiver: BroadcastReceiver? = null
    private var lastShownLevel: Int? = null
    private var lastPopupTime: Long = 0
    private val POPUP_COOLDOWN = 5000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Monitoring battery..."))
        registerBatteryReceiver()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitors battery status"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Monitor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra("level", -1)
                    val scale = intent.getIntExtra("scale", -1)
                    val batteryPct = if (level >= 0 && scale > 0) {
                        (level * 100) / scale
                    } else {
                        -1
                    }

                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(NOTIFICATION_ID, createNotification("Battery: $batteryPct%"))

                    when {
                        batteryPct <= 10 && lastShownLevel != 10 -> {
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
        val intent = Intent(this, LowBatteryActivity::class.java).apply {
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
            } catch (e: Exception) {
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
