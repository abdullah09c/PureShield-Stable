package com.abdullah09c.pureshield.util

/**
 * Contains all known view node signatures for detecting Reels/Shorts screens.
 * These are the view IDs / class names / content descriptions that appear ONLY
 * when the user is inside a Reels or Shorts section.
 *
 * Detection strategy:
 * - YouTube: detect ShortsActivity class name OR "reel_watch_player" view ID
 * - Facebook: detect "reels_video_container" OR "video_timeline" OR reels tab selected
 * - FB Lite: detect reels-specific node IDs in com.facebook.lite package
 * - Instagram: block entire app (full-app block approach)
 * - TikTok: block entire app (full-app block approach)
 */
object BlockTargets {

    // Package names
    const val PKG_YOUTUBE = "com.google.android.youtube"
    const val PKG_YOUTUBE_REVANCED = "app.revanced.android.youtube"
    const val PKG_FACEBOOK = "com.facebook.katana"
    const val PKG_FBLITE = "com.facebook.lite"
    const val PKG_INSTAGRAM = "com.instagram.android"
    const val PKG_TIKTOK = "com.zhiliaoapp.musically"
    const val PKG_TIKTOK_ALT = "com.ss.android.ugc.trill" // TikTok in some regions
    const val PKG_TIKTOK_AWEME = "com.ss.android.ugc.aweme" // TikTok China variant
    const val PKG_TIKTOK_LITE = "com.tiktok.lite.go" // TikTok Lite
    const val PKG_TIKTOK_LITE_LEGACY = "com.zhiliaoapp.musically.go" // Older TikTok Lite package
    const val PKG_TIKTOK_LITE_ALT = "com.ss.android.ugc.trill.go" // TikTok Lite variant in some regions

    // YouTube Shorts detection
    // These class names appear when user is inside the Shorts player
    val YOUTUBE_SHORTS_CLASSES = setOf(
        "com.google.android.apps.youtube.app.shorts.ShortsPivotActivity",
        "com.google.android.apps.youtube.app.watchwhile.shorts.ShortsActivity"
    )

    // Exact full view IDs for YouTube Shorts player surfaces.
    val YOUTUBE_SHORTS_FULL_VIEW_IDS = setOf(
        "com.google.android.youtube:id/reel_watch_player",
        "com.google.android.youtube:id/reel_player_page_container",
        "com.google.android.youtube:id/reel_watch_fragment_container",
        "com.google.android.youtube:id/reel_recycler",
        "app.revanced.android.youtube:id/reel_watch_player",
        "app.revanced.android.youtube:id/reel_player_page_container",
        "app.revanced.android.youtube:id/reel_watch_fragment_container",
        "app.revanced.android.youtube:id/reel_recycler"
    )

    val YOUTUBE_ENGAGEMENT_PANEL_FULL_VIEW_IDS = setOf(
        "com.google.android.youtube:id/engagement_panel_content",
        "app.revanced.android.youtube:id/engagement_panel_content"
    )
    // View IDs unique to YouTube Shorts
    val YOUTUBE_SHORTS_VIEW_IDS = setOf(
        "reel_watch_player",
        "shorts_container",
        "shorts_player_sheet",
        "reel_player_page_container",
        "reel_watch_fragment_container",
        "shorts_player_root",
        "reel_recycler"
    )

    // Facebook Reels detection (strict player/container IDs)
    val FACEBOOK_REELS_FULL_VIEW_IDS = setOf(
        "com.facebook.katana:id/reels_video_container",
        "com.facebook.katana:id/reels_player_view",
        "com.facebook.katana:id/video_timeline_story_item"
    )

    val FACEBOOK_REELS_VIEW_IDS = setOf(
        "reels_video_container",
        "reels_player_view",
        "video_timeline_story_item",
        "short_video_container",
        "short_video_player",
        "reels_view_pager",
        "reels_tab",
        "clips_tab"
    )

    // Facebook Lite Reels detection
    // NOTE: FB Lite heavily obfuscates its view IDs. During an active Reel, the
    // ONLY named resource-id visible is video_view inside a scrollable RecyclerView.
    // Regular news feed videos also use video_view, but expose additional IDs:
    //   - video_player_controls
    //   - inline_progress_bar_layout
    //   - video_play_icon
    // These are ABSENT in the Reels full-screen player, making them reliable exclusion markers.
    val FBLITE_REELS_FULL_VIEW_IDS = setOf(
        "com.facebook.lite:id/video_view"
    )

    val FBLITE_REELS_VIEW_IDS = setOf(
        "video_view"
    )

    // These IDs appear in FB Lite NEWS FEED inline videos but NOT in Reels.
    // If any of these are present, we are NOT in Reels.
    val FBLITE_NON_REELS_VIDEO_IDS = setOf(
        "com.facebook.lite:id/video_player_controls",
        "com.facebook.lite:id/inline_progress_bar_layout",
        "com.facebook.lite:id/video_play_icon",
        "com.facebook.lite:id/inline_progress_bar"
    )

    // Instagram: block entire app (full-app block approach)

    // All packages we monitor (used by accessibility service filter)
    val ALL_PACKAGES = setOf(
        PKG_YOUTUBE,
        PKG_YOUTUBE_REVANCED,
        PKG_FACEBOOK,
        PKG_FBLITE,
        PKG_INSTAGRAM,
        PKG_TIKTOK,
        PKG_TIKTOK_ALT,
        PKG_TIKTOK_AWEME,
        PKG_TIKTOK_LITE,
        PKG_TIKTOK_LITE_LEGACY,
        PKG_TIKTOK_LITE_ALT
    )
}
