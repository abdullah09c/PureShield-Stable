package com.abdullah09c.pureshield.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private const val FILE = "pureshield_prefs"

    // Keys
    const val KEY_BLOCKER_ENABLED = "blocker_enabled"
    const val KEY_BLOCK_YOUTUBE = "block_youtube"
    const val KEY_BLOCK_FACEBOOK = "block_facebook"
    const val KEY_BLOCK_FBLITE = "block_fblite"
    const val KEY_BLOCK_INSTAGRAM = "block_instagram"
    const val KEY_BLOCK_TIKTOK = "block_tiktok"
    const val KEY_BLOCK_MESSAGE = "block_message"
    const val KEY_DNS_PRESET = "dns_preset"
    const val KEY_PIN = "protection_pin"
    const val KEY_PROTECTION_ENABLED = "protection_enabled"
    const val KEY_START_ON_BOOT = "start_on_boot"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isBlockerEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_BLOCKER_ENABLED, false)
    fun setBlockerEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_BLOCKER_ENABLED, v).apply()

    fun isYouTubeBlocked(ctx: Context) = prefs(ctx).getBoolean(KEY_BLOCK_YOUTUBE, true)
    fun setYouTubeBlocked(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_BLOCK_YOUTUBE, v).apply()

    fun isFacebookBlocked(ctx: Context) = prefs(ctx).getBoolean(KEY_BLOCK_FACEBOOK, true)
    fun setFacebookBlocked(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_BLOCK_FACEBOOK, v).apply()

    fun isFBLiteBlocked(ctx: Context) = prefs(ctx).getBoolean(KEY_BLOCK_FBLITE, true)
    fun setFBLiteBlocked(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_BLOCK_FBLITE, v).apply()

    fun isInstagramBlocked(ctx: Context) = prefs(ctx).getBoolean(KEY_BLOCK_INSTAGRAM, true)
    fun setInstagramBlocked(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_BLOCK_INSTAGRAM, v).apply()

    fun isTikTokBlocked(ctx: Context) = prefs(ctx).getBoolean(KEY_BLOCK_TIKTOK, true)
    fun setTikTokBlocked(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_BLOCK_TIKTOK, v).apply()

    fun getBlockMessage(ctx: Context): String =
        prefs(ctx).getString(KEY_BLOCK_MESSAGE, ctx.getString(com.abdullah09c.pureshield.R.string.default_block_message))
            ?: ctx.getString(com.abdullah09c.pureshield.R.string.default_block_message)

    fun setBlockMessage(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_BLOCK_MESSAGE, v).apply()

    fun getDnsPreset(ctx: Context) = prefs(ctx).getString(KEY_DNS_PRESET, DnsPreset.NONE.name) ?: DnsPreset.NONE.name
    fun setDnsPreset(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_DNS_PRESET, v).apply()

    fun getPin(ctx: Context): String = prefs(ctx).getString(KEY_PIN, "") ?: ""
    fun setPin(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_PIN, v).apply()

    fun isProtectionEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_PROTECTION_ENABLED, false)
    fun setProtectionEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_PROTECTION_ENABLED, v).apply()

    fun isStartOnBoot(ctx: Context) = prefs(ctx).getBoolean(KEY_START_ON_BOOT, true)
    fun setStartOnBoot(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_START_ON_BOOT, v).apply()
}

enum class DnsPreset(val displayName: String, val address: String, val features: List<String>) {
    NONE("None (Disabled)", "", listOf()),
    
    CLEANBROWSING_FAMILY("CleanBrowsing Family", "family-filter-dns.cleanbrowsing.org", listOf(
        "Blocks Adult Content",
        "Blocks Proxies & VPNs",
        "Blocks Mixed Content (e.g. Reddit)",
        "Blocks Malware & Phishing",
        "Forces SafeSearch"
    )),
    
    CLEANBROWSING_ADULT("CleanBrowsing Adult", "adult-filter-dns.cleanbrowsing.org", listOf(
        "Blocks Adult Content",
        "Blocks Malware & Phishing",
        "Forces SafeSearch",
        "Allows Proxies & VPNs",
        "Allows Mixed Content (e.g. Reddit)"
    )),

    CLEANBROWSING_SECURITY("CleanBrowsing Security", "security-filter-dns.cleanbrowsing.org", listOf(
        "Blocks Malware, Phishing & Spam",
        "Updated Hourly",
        "Allows Adult Content"
    )),
    
    CLOUDFLARE_FAMILY("Cloudflare Family", "family.cloudflare-dns.com", listOf(
        "Blocks Adult Content",
        "Blocks Malware",
        "High Speed DNS"
    )),

    CLOUDFLARE_SECURITY("Cloudflare Security", "security.cloudflare-dns.com", listOf(
        "Blocks Malware",
        "High Speed DNS",
        "Allows Adult Content"
    ))
}
