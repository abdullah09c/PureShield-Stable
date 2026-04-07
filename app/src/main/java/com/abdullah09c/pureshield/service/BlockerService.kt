package com.abdullah09c.pureshield.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.abdullah09c.pureshield.R
import com.abdullah09c.pureshield.receiver.NotificationActionReceiver
import com.abdullah09c.pureshield.ui.MainActivity
import com.abdullah09c.pureshield.ui.OverlayActivity
import com.abdullah09c.pureshield.util.BlockTargets
import com.abdullah09c.pureshield.util.Prefs

@android.annotation.SuppressLint("AccessibilityServiceApi", "AccessibilityPolicy")
class BlockerService : AccessibilityService() {

    companion object {
        const val CHANNEL_ID = "pureshield_service"
        const val NOTIF_ID = 1001
        const val ACTION_STOP_FROM_NOTIFICATION = "com.abdullah09c.pureshield.action.STOP_FROM_NOTIFICATION"
        const val ACTION_SYNC_NOTIFICATION = "com.abdullah09c.pureshield.action.SYNC_NOTIFICATION"
        const val ACTION_DOUBLE_BACK = "com.abdullah09c.pureshield.action.DOUBLE_BACK"
        private const val TAG = "BlockerService"

        private const val BLOCK_ACTION_COOLDOWN_MS = 900L
        private const val FBLITE_CONFIRM_DELAY_MS = 500L  // Wait before confirming Reels block
        private const val FB_CONFIRM_DELAY_MS = 400L      // Wait before confirming FB Reels block
        private const val MAX_NODE_SCAN = 1800
        private const val TOAST_SHORT_FEED_BLOCKED = "Reels/Shorts Blocked"
        private const val TOAST_TIKTOK_BLOCKED = "TikTok Blocked"

        private const val TARGET_EVENT_MASK =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED

        var isRunning = false
    }

    private var lastBlockedPkg = ""
    private var lastBlockTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isForegroundShown = false

