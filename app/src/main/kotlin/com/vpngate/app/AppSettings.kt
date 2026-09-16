package com.vpngate.app

data class AppSettings(
    val language: String = "system",      // "system" | "en" | "fa"
    val themeMode: String = "system",     // "system" | "light" | "dark"
    val sortBy: String = "score",         // "score" | "ping" | "speed" | "sessions"
    val autoRefreshMinutes: Int = 0,      // 0 = off, 10, 30, 60
    val useMirror: Boolean = false
)

object AppDefaults {
    val autoRefreshOptions = listOf(0, 10, 30, 60)
}