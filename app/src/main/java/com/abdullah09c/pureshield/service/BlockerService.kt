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

class BlockerService : AccessibilityService() {

    companion object {
        const val CHANNEL_ID = "pureshield_service"
        const val NOTIF_ID = 1001
        const val ACTION_STOP_FROM_NOTIFICATION = "com.abdullah09c.pureshield.action.STOP_FROM_NOTIFICATION"
        const val ACTION_SYNC_NOTIFICATION = "com.abdullah09c.pureshield.action.SYNC_NOTIFICATION"
        const val ACTION_DOUBLE_BACK = "com.abdullah09c.pureshield.action.DOUBLE_BACK"
        private const val TAG = "BlockerService"

        private const val BLOCK_ACTION_COOLDOWN_MS = 900L
        private const val MAX_NODE_SCAN = 400
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
    private var fbReelsEnterTime = 0L
    private var fbLiteReelsEnterTime = 0L
    private var ytShortsEnterTime = 0L

    // Screen state detection caching
    private var lastFBReelsCheckTime = 0L
    private var lastFBReelsResult = false

    private var lastFBLiteReelsCheckTime = 0L
    private var lastFBLiteReelsResult = false

    private var lastYTShortsCheckTime = 0L
    private var lastYTShortsResult = false
    // ─── Lifecycle ──────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        if (Prefs.isBlockerEnabled(this) && Prefs.hasAccessibilityConsent(this)) {
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

        val eventType = event.eventType
        if ((eventType and TARGET_EVENT_MASK) == 0) return

        // Only act when blocker is enabled
        if (!Prefs.isBlockerEnabled(this) || !Prefs.hasAccessibilityConsent(this)) {
            if (isForegroundShown) stopForegroundCompat()
            return
        }

        if (!isForegroundShown) {
            startForegroundNotification()
        }

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return

        // Reset timers if the user navigated away
        if (pkg != BlockTargets.PKG_FACEBOOK) fbReelsEnterTime = 0L
        if (pkg != BlockTargets.PKG_FBLITE) fbLiteReelsEnterTime = 0L
        if (pkg != BlockTargets.PKG_YOUTUBE && pkg != BlockTargets.PKG_YOUTUBE_REVANCED) ytShortsEnterTime = 0L
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

        val instantBlock = Prefs.isInstantBlock(this)

        // FB Lite gets special treatment: block when Reels is confirmed 
        if (pkg == BlockTargets.PKG_FBLITE && Prefs.isFBLiteBlocked(this)) {
            if (checkIsFBLiteReelsCached(eventType, now)) {
                if (instantBlock) {
                    lastBlockedPkg = pkg
                    lastBlockTime = now
                    fbLiteReelsEnterTime = 0L
                    blockWithGlobalAction()
                    return
                }

                if (fbLiteReelsEnterTime == 0L) {
                    fbLiteReelsEnterTime = now
                }
                
                // For FB Lite, we block immediately on genuine scroll after 800ms stabilization window
                if (isGenuineUserScroll(event) && (now - fbLiteReelsEnterTime > 800L)) {
                    lastBlockedPkg = pkg
                    lastBlockTime = now
                    fbLiteReelsEnterTime = 0L
                    blockWithGlobalAction()
                }
            } else {
                fbLiteReelsEnterTime = 0L
            }
            return
        }

        // FB main app
        if (pkg == BlockTargets.PKG_FACEBOOK && Prefs.isFacebookBlocked(this)) {
            if (checkIsFacebookReelsCached(eventType, now)) {
                if (instantBlock) {
                    lastBlockedPkg = pkg
                    lastBlockTime = now
                    fbReelsEnterTime = 0L
                    blockWithGlobalAction()
                    return
                }

                if (fbReelsEnterTime == 0L) {
                    fbReelsEnterTime = now
                }
                
                // Ignore automatic layout scrolls when Reels first opens (800ms stabilization window)
                // AND ensure the scroll event is a genuine user swipe, not a video progress bar update
                if (isGenuineUserScroll(event) && (now - fbReelsEnterTime > 800L)) {
                    lastBlockedPkg = pkg
                    lastBlockTime = now
                    fbReelsEnterTime = 0L // reset for next time
                    blockWithGlobalAction()
                }
            } else {
                // Reset timer when we exit Reels so re-entry works correctly
                fbReelsEnterTime = 0L
            }
            return
        }

        // YouTube and YouTube ReVanced (Shorts Block)
        val isYouTube = pkg == BlockTargets.PKG_YOUTUBE || pkg == BlockTargets.PKG_YOUTUBE_REVANCED
        if (isYouTube && Prefs.isYouTubeBlocked(this)) {
            if (checkIsYouTubeShortsCached(event, now)) {
                if (instantBlock) {
                    lastBlockedPkg = pkg
                    lastBlockTime = now
                    ytShortsEnterTime = 0L
                    blockWithGlobalAction()
                    return
                }

                if (ytShortsEnterTime == 0L) {
                    ytShortsEnterTime = now
                }

                if (isGenuineUserScroll(event) && (now - ytShortsEnterTime > 800L)) {
                    lastBlockedPkg = pkg
                    lastBlockTime = now
                    ytShortsEnterTime = 0L
                    blockWithGlobalAction()
                }
            } else {
                ytShortsEnterTime = 0L
            }
            return
        }
    }

