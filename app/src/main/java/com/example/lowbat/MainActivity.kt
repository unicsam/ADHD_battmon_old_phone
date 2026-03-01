package com.example.lowbat

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {

    companion object {
        const val NOTIFICATION_PERMISSION_CODE = 1001
        const val PREFS_NAME = "BatteryMonitorPrefs"
        const val KEY_PAUSED = "paused_until_charge"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var pauseButton: MaterialButton
    private lateinit var pauseStatus: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        pauseButton = findViewById(R.id.pauseButton)
        pauseStatus = findViewById(R.id.pauseStatus)

        updatePauseUI()

        pauseButton.setOnClickListener {
            togglePause()
        }

        checkPermissions()
        startBatteryService()
    }

    override fun onResume() {
        super.onResume()
        updatePauseUI()
    }

    private fun togglePause() {
        val isPaused = prefs.getBoolean(KEY_PAUSED, false)
        if (isPaused) {
            prefs.edit().putBoolean(KEY_PAUSED, false).apply()
        } else {
            prefs.edit().putBoolean(KEY_PAUSED, true).apply()
        }
        updatePauseUI()
    }

    private fun updatePauseUI() {
        val isPaused = prefs.getBoolean(KEY_PAUSED, false)
        if (isPaused) {
            pauseButton.text = "Resume Alerts"
            pauseStatus.text = "Paused until next charge"
            pauseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            pauseButton.text = "Pause Until Next Charge"
            pauseStatus.text = "Currently active"
            pauseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun startBatteryService() {
        val serviceIntent = Intent(this, BatteryMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
