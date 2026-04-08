package com.abdullah09c.pureshield.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.abdullah09c.pureshield.databinding.ActivityPinBinding
import com.abdullah09c.pureshield.receiver.AdminReceiver
import com.abdullah09c.pureshield.util.Prefs

class PinActivity : AppCompatActivity() {

    companion object {
        const val MODE_KEY = "pin_mode"
        const val MODE_SETUP = "setup"
        const val MODE_VERIFY = "verify"
    }

    private lateinit var binding: ActivityPinBinding
    private var mode = MODE_SETUP
    private var firstPin = ""
    private var awaitingConfirm = false

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        mode = intent.getStringExtra(MODE_KEY) ?: MODE_SETUP
        updateUI()
        setupNumpad()
    }

    private fun updateUI() {
        when {
            mode == MODE_VERIFY -> {
                binding.tvPinTitle.text = getString(com.abdullah09c.pureshield.R.string.pin_enter_title)
                binding.tvPinSubtitle.text = "Enter your 4-digit PIN to disable protection"
                supportActionBar?.title = "Verify PIN"
            }
            awaitingConfirm -> {
                binding.tvPinTitle.text = getString(com.abdullah09c.pureshield.R.string.pin_confirm)
                binding.tvPinSubtitle.text = "Re-enter your PIN to confirm"
                supportActionBar?.title = getString(com.abdullah09c.pureshield.R.string.title_pin)
            }
            else -> {
                binding.tvPinTitle.text = getString(com.abdullah09c.pureshield.R.string.pin_setup_title)
                binding.tvPinSubtitle.text = "Choose a 4-digit PIN"
                supportActionBar?.title = getString(com.abdullah09c.pureshield.R.string.title_pin)
            }
        }
        binding.tvPinDots.text = "● ● ● ●".take(0)
        binding.etPin.setText("")
    }

    private fun setupNumpad() {
        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )
        val digits = listOf("0","1","2","3","4","5","6","7","8","9")

        buttons.forEachIndexed { i, btn ->
            btn.setOnClickListener { appendDigit(digits[i]) }
        }

        binding.btnDelete.setOnClickListener {
            val current = binding.etPin.text.toString()
            if (current.isNotEmpty()) {
                binding.etPin.setText(current.dropLast(1))
                updateDots(current.length - 1)
            }
        }

        binding.btnConfirm.setOnClickListener { handleConfirm() }
    }

    private fun appendDigit(digit: String) {
        val current = binding.etPin.text.toString()
        if (current.length >= 4) return
        val newPin = current + digit
        binding.etPin.setText(newPin)
        updateDots(newPin.length)
        if (newPin.length == 4) handleConfirm()
    }

    private fun updateDots(count: Int) {
        val filled = "●"
        val empty = "○"
        binding.tvPinDots.text = (filled.repeat(count) + empty.repeat(4 - count))
            .chunked(1).joinToString(" ")
    }

    private fun handleConfirm() {
        val entered = binding.etPin.text.toString()
        if (entered.length < 4) {
            Toast.makeText(this, "Enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
            return
        }

        when (mode) {
            MODE_VERIFY -> {
                val savedPin = Prefs.getPin(this)
                if (entered == savedPin) {
                    // Revoke device admin — now user can uninstall
                    dpm.removeActiveAdmin(adminComponent)
                    Prefs.setProtectionEnabled(this, false)
                    Prefs.setPin(this, "")
                    Toast.makeText(this, "Protection disabled", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.etPin.setText("")
                    updateDots(0)
                    binding.tvPinSubtitle.text = getString(com.abdullah09c.pureshield.R.string.pin_wrong)
                    binding.tvPinSubtitle.setTextColor(getColor(com.abdullah09c.pureshield.R.color.danger))
                }
            }
            MODE_SETUP -> {
                if (!awaitingConfirm) {
                    firstPin = entered
                    awaitingConfirm = true
                    updateUI()
                } else {
                    if (entered == firstPin) {
                        Prefs.setPin(this, entered)
                        Prefs.setProtectionEnabled(this, true)
                        Toast.makeText(this, getString(com.abdullah09c.pureshield.R.string.pin_set_success), Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        awaitingConfirm = false
                        firstPin = ""
                        updateUI()
                        binding.tvPinSubtitle.text = "PINs didn't match. Try again."
                        binding.tvPinSubtitle.setTextColor(getColor(com.abdullah09c.pureshield.R.color.danger))
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
