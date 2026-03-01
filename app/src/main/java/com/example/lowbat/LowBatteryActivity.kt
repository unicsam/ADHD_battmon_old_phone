package com.example.lowbat

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.lowbat.databinding.ActivityAlertBinding

class LowBatteryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding

    companion object {
        var currentInstance: LowBatteryActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        currentInstance?.finish()
        currentInstance = this

        super.onCreate(savedInstanceState)
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val batteryLevel = intent.getIntExtra("battery_level", 0)

        // Set background color based on battery level
        val backgroundColor = when {
            batteryLevel <= 10 -> Color.RED        // Red < 10%
            batteryLevel <= 15 -> Color.YELLOW     // Yellow < 15%
            batteryLevel <= 20 -> Color.BLACK      // Black < 20%
            else -> Color.TRANSPARENT
        }

        // Apply background color to the activity's window
        window.setBackgroundDrawable(ColorDrawable(backgroundColor))
        
        // Ensure background fills the entire screen
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // Update UI with battery level
        binding.batteryLevelText.text = "Your battery is at $batteryLevel%"
        
        // Adjust UI elements based on color
        if (backgroundColor == Color.YELLOW) {
            binding.alertTitle.setTextColor(Color.BLACK)
            binding.batteryLevelText.setTextColor(Color.DKGRAY)
            binding.okButton.setBackgroundColor(Color.parseColor("#806000")) // Darker yellow/brown for button
        } else if (backgroundColor == Color.BLACK) {
            binding.okButton.setBackgroundColor(Color.DKGRAY)
        }

        binding.okButton.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentInstance == this) {
            currentInstance = null
        }
    }
}
