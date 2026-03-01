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

        // Make the window full screen to show the background color
        window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        )
        window.setDimAmount(0.0f) // No dimming, let the background color shine through

        val batteryLevel = intent.getIntExtra("battery_level", 0)

        // Exact colors from Stitch designs (Tailwind bg-red-600 and bg-yellow-400)
        val bgColor =
                when {
                    batteryLevel <= 10 -> Color.parseColor("#dc2626")
                    batteryLevel <= 15 -> Color.parseColor("#facc15")
                    else -> Color.BLACK
                }

        window.setBackgroundDrawable(ColorDrawable(bgColor))

        // Update body text according to battery level (if you want it dynamic,
        // otherwise just use the Stitch text)
        // Stitch says "Your battery is low."
        binding.batteryLevelText.text = "Your battery is at $batteryLevel%"

        binding.okButton.setOnClickListener { finishAndRemoveTask() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentInstance == this) {
            currentInstance = null
        }
    }
}
