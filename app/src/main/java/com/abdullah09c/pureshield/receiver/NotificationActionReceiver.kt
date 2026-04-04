package com.abdullah09c.pureshield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.abdullah09c.pureshield.service.BlockerService
import com.abdullah09c.pureshield.util.Prefs

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != BlockerService.ACTION_STOP_FROM_NOTIFICATION) return

        // Always disable blocker state when user taps Stop from the notification.
        Prefs.setBlockerEnabled(context, false)

        // Remove notification immediately; service will also reconcile state on next event.
        NotificationManagerCompat.from(context).cancel(BlockerService.NOTIF_ID)

        // Ask service to sync foreground state if it accepts start commands on this device.
        val syncIntent = Intent(context, BlockerService::class.java).apply {
            action = BlockerService.ACTION_SYNC_NOTIFICATION
        }
        context.startService(syncIntent)
    }
}
