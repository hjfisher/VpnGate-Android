package com.vpngate.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpngate.app.AppSettings
import com.vpngate.app.R
import com.vpngate.app.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onClearCache: () -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionHeader(Icons.Filled.Language, stringResource(R.string.language))
            ChoiceGroup(
                options = listOf(
                    Triple("system", stringResource(R.string.lang_system), null),
                    Triple("en", stringResource(R.string.lang_en), null),
                    Triple("fa", stringResource(R.string.lang_fa), null)
                ),
                selected = settings.language,
                onSelect = {
                    if (it != settings.language) {
                        onUpdate(settings.copy(language = it))
                        onLanguageChanged(it)
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader(Icons.Filled.Palette, stringResource(R.string.theme))
            ChoiceGroup(
                options = listOf(
                    Triple("system", stringResource(R.string.theme_system), null),
                    Triple("light", stringResource(R.string.theme_light), null),
                    Triple("dark", stringResource(R.string.theme_dark), null)
                ),
                selected = settings.themeMode,
                onSelect = { onUpdate(settings.copy(themeMode = it)) }
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader(Icons.Filled.Sort, stringResource(R.string.default_sort))
            ChoiceGroup(
                options = listOf(
                    Triple("score", stringResource(R.string.sort_score), null),
                    Triple("ping", stringResource(R.string.sort_ping), null),
                    Triple("speed", stringResource(R.string.sort_speed), null),
                    Triple("sessions", stringResource(R.string.sort_sessions), null)
                ),
                selected = settings.sortBy,
                onSelect = { onUpdate(settings.copy(sortBy = it)) }
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader(Icons.Filled.Sync, stringResource(R.string.auto_refresh))
            ChoiceGroup(
                options = listOf(
                    Triple("0", stringResource(R.string.auto_refresh_off), null),
                    Triple("10", stringResource(R.string.auto_refresh_10m), null),
                    Triple("30", stringResource(R.string.auto_refresh_30m), null),
                    Triple("60", stringResource(R.string.auto_refresh_1h), null)
                ),
                selected = settings.autoRefreshMinutes.toString(),
                onSelect = { onUpdate(settings.copy(autoRefreshMinutes = it.toInt())) }
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader(Icons.Filled.Security, stringResource(R.string.use_mirror))
            SwitchRow(
                title = stringResource(R.string.use_mirror),
                desc = stringResource(R.string.use_mirror_desc),
                checked = settings.useMirror,
                onCheckedChange = { onUpdate(settings.copy(useMirror = it)) }
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.clear_cache),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.app_name) + "  •  " +
                        stringResource(R.string.version_label) + " 1.0.0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(stringResource(R.string.clear_cache)) },
                text = { Text(stringResource(R.string.clear_cache_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        onClearCache()
                        showClearDialog = false
                    }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ChoiceGroup(
    options: List<Triple<String, String, Nothing?>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        options.forEach { (value, label, _) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == value,
                    onClick = { onSelect(value) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}