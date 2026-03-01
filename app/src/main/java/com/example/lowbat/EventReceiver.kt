package com.example.lowbat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class EventReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_BATTERY_LOW = "android.intent.action.BATTERY_LOW"
        const val ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED"
        const val ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action

        // If our service is not running, start it
        when (action) {
            ACTION_BATTERY_LOW,
            ACTION_POWER_CONNECTED,
            ACTION_SCREEN_ON,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_BATTERY_CHANGED -> {
                startServiceIfNeeded(context)
            }
        }
    }

    private fun startServiceIfNeeded(context: Context) {
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        
        // Check if service is already running by trying to stop it first
        // If it throws exception, service wasn't running
        try {
            context.stopService(serviceIntent)
        } catch (e: Exception) {
            // Service not running, start it
        }
        
        // Start the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
