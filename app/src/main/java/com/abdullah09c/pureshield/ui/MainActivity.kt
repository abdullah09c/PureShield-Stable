package com.abdullah09c.pureshield.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.abdullah09c.pureshield.R
import com.abdullah09c.pureshield.databinding.ActivityMainBinding
import com.abdullah09c.pureshield.service.BlockerService
import com.abdullah09c.pureshield.util.DnsHelper
import com.abdullah09c.pureshield.util.DnsPreset
import com.abdullah09c.pureshield.util.Prefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REVIEW_PROMPT_TRIGGER_MS = 120_000L
        private const val REVIEW_PROMPT_LATER_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000
    }

    private lateinit var binding: ActivityMainBinding
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sessionResumeAtMs: Long? = null
    private var reviewPromptShownThisResume = false

    private val reviewPromptRunnable = Runnable {
        maybeShowReviewPrompt(forceThresholdCheck = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()

        if (!Prefs.isBlockingBehaviorChosen(this)) {
            showBlockingBehaviorDialog {
                showBatteryOptimizationInfo()
                requestNotificationPermission()
            }
        } else {
            showBatteryOptimizationInfo()
            requestNotificationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        updateAllCardStates()
        sessionResumeAtMs = SystemClock.elapsedRealtime()
        reviewPromptShownThisResume = false
        scheduleReviewPromptCheck()
    }

    override fun onPause() {
        super.onPause()
        persistSessionUsageTime()
        mainHandler.removeCallbacks(reviewPromptRunnable)
    }

    // ─── UI State ────────────────────────────────────────────────────────────

    private fun updateAllCardStates() {
        updateBlockerCard()
        updateDnsCard()
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
            preset.displayName(this)
        binding.tvDnsStatus.setTextColor(
            if (preset == DnsPreset.NONE) getColor(R.color.inactive_gray)
            else getColor(R.color.active_green)
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
                } else if (!Prefs.hasAccessibilityConsent(this)) {
                    showAccessibilityDisclosureDialog(
                        onAccept = {
                            Prefs.setAccessibilityConsentAccepted(this, true)
                            Prefs.setBlockerEnabled(this, true)
                            syncBlockerServiceNotification()
                            updateBlockerCard()
                        },
                        onCancel = {
                            binding.switchBlocker.isChecked = false
                        }
                    )
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
        val names = presets.map { it.displayName(this) }.toTypedArray()
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

                    tvDnsTitle.text = selected.displayName(this)
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
                        .create()

                    infoDialog.setOnShowListener {
                        applyResponsiveDialogSize(infoDialog)
                    }
                    infoDialog.show()

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

    private fun applyResponsiveDialogSize(dialog: AlertDialog) {
        val displayMetrics = resources.displayMetrics
        val maxWidthPx = (560 * displayMetrics.density).toInt()
        val targetWidthPx = min((displayMetrics.widthPixels * 0.94f).toInt(), maxWidthPx)
        dialog.window?.setLayout(targetWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

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
        val dialogView = layoutInflater.inflate(R.layout.dialog_accessibility_guide, null)
        
        val containerA13Warning = dialogView.findViewById<android.view.View>(R.id.containerA13Warning)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)
        val btnAppInfo = dialogView.findViewById<android.widget.Button>(R.id.btnAppInfo)
        val btnOpenSettings = dialogView.findViewById<android.widget.Button>(R.id.btnOpenSettings)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            containerA13Warning.visibility = android.view.View.VISIBLE
            btnAppInfo.visibility = android.view.View.VISIBLE
        }

        val infoDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .show()

        btnCancel.setOnClickListener {
            infoDialog.dismiss()
        }

        btnAppInfo.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            infoDialog.dismiss()
        }

        btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            infoDialog.dismiss()
        }
    }

    // ─── System Permission Helpers ───────────────────────────────────────────

    private fun showBatteryOptimizationInfo() {
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.battery_opt_title))
                .setMessage(getString(R.string.battery_opt_msg))
                .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.later), null)
                .show()
        }
    }

    private fun showAccessibilityDisclosureDialog(onAccept: () -> Unit, onCancel: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.accessibility_disclosure_title))
            .setMessage(getString(R.string.accessibility_disclosure_msg))
            .setPositiveButton(getString(R.string.continue_label)) { _, _ -> onAccept() }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    private fun scheduleReviewPromptCheck() {
        mainHandler.removeCallbacks(reviewPromptRunnable)
        if (Prefs.isReviewPromptCompleted(this)) return

        val accumulated = Prefs.getReviewPromptAccumulatedMs(this)
        if (accumulated >= REVIEW_PROMPT_TRIGGER_MS) {
            maybeShowReviewPrompt(forceThresholdCheck = false)
            return
        }

        val remaining = REVIEW_PROMPT_TRIGGER_MS - accumulated
        mainHandler.postDelayed(reviewPromptRunnable, remaining)
    }

    private fun persistSessionUsageTime() {
        val startedAt = sessionResumeAtMs ?: return
        val elapsed = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
        val updated = Prefs.getReviewPromptAccumulatedMs(this) + elapsed
        Prefs.setReviewPromptAccumulatedMs(this, updated)
        sessionResumeAtMs = null
    }

    private fun maybeShowReviewPrompt(forceThresholdCheck: Boolean) {
        if (isFinishing || isDestroyed || reviewPromptShownThisResume) return
        if (Prefs.isReviewPromptCompleted(this)) return

        persistSessionUsageTime()

        val accumulated = Prefs.getReviewPromptAccumulatedMs(this)
        if (forceThresholdCheck && accumulated < REVIEW_PROMPT_TRIGGER_MS) {
            sessionResumeAtMs = SystemClock.elapsedRealtime()
            scheduleReviewPromptCheck()
            return
        }

        val now = System.currentTimeMillis()
        val lastLaterAt = Prefs.getReviewPromptLastLaterAtMs(this)
        if (now - lastLaterAt < REVIEW_PROMPT_LATER_COOLDOWN_MS) {
            sessionResumeAtMs = SystemClock.elapsedRealtime()
            return
        }

        reviewPromptShownThisResume = true
        sessionResumeAtMs = SystemClock.elapsedRealtime()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.review_prompt_title))
            .setMessage(getString(R.string.review_prompt_message))
            .setPositiveButton(getString(R.string.review_now)) { _, _ ->
                Prefs.setReviewPromptCompleted(this, true)
                openPlayStoreForReview()
            }
            .setNegativeButton(getString(R.string.later)) { _, _ ->
                Prefs.setReviewPromptLastLaterAtMs(this, System.currentTimeMillis())
            }
            .setOnCancelListener {
                Prefs.setReviewPromptLastLaterAtMs(this, System.currentTimeMillis())
            }
            .show()
    }

    private fun openPlayStoreForReview() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun showBlockingBehaviorDialog(onCompleted: (() -> Unit)? = null) {
        if (isFinishing || isDestroyed) return
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_blocking_behavior, null)
        val cardScroll = dialogView.findViewById<android.view.View>(R.id.cardScroll)
        val cardInstant = dialogView.findViewById<android.view.View>(R.id.cardInstant)
        val tvScrollSelect = dialogView.findViewById<android.widget.TextView>(R.id.tvScrollSelect)
        val tvInstantSelect = dialogView.findViewById<android.widget.TextView>(R.id.tvInstantSelect)
        val btnContinue = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinueBehavior)

        val dialog = android.app.Dialog(this, R.style.Theme_PureShield)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)

        val window = dialog.window
        if (window != null) {
            window.setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT
            )
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(getColor(R.color.background)))
        }

        val scrollCard = cardScroll as MaterialCardView
        val instantCard = cardInstant as MaterialCardView
        var selectedBlockOnScroll = true

        val updateSelectionUi = {
            val selectedBg = ContextCompat.getColor(this, R.color.surface_variant)
            val normalBg = ContextCompat.getColor(this, R.color.surface)
            val selectedStroke = ContextCompat.getColor(this, R.color.green)
            val normalStroke = ContextCompat.getColor(this, R.color.card_stroke)
            val selectedText = ContextCompat.getColor(this, R.color.green_dark)
            val normalText = ContextCompat.getColor(this, R.color.text_hint)

            if (selectedBlockOnScroll) {
                scrollCard.setCardBackgroundColor(selectedBg)
                scrollCard.strokeColor = selectedStroke
                scrollCard.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                tvScrollSelect.text = getString(R.string.selected)
                tvScrollSelect.setTextColor(selectedText)

                instantCard.setCardBackgroundColor(normalBg)
                instantCard.strokeColor = normalStroke
                instantCard.strokeWidth = resources.displayMetrics.density.toInt()
                tvInstantSelect.text = getString(R.string.not_selected)
                tvInstantSelect.setTextColor(normalText)
            } else {
                instantCard.setCardBackgroundColor(selectedBg)
                instantCard.strokeColor = selectedStroke
                instantCard.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                tvInstantSelect.text = getString(R.string.selected)
                tvInstantSelect.setTextColor(selectedText)

                scrollCard.setCardBackgroundColor(normalBg)
                scrollCard.strokeColor = normalStroke
                scrollCard.strokeWidth = resources.displayMetrics.density.toInt()
                tvScrollSelect.text = getString(R.string.not_selected)
                tvScrollSelect.setTextColor(normalText)
            }
        }

        updateSelectionUi()

        cardScroll.setOnClickListener {
            selectedBlockOnScroll = true
            updateSelectionUi()
        }

        cardInstant.setOnClickListener {
            selectedBlockOnScroll = false
            updateSelectionUi()
        }

        btnContinue.setOnClickListener {
            Prefs.setBlockOnScroll(this, selectedBlockOnScroll)
            Prefs.setBlockingBehaviorChosen(this, true)
            dialog.dismiss()
            Toast.makeText(this, getString(R.string.blocking_behavior_saved), Toast.LENGTH_SHORT).show()
            onCompleted?.invoke()
        }

        dialog.show()
    }
}
