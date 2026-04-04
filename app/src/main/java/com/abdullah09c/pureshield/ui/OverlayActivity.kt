package com.abdullah09c.pureshield.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.abdullah09c.pureshield.databinding.ActivityOverlayBinding
import com.abdullah09c.pureshield.util.Prefs

class OverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make it appear over lock screen and other apps
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show user's custom block message
        val message = Prefs.getBlockMessage(this)
        binding.tvBlockMessage.text = message

        // Go back button - close overlay and return to the current app flow
        binding.btnGoBack.setOnClickListener {
            finish()
        }

        // Also allow tapping anywhere on the screen to dismiss (optional, feels natural)
        binding.root.setOnClickListener { /* consume to prevent accidental dismiss */ }

        // Prevent back button from just closing overlay (user must use Go Back button)
        onBackPressedDispatcher.addCallback(this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.btnGoBack.performClick()
                }
            }
        )
    }
}
