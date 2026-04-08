package com.abdullah09c.pureshield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.abdullah09c.pureshield.util.Prefs

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Only proceed if start-on-boot is enabled by user
            if (!Prefs.isStartOnBoot(context)) return

            // Check if accessibility service is actually enabled
            // The service starts itself when granted permission;
            // we just need to ensure the setting is still on.
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return

            val packageName = context.packageName
            if (enabledServices.contains(packageName)) {
                // Accessibility service is enabled - Android will restart it automatically.
                // We just update the preference so UI shows correct state.
                Prefs.setBlockerEnabled(context, true)
            }
        }
    }
}
