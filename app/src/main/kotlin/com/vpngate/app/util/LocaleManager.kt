package com.vpngate.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the selected language before composition. Configurations in
 * Android 13+ are handled automatically by the system, but with the manual
 * approach below we get consistent behaviour on every API level and inside
 * Compose screens (resources re-resolved on recreate).
 */
object LocaleManager {

    fun apply(context: Context, language: String) {
        val locale = when (language) {
            "fa" -> Locale("fa")
            "en" -> Locale("en")
            else -> Locale.getDefault()
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        // Application context resources are recreated through updateConfiguration
        val appResources = context.applicationContext.resources
        appResources.updateConfiguration(config, appResources.displayMetrics)
    }
}