    // Pending confirmation runnable for FB Lite Reels detection.
    // We delay the block and re-check to avoid false positives when fast-scrolling
    // the news feed causes inline videos to briefly look like Reels.
    private var fblitePendingBlockRunnable: Runnable? = null
    // Pending confirmation runnable for FB (main app) Reels detection.
    private var fbPendingBlockRunnable: Runnable? = null

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        if (Prefs.isBlockerEnabled(this)) {
            startForegroundNotification()
        }
    }

    override fun onDestroy() {
        stopForegroundCompat()
        super.onDestroy()
        isRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_FROM_NOTIFICATION -> {
                // User explicitly stopped blocker from notification action.
                Prefs.setBlockerEnabled(this, false)
                stopForegroundCompat()
                return START_STICKY
            }

            ACTION_SYNC_NOTIFICATION -> {
                if (Prefs.isBlockerEnabled(this)) {
                    startForegroundNotification()
                } else {
                    stopForegroundCompat()
                }
                return START_STICKY
            }

            ACTION_DOUBLE_BACK -> {
                performDoubleBackAction()
                return START_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onInterrupt() {
        // Required override - no action needed
    }

    // ─── Core Event Handler ─────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if ((event.eventType and TARGET_EVENT_MASK) == 0) return

        // Only act when blocker is enabled
        if (!Prefs.isBlockerEnabled(this)) {
            if (isForegroundShown) stopForegroundCompat()
            return
        }

        if (!isForegroundShown) {
            startForegroundNotification()
        }

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return

        // Cancel pending delayed blocks if the user navigated away
        if (pkg != BlockTargets.PKG_FBLITE) {
            cancelPendingFBLiteBlock()
        }
        if (pkg != BlockTargets.PKG_FACEBOOK) {
            cancelPendingFBBlock()
        }

        // Only process events from our target apps
        if (pkg !in BlockTargets.ALL_PACKAGES) return

        // Debounce to avoid flickering
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockTime < BLOCK_ACTION_COOLDOWN_MS) return

        val isTikTokFamily = pkg == BlockTargets.PKG_TIKTOK ||
            pkg == BlockTargets.PKG_TIKTOK_ALT ||
            pkg == BlockTargets.PKG_TIKTOK_AWEME ||
            pkg == BlockTargets.PKG_TIKTOK_LITE ||
            pkg == BlockTargets.PKG_TIKTOK_LITE_LEGACY ||
            pkg == BlockTargets.PKG_TIKTOK_LITE_ALT

        if (isTikTokFamily && Prefs.isTikTokBlocked(this)) {
            lastBlockedPkg = pkg
            lastBlockTime = now
            blockFullAppWithGlobalAction(toastMessage = TOAST_TIKTOK_BLOCKED)
            return
        }

        if (pkg == BlockTargets.PKG_INSTAGRAM && Prefs.isInstagramBlocked(this)) {
            lastBlockedPkg = pkg
            lastBlockTime = now
            blockFullAppWithGlobalAction(toastMessage = "Instagram Blocked")
            return
        }

        // FB Lite gets special treatment: schedule a delayed confirmation check
        // to avoid false positives when fast-scrolling the news feed causes inline
        // videos to momentarily look like Reels.
        if (pkg == BlockTargets.PKG_FBLITE && Prefs.isFBLiteBlocked(this)) {
            if (isFBLiteReels()) {
                scheduleFBLiteBlockIfNotPending()
            } else {
                cancelPendingFBLiteBlock()
            }
            return
        }

        // FB main app: use delayed-confirm pattern to avoid false positives
        if (pkg == BlockTargets.PKG_FACEBOOK && Prefs.isFacebookBlocked(this)) {
            if (isFacebookReels()) {
                scheduleFBBlockIfNotPending()
            } else {
                cancelPendingFBBlock()
            }
            return
        }

        val shouldBlock = when (pkg) {
            BlockTargets.PKG_YOUTUBE,
            BlockTargets.PKG_YOUTUBE_REVANCED -> {
                Prefs.isYouTubeBlocked(this) && isYouTubeShorts(event)
            }
            else -> false
        }

        if (shouldBlock) {
            lastBlockedPkg = pkg
            lastBlockTime = now
            blockWithGlobalAction()
        }
    }

    private fun scheduleFBLiteBlockIfNotPending() {
        if (fblitePendingBlockRunnable != null) return  // already scheduled
        val runnable = Runnable {
            fblitePendingBlockRunnable = null
            // Re-confirm: still looks like Reels after the delay?
            val root = rootInActiveWindow ?: return@Runnable
            if (hasFBLiteFullScreenVideoInRecycler(root)) {
                lastBlockedPkg = BlockTargets.PKG_FBLITE
                lastBlockTime = System.currentTimeMillis()
                blockWithGlobalAction()
            }
        }
        fblitePendingBlockRunnable = runnable
        mainHandler.postDelayed(runnable, FBLITE_CONFIRM_DELAY_MS)
    }

    private fun cancelPendingFBLiteBlock() {
        fblitePendingBlockRunnable?.let { mainHandler.removeCallbacks(it) }
        fblitePendingBlockRunnable = null
    }

    private fun scheduleFBBlockIfNotPending() {
        if (fbPendingBlockRunnable != null) return  // already scheduled
        val runnable = Runnable {
            fbPendingBlockRunnable = null
            // Re-confirm: still looks like Reels after the delay?
            if (isFacebookReels()) {
                lastBlockedPkg = BlockTargets.PKG_FACEBOOK
                lastBlockTime = System.currentTimeMillis()
                blockWithGlobalAction()
            }
        }
        fbPendingBlockRunnable = runnable
        mainHandler.postDelayed(runnable, FB_CONFIRM_DELAY_MS)
    }

    private fun cancelPendingFBBlock() {
        fbPendingBlockRunnable?.let { mainHandler.removeCallbacks(it) }
        fbPendingBlockRunnable = null
    }

    // ─── Detection Logic ────────────────────────────────────────────────────

    private fun isYouTubeShorts(event: AccessibilityEvent): Boolean {
        val className = event.className?.toString() ?: ""
        if (className in BlockTargets.YOUTUBE_SHORTS_CLASSES) return true
        if (className.contains(".shorts.", ignoreCase = true)) return true

        val root = rootInActiveWindow ?: return false
        val hasEngagementPanel = containsAnyFullViewId(root, BlockTargets.YOUTUBE_ENGAGEMENT_PANEL_FULL_VIEW_IDS)

        val hasStrongShortsId = containsAnyFullViewId(root, BlockTargets.YOUTUBE_SHORTS_FULL_VIEW_IDS)
        if (hasStrongShortsId && !hasEngagementPanel) return true

        val hasShortsShortId = containsAnyViewId(root, BlockTargets.YOUTUBE_SHORTS_VIEW_IDS)
        val sourceClass = event.source?.className?.toString() ?: ""
        val isScrollableFeed = sourceClass.contains("RecyclerView") || sourceClass.contains("ViewPager")

        return hasShortsShortId && isScrollableFeed && !hasEngagementPanel
    }

    private fun isFacebookReels(): Boolean {
        val root = rootInActiveWindow ?: return false

        // Detection based on LIVE UI dump of Facebook Reels (2026-04-07).
        // Facebook obfuscates all resource-ids with "(name removed)", so we rely purely
        // on content-desc values which are stable and unique to the Reels player.
        //
        // Confirmed signals in live Reels dump:
        //   "Reel details"                     — individual Reel item node (strong signal)
        //   "Navigate to your Reels profile"   — header button unique to Reels player
        //   "Tap to show video controls"        — Reels video player controls hint
        //   "Pick viewer content to show"       — Reels audience filter button
        //
        // NOTE: Old signal "reels tab details" was NOT found in live dump — removed.
        // NOTE: resource-ids are all "(name removed)" — cannot use them.

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var inspected = 0
        var reelsSignalCount = 0

        while (queue.isNotEmpty() && inspected < MAX_NODE_SCAN) {
            val node = queue.removeFirst()
            val desc = node.contentDescription?.toString() ?: ""
            val descLower = desc.lowercase()

            // Strong definitive signal — individual reel item container
            if (descLower == "reel details") return true

            // Header button that only appears when inside the Reels player
            if (descLower == "navigate to your reels profile") reelsSignalCount++

            // Reels video player hint text — only shown inside Reels player
            if (descLower == "tap to show video controls") reelsSignalCount++

            // Audience filter button only rendered in Reels player header
            if (descLower == "pick viewer content to show") reelsSignalCount++

            // "Create reel" button sometimes visible in Reels feed header
            if (descLower == "create reel") reelsSignalCount++

            // Quick exit: messaging-specific descriptors mean we're NOT in Reels
            if (descLower.contains("type a message") ||
                descLower.contains("active now") ||
                descLower.contains("send message")) return false

            // If we have 2+ supporting signals, that's sufficient confidence
            if (reelsSignalCount >= 2) return true

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            inspected++
        }

        // At least 1 strong supporting signal (e.g. Reels profile button alone)
        return reelsSignalCount >= 1
    }

    private fun isFBLiteReels(): Boolean {
        val root = rootInActiveWindow ?: return false
        return hasFBLiteFullScreenVideoInRecycler(root)
    }

    /**
     * Structural heuristic based on live UI dumps:
     *
     * Reels player:    RecyclerView (scrollable) + video_view + NO video_player_controls/inline_progress_bar
     * News feed video: RecyclerView (scrollable) + video_view + HAS video_player_controls + inline_progress_bar
     * Stories feed:    RecyclerView (scrollable) + NO video_view at all
     * Messaging:       Text content present
     *
     * The key: news feed inline videos expose video_player_controls and inline_progress_bar_layout.
     * Reels full-screen player does NOT have these IDs — they are the perfect exclusion marker.
     */
    private fun hasFBLiteFullScreenVideoInRecycler(root: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var inspected = 0
        var hasScrollableRecycler = false
        var hasVideoView = false
        var hasNonReelsVideoId = false

        while (queue.isNotEmpty() && inspected < MAX_NODE_SCAN) {
            val node = queue.removeFirst()
            val cls = node.className?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""

            if (cls.contains("RecyclerView") && node.isScrollable) hasScrollableRecycler = true
            if (viewId == "com.facebook.lite:id/video_view") hasVideoView = true
            // If any inline-player-specific IDs are present, this is a feed video, not Reels
            if (viewId in BlockTargets.FBLITE_NON_REELS_VIDEO_IDS) hasNonReelsVideoId = true

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            inspected++
        }

        // Must have video_view in a scrollable RecyclerView, but NOT have inline player controls
        return hasScrollableRecycler && hasVideoView && !hasNonReelsVideoId
    }

    // ─── Node Traversal Helpers ─────────────────────────────────────────────

    /**
     * BFS traversal to find if any node has one of the target view IDs.
     * Stops early as soon as a match is found (efficient).
     */
    private fun containsAnyViewId(root: AccessibilityNodeInfo, targetIds: Set<String>): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var inspected = 0
        val maxNodes = 2500
        while (queue.isNotEmpty() && inspected < maxNodes) {
            val node = queue.removeFirst()
            val viewId = node.viewIdResourceName
            if (viewId != null) {
                val shortId = viewId.substringAfter("/")
                if (shortId in targetIds) return true
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            inspected++
        }
        return false
    }

    private fun containsAnyFullViewId(root: AccessibilityNodeInfo, fullViewIds: Set<String>): Boolean {
        for (id in fullViewIds) {
            val matches = root.findAccessibilityNodeInfosByViewId(id)
            if (!matches.isNullOrEmpty()) return true
        }
        return false
    }

    private fun blockWithGlobalAction() {
        mainHandler.post {
            Toast.makeText(applicationContext, TOAST_SHORT_FEED_BLOCKED, Toast.LENGTH_LONG).show()
            showBlockOverlay()
        }
    }

    private fun blockFullAppWithGlobalAction(toastMessage: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, toastMessage, Toast.LENGTH_LONG).show()
            performGlobalAction(GLOBAL_ACTION_HOME)
            showBlockOverlay()
        }
    }

    private fun performDoubleBackAction() {
        mainHandler.post {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun showBlockOverlay() {
        val overlayIntent = Intent(this, OverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }

        try {
            startActivity(overlayIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay", e)
        }
    }

    // ─── Foreground Notification ─────────────────────────────────────────────

    private fun startForegroundNotification() {
        createNotificationChannel()

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = ACTION_STOP_FROM_NOTIFICATION
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_shield_notif)
            .setColor(androidx.core.content.ContextCompat.getColor(this, R.color.notification_color))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)
            .build()

        startForeground(NOTIF_ID, notification)
        isForegroundShown = true
    }

    private fun stopForegroundCompat() {
        if (isForegroundShown) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
        isForegroundShown = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
