package com.vpngate.app.data

data class VpnServer(
    val hostName: String,
    val ip: String,
    val score: Long,
    val ping: Int,
    val speed: Long,
    val countryLong: String,
    val countryShort: String,
    val numVpnSessions: Int,
    val uptime: Long,
    val totalUsers: Long,
    val totalTraffic: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val openVpnConfigBase64: String,
    val protoType: String = "UDP"
) {
    // Decode the OpenVPN config on demand (can be large)
    fun openVpnConfig(): String =
        try {
            String(android.util.Base64.decode(openVpnConfigBase64, android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            ""
        }

    val speedMbps: Float
        get() = speed / 1_000_000f

    val totalTrafficFormatted: String
        get() {
            val tb = 1_000_000_000_000L
            val gb = 1_000_000_000L
            val mb = 1_000_000L
            return if (totalTraffic >= tb) "%.1f TB".format(totalTraffic.toDouble() / tb.toDouble())
            else if (totalTraffic >= gb) "%.1f GB".format(totalTraffic.toDouble() / gb.toDouble())
            else if (totalTraffic >= mb) "%.1f MB".format(totalTraffic.toDouble() / mb.toDouble())
            else "$totalTraffic B"
        }
}