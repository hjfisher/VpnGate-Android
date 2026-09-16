package com.vpngate.app.connection

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.vpngate.app.R
import com.vpngate.app.data.VpnServer
import java.io.File

/**
 * Handles storing the OpenVPN config and launching a VPN client.
 *
 * Two reliable client apps on the Play Store / F-Droid:
 *  - de.blinkt.openvpn  (OpenVPN for Android, F-Droid)
 *  - net.openvpn.connect.android (OpenVPN Connect)
 *
 * We detect the installed client, otherwise fall back to an
 * ACTION_VIEW with a proper mime type which opens any file picker /
 * OpenVPN-aware app installed on the device.
 */
object VpnConnector {

    private const val MIME_OVPN = "application/x-openvpn-profile"

    private val clientPackages = listOf(
        "de.blinkt.openvpn",
        "net.openvpn.connect.android",
        "net.openvpn.connect",
        "com.openvpn.client.android"
    )

    fun saveConfig(context: Context, server: VpnServer): File {
        val dir = File(context.filesDir, "ovpn")
        if (!dir.exists()) dir.mkdirs()
        val safeName = server.hostName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(dir, "$safeName.ovpn")
        file.writeText(server.openVpnConfig())
        return file
    }

    /** Returns true if any known OpenVPN client is installed. */
    fun hasOpenVpnClient(context: Context): Boolean =
        clientPackages.any { pkg ->
            context.packageManager.getLaunchIntentForPackage(pkg) != null
        }

    fun connect(context: Context, server: VpnServer) {
        val file = saveConfig(context, server)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        // 1) Try the installed OpenVPN client directly
        for (pkg in clientPackages) {
            if (context.packageManager.getLaunchIntentForPackage(pkg) != null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, MIME_OVPN)
                        setPackage(pkg)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                } catch (e: ActivityNotFoundException) {
                    // try next
                }
            }
        }

        // 2) Generic share/open action
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, MIME_OVPN)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.openvpn_client_required)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.openvpn_client_needed), Toast.LENGTH_LONG
            ).show()
        }
    }

    fun shareConfig(context: Context, server: VpnServer) {
        val file = saveConfig(context, server)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_OVPN
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_TEXT, "# ${server.hostName} — VPN Gate config")
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_config)))
    }

    fun copyToClipboard(context: Context, server: VpnServer) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(
            "OpenVPN Config",
            server.openVpnConfig()
        )
        cm.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.config_copied), Toast.LENGTH_SHORT).show()
    }
}