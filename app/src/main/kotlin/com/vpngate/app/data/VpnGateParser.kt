package com.vpngate.app.data

import java.io.BufferedReader

/**
 * Parses the CSV returned by the VPN Gate public API.
 *
 * Format example:
 *   *vpn_servers
 *   #HostName,IP,Score,...
 *   public-vpn-88,219.100.37.30,2837699,14,...,<base64 ovpn config>
 *
 * Lines starting with '#' or '*' are metadata and are skipped.
 * The OpenVPN base64 payload never contains commas, so a naive
 * split on ',' is safe. Base64 is always the last column.
 */
object VpnGateParser {

    fun parse(body: String): List<VpnServer> {
        val servers = mutableListOf<VpnServer>()
        val reader = BufferedReader(body.reader())

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachLine
            if (line.startsWith("#") || line.startsWith("*")) return@forEachLine

            val parts = line.split(",", limit = 15)
            if (parts.size < 15) return@forEachLine

            val base64 = parts[14]
            val protoType = detectProto(base64)
            val server = VpnServer(
                hostName = parts[0],
                ip = parts[1],
                score = parts[2].toLongOrNull() ?: 0,
                ping = parts[3].toIntOrNull() ?: 0,
                speed = parts[4].toLongOrNull() ?: 0,
                countryLong = parts[5],
                countryShort = parts[6],
                numVpnSessions = parts[7].toIntOrNull() ?: 0,
                uptime = parts[8].toLongOrNull() ?: 0,
                totalUsers = parts[9].toLongOrNull() ?: 0,
                totalTraffic = parts[10].toLongOrNull() ?: 0,
                logType = parts[11],
                operator = parts[12],
                message = parts[13],
                openVpnConfigBase64 = base64,
                protoType = protoType
            )
            servers.add(server)
        }
        return servers.distinctBy { it.ip }
    }

    /** Detects the OpenVPN transport from the config: "TCP", "UDP" or "BOTH". */
    private fun detectProto(base64: String): String {
        val config = try {
            String(android.util.Base64.decode(base64, android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            ""
        }
        val hasUdp = config.contains("proto udp", ignoreCase = true)
        val hasTcp = config.contains("proto tcp", ignoreCase = true)
        return when {
            hasUdp && hasTcp -> "BOTH"
            hasTcp -> "TCP"
            else -> "UDP"
        }
    }
}