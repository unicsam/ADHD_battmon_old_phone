package com.example.lowbat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

class BeepReceiver : BroadcastReceiver() {

    companion object {
        private const val BEEP_INTERVAL_10_PERCENT = 600000L // 10 minutes
        private const val BEEP_INTERVAL_5_PERCENT = 300000L // 5 minutes
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            val currentBatteryPct = getCurrentBatteryLevel(ctx)
            
            if (currentBatteryPct > 5) {
                playDoubleBeep(ctx)
                scheduleNextBeep(ctx, currentBatteryPct)
            } else {
                cancelBeepAlarm(ctx)
            }
        }
    }

    private fun cancelBeepAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, BeepReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun getCurrentBatteryLevel(context: Context): Int {
        return try {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra("level", -1) ?: -1
            val scale = batteryIntent?.getIntExtra("scale", -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100) / scale else 15
        } catch (e: Exception) {
            15
        }
    }

    private fun playDoubleBeep(context: Context) {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            Handler(Looper.getMainLooper()).postDelayed({
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            }, 300)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleNextBeep(context: Context, batteryPct: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, BeepReceiver::class.java).apply {
            putExtra("battery_level", batteryPct)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMs = when {
            batteryPct < 10 -> BEEP_INTERVAL_10_PERCENT
            else -> BEEP_INTERVAL_5_PERCENT
        }

        val triggerTime = System.currentTimeMillis() + intervalMs

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
