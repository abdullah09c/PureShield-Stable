package com.abdullah09c.pureshield.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object DnsHelper {

    /**
     * Opens the system Private DNS settings screen.
     * Android 9+ supports Private DNS (DNS over TLS).
     * The user selects "Private DNS provider hostname" and enters the address.
     */
    fun openPrivateDnsSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ has direct action
            try {
                val intent = Intent("android.settings.PVT_DNS_SETTINGS")
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9 — try wireless settings which contains DNS
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }

        // Fallback to general settings
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    fun isPrivateDnsSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun getAllPresets(): List<DnsPreset> = DnsPreset.values().toList()
}
