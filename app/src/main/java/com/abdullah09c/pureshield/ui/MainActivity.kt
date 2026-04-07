package com.abdullah09c.pureshield.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.abdullah09c.pureshield.R
import com.abdullah09c.pureshield.databinding.ActivityMainBinding
import com.abdullah09c.pureshield.receiver.AdminReceiver
import com.abdullah09c.pureshield.service.BlockerService
import com.abdullah09c.pureshield.util.DnsHelper
import com.abdullah09c.pureshield.util.DnsPreset
import com.abdullah09c.pureshield.util.Prefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        setupClickListeners()
        requestBatteryOptimizationExemption()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        updateAllCardStates()
    }

    // ─── UI State ────────────────────────────────────────────────────────────

    private fun updateAllCardStates() {
        updateBlockerCard()
        updateDnsCard()
        updateProtectionCard()
        updateBootToggle()
    }

    private fun updateBlockerCard() {
        val isAccessibilityOn = isAccessibilityServiceEnabled()
        val isEnabled = Prefs.isBlockerEnabled(this) && isAccessibilityOn

        binding.switchBlocker.isChecked = isEnabled
        binding.tvBlockerStatus.text = if (isEnabled)
            getString(R.string.status_active) else getString(R.string.status_inactive)
        binding.tvBlockerStatus.setTextColor(
            if (isEnabled) getColor(R.color.active_green) else getColor(R.color.inactive_gray)
        )
    }

    private fun updateDnsCard() {
        val preset = try {
            DnsPreset.valueOf(Prefs.getDnsPreset(this))
        } catch (e: IllegalArgumentException) {
            Prefs.setDnsPreset(this, DnsPreset.NONE.name)
            DnsPreset.NONE
        }
        
        binding.tvDnsStatus.text = if (preset == DnsPreset.NONE)
            getString(R.string.status_inactive)
        else
            preset.displayName
        binding.tvDnsStatus.setTextColor(
            if (preset == DnsPreset.NONE) getColor(R.color.inactive_gray)
            else getColor(R.color.active_green)
        )
    }

    private fun updateProtectionCard() {
        val isAdmin = dpm.isAdminActive(adminComponent)
        binding.switchProtection.isChecked = isAdmin
        binding.tvProtectionStatus.text = if (isAdmin)
            getString(R.string.status_active) else getString(R.string.status_inactive)
        binding.tvProtectionStatus.setTextColor(
            if (isAdmin) getColor(R.color.active_green) else getColor(R.color.inactive_gray)
        )
    }

    private fun updateBootToggle() {
        binding.switchBoot.isChecked = Prefs.isStartOnBoot(this)
    }

    // ─── Click Listeners ─────────────────────────────────────────────────────

    private fun setupClickListeners() {

        // Blocker toggle
        binding.switchBlocker.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isAccessibilityServiceEnabled()) {
                    binding.switchBlocker.isChecked = false
                    showAccessibilityDialog()
                } else {
                    Prefs.setBlockerEnabled(this, true)
                    syncBlockerServiceNotification()
                    updateBlockerCard()
                }
            } else {
                Prefs.setBlockerEnabled(this, false)
                syncBlockerServiceNotification()
                updateBlockerCard()
            }
        }

        // Blocker settings button
        binding.btnBlockerSettings.setOnClickListener {
            startActivity(Intent(this, BlockerSettingsActivity::class.java))
        }

        // DNS card
        binding.cardDns.setOnClickListener {
            showDnsPickerDialog()
        }

        // Protection toggle
        binding.switchProtection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!dpm.isAdminActive(adminComponent)) {
                    // Launch device admin enrollment
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "PureShield uses Device Admin to prevent impulsive uninstallation."
                        )
                    }
                    startActivity(intent)
                    binding.switchProtection.isChecked = false
                } else {
                    // Already admin, just show as active
                }
            } else {
                if (dpm.isAdminActive(adminComponent)) {
                    // Require PIN to disable
                    startActivity(Intent(this, PinActivity::class.java).apply {
                        putExtra(PinActivity.MODE_KEY, PinActivity.MODE_VERIFY)
                    })
                    binding.switchProtection.isChecked = true // revert until PIN verified
                }
            }
        }

        // Boot toggle
        binding.switchBoot.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setStartOnBoot(this, isChecked)
        }

        // About
        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // Feedback
        binding.btnFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
    }

    // ─── DNS Picker Dialog ───────────────────────────────────────────────────

    private fun showDnsPickerDialog() {
        if (!DnsHelper.isPrivateDnsSupported()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_dns))
                .setMessage(getString(R.string.dns_android9_warning))
                .setPositiveButton(getString(R.string.ok), null)
                .show()
            return
        }

        val presets = DnsHelper.getAllPresets()
        val names = presets.map { it.displayName }.toTypedArray()
        val currentPreset = Prefs.getDnsPreset(this)
        val currentIndex = presets.indexOfFirst { it.name == currentPreset }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dns_title))
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selected = presets[which]
                Prefs.setDnsPreset(this, selected.name)
                updateDnsCard()
                dialog.dismiss()
                if (selected != DnsPreset.NONE) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_dns_info, null)
                    
                    val tvDnsTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvDnsTitle)
                    val tvDnsAddress = dialogView.findViewById<android.widget.TextView>(R.id.tvDnsAddress)
                    val dnsFeaturesContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.dnsFeaturesContainer)
                    val btnCopy = dialogView.findViewById<android.widget.Button>(R.id.btnCopy)
                    val btnOpenSettings = dialogView.findViewById<android.widget.Button>(R.id.btnOpenSettings)

                    tvDnsTitle.text = selected.displayName
                    tvDnsAddress.text = selected.address

                    selected.features.forEach { feature ->
                        val featureView = layoutInflater.inflate(R.layout.item_dns_feature, dnsFeaturesContainer, false)
                        val tvFeatureIcon = featureView.findViewById<android.widget.TextView>(R.id.tvFeatureIcon)
                        val tvFeatureText = featureView.findViewById<android.widget.TextView>(R.id.tvFeatureText)
                        
                        if (feature.startsWith("Allows")) {
                            tvFeatureIcon.text = "❌"
                            tvFeatureIcon.setTextColor(getColor(R.color.danger))
                        } else if (feature.startsWith("Blocks")) {
                            tvFeatureIcon.text = "🛡️"
                        } else if (feature.startsWith("High Speed")) {
                            tvFeatureIcon.text = "⚡"
                        } else if (feature.startsWith("Updated")) {
                            tvFeatureIcon.text = "🔄"
                        } else if (feature.startsWith("Forces")) {
                            tvFeatureIcon.text = "🔒"
                        }
                        
                        tvFeatureText.text = feature
                        dnsFeaturesContainer.addView(featureView)
                    }

                    val infoDialog = MaterialAlertDialogBuilder(this)
                        .setView(dialogView)
                        .show()

                    btnCopy.setOnClickListener {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DNS", selected.address))
                        Toast.makeText(this, "Address copied!", Toast.LENGTH_SHORT).show()
                    }

                    btnOpenSettings.setOnClickListener {
                        infoDialog.dismiss()
                        DnsHelper.openPrivateDnsSettings(this)
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ─── Accessibility Helpers ───────────────────────────────────────────────

    private fun syncBlockerServiceNotification() {
        val intent = Intent(this, BlockerService::class.java).apply {
            action = BlockerService.ACTION_SYNC_NOTIFICATION
        }
        startService(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains("${packageName}/${BlockerService::class.java.name}")
    }

    private fun showAccessibilityDialog() {
        val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "To enable PureShield:\n\n" +
            "1. Tap 'Open Settings'\n" +
            "2. Find 'PureShield'\n" +
            "3. Tap it and enable the service\n\n" +
            "⚠️ On Android 13+: If you see 'Restricted Setting', tap the 3-dot menu → Allow Restricted Settings first."
        } else {
            "Tap 'Open Settings', find PureShield and enable the accessibility service."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage(msg)
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ─── System Permission Helpers ───────────────────────────────────────────

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.battery_opt_title))
                    .setMessage(getString(R.string.battery_opt_msg))
                    .setPositiveButton("Disable Optimization") { _, _ ->
                        try {
                            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        } catch (_: Exception) {
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                    .setNegativeButton("Later", null)
                    .show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }
}
