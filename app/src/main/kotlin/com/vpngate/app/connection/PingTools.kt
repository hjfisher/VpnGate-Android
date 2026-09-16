package com.vpngate.app.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTools {

    /**
     * Measures TCP connect latency to a VPN Gate endpoint.
     * The OpenVPN servers accept connections on port 443 (or 443/udp).
     * Returns latency in ms, or null on failure.
     */
    suspend fun tcpPing(host: String, port: Int = 443, timeoutMs: Int = 4000): Long? =
        withContext(Dispatchers.IO) {
            try {
                val start = System.nanoTime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    socket.setSoTimeout(1000)
                    socket.getOutputStream().write(byteArrayOf(0x00))
                    socket.getOutputStream().flush()
                    // read nothing, just measure connection time
                }
                val elapsedMs = (System.nanoTime() - start) / 1_000_000
                elapsedMs
            } catch (e: Exception) {
                null
            }
        }
}