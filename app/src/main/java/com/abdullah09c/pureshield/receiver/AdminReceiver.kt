package com.abdullah09c.pureshield.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.abdullah09c.pureshield.util.Prefs

class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        // Device admin was enabled — protection is now active
        Prefs.setProtectionEnabled(context, true)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Device admin was disabled — protection is off
        Prefs.setProtectionEnabled(context, false)
        Prefs.setPin(context, "")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Enter your PureShield PIN to disable uninstall protection."
    }
}
