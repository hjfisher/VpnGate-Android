package com.vpngate.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * VPN Gate has no official, stable download server guarantees.
 * In restricted networks the main host is often blocked, so we try a
 * list of addresses in order. "Alternate" endpoints (jina.ai proxy)
 * help when the main site is unreachable — enabled from Settings.
 */
object VpnGateApi {

    private const val MAIN_HTTP = "http://www.vpngate.net/api/iphone/"
    private const val MAIN_HTTPS = "https://www.vpngate.net/api/iphone/"
    private const val BARE_HTTPS = "https://vpngate.net/api/iphone/"

    // The jina.ai reader proxies the URL and returns the plain text body.
    private const val MIRROR_JINA = "https://r.jina.ai/http://www.vpngate.net/api/iphone/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Returns the decoded CSV payload from the first endpoint that works.
     * [useMirror] adds the mirror address to the candidates.
     */
    fun fetchRawCsv(useMirror: Boolean = false): String {
        val endpoints = mutableListOf(MAIN_HTTPS, MAIN_HTTP, BARE_HTTPS)
        if (useMirror) {
            // With a mirror configured, alternate order: mirror first is often
            // the only reachable one in filtered networks.
            endpoints.reverse()
            endpoints.add(MIRROR_JINA)
            endpoints.reverse()
        }

        var lastError: Exception? = null
        var successBody: String? = null
        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android) VpnGate-Client")
                    .header("Accept", "text/plain,*/*")
                    .build()
                var httpError: RuntimeException? = null
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        httpError = RuntimeException("HTTP ${response.code}")
                    } else {
                        val body = response.body?.string().orEmpty()
                        if (body.isNotBlank()) successBody = body
                    }
                }
                if (httpError != null) {
                    lastError = httpError
                    continue
                }
                val body = successBody
                if (body != null) return body
            } catch (e: Exception) {
                lastError = e
            }
        }
        // All endpoints failed (or returned empty) → no way to reach the API
        throw lastError ?: RuntimeException("All endpoints unavailable")
    }
}