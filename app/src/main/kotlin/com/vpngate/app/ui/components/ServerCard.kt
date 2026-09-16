package com.vpngate.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpngate.app.R
import com.vpngate.app.data.VpnServer
import com.vpngate.app.ui.theme.Success
import com.vpngate.app.ui.theme.Warning
import java.util.Locale

/** Circular badge showing the ISO country code (flag emoji is unsupported on some devices). */
@Composable
fun CountryFlagBadge(countryCode: String, modifier: Modifier = Modifier, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countryCode.uppercase(Locale.US),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ScoreBadge(score: Long, modifier: Modifier = Modifier) {
    val color = when {
        score >= 1_000_000 -> Success
        score >= 100_000 -> Color(0xFF60A5FA)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.score_label, formatScore(score)),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

fun formatScore(score: Long): String = when {
    score >= 1_000_000 -> "%.1fM".format(score / 1_000_000.0)
    score >= 1_000 -> "%.1fK".format(score / 1_000.0)
    else -> score.toString()
}

fun pingColor(ping: Int): Color = when {
    ping <= 0 -> Color.Gray
    ping < 60 -> Success
    ping < 150 -> Color(0xFF60A5FA)
    else -> Warning
}

@Composable
fun VpnServer.uptimeFormattedLocalized(): String {
    val hours = uptime / 3600
    val days = hours / 24
    return when {
        hours >= 720 -> stringResource(R.string.months_format, "%.1f".format(hours / 720f))
        hours >= 168 -> stringResource(R.string.weeks_format, "%.1f".format(hours / 168f))
        hours >= 24 -> stringResource(R.string.days_format, days)
        else -> stringResource(R.string.hours_format, hours)
    }
}

@Composable
fun ServerCard(
    server: VpnServer,
    isFavorite: Boolean,
    measuredPing: Long?,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit,
    onConnect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = { if (selectionMode) onToggleSelect() else onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selectionMode && isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
            }

            CountryFlagBadge(
                countryCode = server.countryShort,
                modifier = Modifier.size(40.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.countryLong,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = protocolLabel(server.protoType),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = server.hostName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = server.ip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
            }

            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite_toggle),
                            tint = if (isFavorite) Color(0xFFF472B6) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        onClick = onConnect,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = stringResource(R.string.connect),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun protocolLabel(protoType: String): String = when (protoType) {
    "TCP" -> stringResource(R.string.ovpn_tcp)
    "BOTH" -> stringResource(R.string.ovpn_both)
    else -> stringResource(R.string.ovpn_udp)
}

// Tiny stat chips used on the detail screen
@Composable
fun StatChip(label: String, value: String, tint: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = tint
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}