    private fun checkIsFacebookReelsCached(eventType: Int, now: Long): Boolean {
        val needsFreshCheck = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                (now - lastFBReelsCheckTime > 1000L)
        if (!needsFreshCheck) return lastFBReelsResult
        lastFBReelsCheckTime = now
        lastFBReelsResult = isFacebookReels()
        return lastFBReelsResult
    }

    private fun checkIsFBLiteReelsCached(eventType: Int, now: Long): Boolean {
        val needsFreshCheck = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                (now - lastFBLiteReelsCheckTime > 1000L)
        if (!needsFreshCheck) return lastFBLiteReelsResult
        lastFBLiteReelsCheckTime = now
        lastFBLiteReelsResult = isFBLiteReels()
        return lastFBLiteReelsResult
    }

    private fun checkIsYouTubeShortsCached(event: AccessibilityEvent, now: Long): Boolean {
        val eventType = event.eventType
        val needsFreshCheck = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                (now - lastYTShortsCheckTime > 1000L)
        if (!needsFreshCheck) return lastYTShortsResult
        lastYTShortsCheckTime = now
        lastYTShortsResult = isYouTubeShorts(event)
        return lastYTShortsResult
    }

    // ─── Detection Logic ────────────────────────────────────────────────────

    private fun isGenuineUserScroll(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false

        val className = event.className?.toString() ?: ""
        
        // Filter out obvious horizontal tab ViewPagers (Reels/Shorts scroll vertically via ViewPager2 or RecyclerView)
        if (className.contains("ViewPager") && !className.contains("ViewPager2")) {
            return false
        }

        // Filter out obvious fake scrolls from progress bars
        if (className.contains("SeekBar") || className.contains("ProgressBar")) {
            return false
        }

        // Check for vertical scroll deltas on Android P and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dx = Math.abs(event.scrollDeltaX)
            val dy = Math.abs(event.scrollDeltaY)
            // If horizontal scroll is dominant, it is NOT a reels swipe (reels are vertical)
            if (dx > dy) {
                return false
            }
            if (dy > 5) {
                return true
            }
        }

        // If it's a known vertical/scrollable container, it's very likely a genuine user swipe.
        if (className.contains("RecyclerView") || className.contains("ViewPager2") || className.contains("ListView") || className.contains("ScrollView")) {
            return true
        }

        // If the scroll event reports more than 0 items (typical for lists), consider it genuine
        if (event.itemCount > 0) {
            return true
        }

        return false
    }

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
            
            // Only inspect nodes visible to the user (filters out offscreen cached tab fragments)
            if (!node.isVisibleToUser) continue

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

        // Require at least 2 supporting signals (e.g., reels profile profile icon + reels controls)
        // to avoid single "create reel" button false positives on standard news feeds.
        return reelsSignalCount >= 2
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
            
            // Only inspect nodes visible to the user to avoid picking up cached background pages
            if (!node.isVisibleToUser) continue

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
        val maxNodes = 400
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
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var inspected = 0
        val maxNodes = 400

        while (queue.isNotEmpty() && inspected < maxNodes) {
            val node = queue.removeFirst()
            val viewId = node.viewIdResourceName
            if (viewId != null && viewId in fullViewIds) return true

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            inspected++
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

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIF_ID, notification)
        isForegroundShown = true
    }

    private fun stopForegroundCompat() {
        if (isForegroundShown) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NOTIF_ID)
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
