package com.vpngate.app.connection

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.vpngate.app.data.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ExportOutcome(val count: Int, val folder: String, val error: String? = null)

/** Writes OpenVPN configs to disk as .ovpn files in a dedicated folder. */
object OvnExporter {

    suspend fun export(context: Context, servers: List<VpnServer>): ExportOutcome =
        withContext(Dispatchers.IO) {
            var saved = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                for (server in servers) {
                    val config = server.openVpnConfig()
                    if (config.isBlank()) continue
                    try {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName(server))
                            put(MediaStore.Downloads.MIME_TYPE, "application/x-openvpn")
                            put(MediaStore.Downloads.RELATIVE_PATH, "Download/VPNGate")
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val uri = resolver.insert(collection, values) ?: continue
                        resolver.openOutputStream(uri)?.use { it.write(config.toByteArray()) }
                        resolver.update(
                            uri,
                            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                            null, null
                        )
                        saved++
                    } catch (e: Exception) {
                        // skip failing entries
                    }
                }
                ExportOutcome(saved, "Download/VPNGate", if (saved == 0) "write failed" else null)
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "VPNGate")
                dir.mkdirs()
                for (server in servers) {
                    val config = server.openVpnConfig()
                    if (config.isBlank()) continue
                    try {
                        File(dir, fileName(server)).writeText(config)
                        saved++
                    } catch (e: Exception) {
                        // skip failing entries
                    }
                }
                ExportOutcome(saved, dir.absolutePath, if (saved == 0) "write failed" else null)
            }
        }

    private fun fileName(server: VpnServer): String {
        val country = server.countryShort.uppercase().replace(Regex("[^A-Z0-9]"), "")
        val host = server.hostName.replace(Regex("[^A-Za-z0-9._-]"), "").take(40)
        return "$country-$host.ovpn"
    }
}