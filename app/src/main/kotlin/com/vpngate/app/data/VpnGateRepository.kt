package com.vpngate.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vpngate.app.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "vpn_gate")

class VpnGateRepository(private val context: Context) {

    // --- Server store (accumulated, deduplicated, survives restarts) ---
    private val lastFetchKey = longPreferencesKey("last_fetch_ms")

    private fun serversFile() = File(context.filesDir, "servers.json")

    /** Loads every config saved so far (accumulated across refreshes). */
    suspend fun loadServers(): List<VpnServer> = withContext(Dispatchers.IO) {
        val file = serversFile()
        if (!file.exists()) return@withContext emptyList()
        try {
            val text = file.readText()
            val json = JSONArray(text)
            buildList {
                for (i in 0 until json.length()) {
                    val obj = json.optJSONObject(i) ?: continue
                    val s = serverFromJson(obj) ?: continue
                    add(s)
                }
            }.distinctBy { it.ip }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Merges a fresh fetch into the stored list.
     * Existing entries are kept untouched; only new (non-duplicate) IPs are added.
     */
    suspend fun mergeServers(fresh: List<VpnServer>): List<VpnServer> {
        val existing = loadServers()
        val byIp = LinkedHashMap<String, VpnServer>()
        existing.forEach { byIp[it.ip] = it }
        fresh.forEach { if (it.ip !in byIp) byIp[it.ip] = it }
        val merged = byIp.values.toList()
        writeServers(merged)
        context.dataStore.edit { prefs ->
            prefs[lastFetchKey] = System.currentTimeMillis()
        }
        return merged
    }

    private suspend fun writeServers(servers: List<VpnServer>) {
        withContext(Dispatchers.IO) {
            try {
                val json = JSONArray()
                servers.forEach { json.put(serverToJson(it)) }
                serversFile().writeText(json.toString())
            } catch (e: Exception) {
                // ignore write failures to avoid crashing a refresh
            }
        }
    }

    /** Removes configs by IP and persists the remaining list. */
    suspend fun deleteServers(ips: Set<String>): List<VpnServer> {
        val remaining = loadServers().filterNot { it.ip in ips }
        writeServers(remaining)
        return remaining
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                serversFile().delete()
            } catch (e: Exception) {
                // ignore
            }
        }
        context.dataStore.edit { prefs ->
            prefs.remove(lastFetchKey)
        }
    }

    suspend fun lastFetchTime(): Long? =
        context.dataStore.data.first()[lastFetchKey]

    private fun serverToJson(s: VpnServer): JSONObject = JSONObject().apply {
        put("host", s.hostName)
        put("ip", s.ip)
        put("score", s.score)
        put("ping", s.ping)
        put("speed", s.speed)
        put("country", s.countryLong)
        put("cc", s.countryShort)
        put("sessions", s.numVpnSessions)
        put("uptime", s.uptime)
        put("users", s.totalUsers)
        put("traffic", s.totalTraffic)
        put("log", s.logType)
        put("op", s.operator)
        put("msg", s.message)
        put("cfg", s.openVpnConfigBase64)
        put("proto", s.protoType)
    }

    private fun serverFromJson(o: JSONObject): VpnServer? {
        val host = o.optString("host")
        val ip = o.optString("ip")
        if (host.isBlank() || ip.isBlank()) return null
        return VpnServer(
            hostName = host,
            ip = ip,
            score = o.optLong("score"),
            ping = o.optInt("ping"),
            speed = o.optLong("speed"),
            countryLong = o.optString("country"),
            countryShort = o.optString("cc"),
            numVpnSessions = o.optInt("sessions"),
            uptime = o.optLong("uptime"),
            totalUsers = o.optLong("users"),
            totalTraffic = o.optLong("traffic"),
            logType = o.optString("log"),
            operator = o.optString("op"),
            message = o.optString("msg"),
            openVpnConfigBase64 = o.optString("cfg"),
            protoType = o.optString("proto", "UDP")
        )
    }

    // --- Settings ---
    private val langKey = stringPreferencesKey("lang")
    private val themeKey = stringPreferencesKey("theme")
    private val sortKey = stringPreferencesKey("sort")
    private val autoRefreshKey = intPreferencesKey("auto_refresh_min")
    private val useMirrorKey = booleanPreferencesKey("use_mirror")

    fun settingsFlow(): Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            language = prefs[langKey] ?: "system",
            themeMode = prefs[themeKey] ?: "system",
            sortBy = prefs[sortKey] ?: "score",
            autoRefreshMinutes = prefs[autoRefreshKey] ?: 0,
            useMirror = prefs[useMirrorKey] ?: false
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[langKey] = settings.language
            prefs[themeKey] = settings.themeMode
            prefs[sortKey] = settings.sortBy
            prefs[autoRefreshKey] = settings.autoRefreshMinutes
            prefs[useMirrorKey] = settings.useMirror
        }
    }
}