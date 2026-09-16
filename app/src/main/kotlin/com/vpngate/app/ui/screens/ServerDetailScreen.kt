package com.vpngate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpngate.app.R
import com.vpngate.app.data.VpnServer
import com.vpngate.app.connection.PingTools
import com.vpngate.app.connection.VpnConnector
import com.vpngate.app.ui.components.CountryFlagBadge
import com.vpngate.app.ui.components.formatScore
import com.vpngate.app.ui.components.pingColor
import com.vpngate.app.ui.components.uptimeFormattedLocalized
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    server: VpnServer,
    isFavorite: Boolean,
    measuredPing: Long?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRecordPing: (String, Long) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkingPing by remember { mutableStateOf(false) }
    var localMeasuredPing by remember { mutableStateOf(measuredPing) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(server.hostName.truncate(22)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite_toggle),
                            tint = if (isFavorite) Color(0xFFF472B6)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_server),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header with flag + country
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CountryFlagBadge(
                        countryCode = server.countryShort,
                        modifier = Modifier.size(56.dp),
                        fontSize = 16.sp
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = server.countryLong,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = server.operator,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Ping check banner
            val measured = localMeasuredPing
            val displayPing = measured ?: server.ping.toLong()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        checkingPing = true
                        scope.launch {
                            val result = PingTools.tcpPing(server.ip)
                            if (result != null) {
                                localMeasuredPing = result
                                onRecordPing(server.hostName, result)
                            }
                            checkingPing = false
                        }
                    },
                    enabled = !checkingPing
                ) {
                    if (checkingPing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.checking), modifier = Modifier.padding(start = 4.dp))
                    } else {
                        Icon(Icons.Filled.NetworkCheck, contentDescription = null)
                        Text(stringResource(R.string.check_connection), modifier = Modifier.padding(start = 4.dp))
                    }
                }

                Text(
                    text = if (measured != null)
                        stringResource(R.string.ping_measured, measured)
                    else stringResource(R.string.ping_service, server.ping),
                    color = pingColor(displayPing.toInt()),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Config preview (if available)
            val configText = server.openVpnConfig()
            if (configText.isNotEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.config_preview),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = extractKeyLines(configText),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 8
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Text(
                text = stringResource(R.string.status_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            InfoRow(stringResource(R.string.field_ip), server.ip)
            InfoRow(stringResource(R.string.field_host), server.hostName)
            InfoRow(stringResource(R.string.field_score), formatScore(server.score))
            InfoRow(stringResource(R.string.field_speed), "${server.speedMbps} Mbit/s")
            InfoRow(stringResource(R.string.field_sessions), server.numVpnSessions.toString())
            InfoRow(stringResource(R.string.field_uptime), server.uptimeFormattedLocalized())
            InfoRow(stringResource(R.string.field_users), server.totalUsers.toString())
            InfoRow(stringResource(R.string.field_traffic), server.totalTrafficFormatted)
            InfoRow(stringResource(R.string.field_logtype), server.logType)

            if (server.message.isNotBlank()) {
                InfoRow(stringResource(R.string.field_message), server.message)
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons
            Button(
                onClick = { VpnConnector.connect(context, server) },
                modifier = Modifier.fillMaxWidth(),
                enabled = configText.isNotEmpty()
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null)
                Text(stringResource(R.string.connect), modifier = Modifier.padding(start = 6.dp))
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { VpnConnector.copyToClipboard(context, server) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CopyAll, contentDescription = null)
                    Text(stringResource(R.string.copy), modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = { VpnConnector.shareConfig(context, server) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text(stringResource(R.string.share), modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.client_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_server)) },
            text = { Text(stringResource(R.string.delete_server_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun String.truncate(maxLen: Int): String =
    if (length <= maxLen) this else take(maxLen - 3) + "…"

private fun extractKeyLines(config: String): String {
    val lines = config.lineSequence()
        .filter { !it.startsWith("#") && it.isNotBlank() }
        .map { it.trim() }
        .filter { line ->
            line.startsWith("remote ") ||
                line.startsWith("proto ") ||
                line.startsWith("client") ||
                line.startsWith("dev ") ||
                line.contains("verb ")
        }
        .take(6)
    return lines.joinToString("\n")
}