package com.vpngate.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vpngate.app.R
import com.vpngate.app.SortBy

@Composable
fun FilterBar(
    countries: List<String>,
    selectedCountry: String?,
    sortBy: SortBy?,
    ascending: Boolean,
    favoritesOnly: Boolean,
    showFavoritesChip: Boolean,
    onCountrySelect: (String?) -> Unit,
    onSortChange: (SortBy) -> Unit,
    onToggleOrder: () -> Unit,
    onToggleFavorites: () -> Unit
) {
    var showCountryDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            if (showFavoritesChip) {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = onToggleFavorites,
                    label = { Text(stringResource(R.string.favorites_filter)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                )
            }

            OutlinedButton(
                onClick = { showCountryDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = selectedCountry ?: stringResource(R.string.all_countries),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SortOption(stringResource(R.string.sort_score), SortBy.SCORE, sortBy, onSortChange)
            SortOption(stringResource(R.string.sort_ping), SortBy.PING, sortBy, onSortChange)
            SortOption(stringResource(R.string.sort_speed), SortBy.SPEED, sortBy, onSortChange)
            SortOption(stringResource(R.string.sort_sessions), SortBy.SESSIONS, sortBy, onSortChange)

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onToggleOrder) {
                Icon(
                    imageVector = if (ascending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = "order",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showCountryDialog) {
        val dialogCountries = listOf<String?>(null) + countries
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text(stringResource(R.string.country_picker)) },
            text = {
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    items(dialogCountries, key = { it ?: "all" }) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCountrySelect(country)
                                    showCountryDialog = false
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCountry == country,
                                onClick = null
                            )
                            Text(
                                text = country ?: stringResource(R.string.all_countries),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SortOption(
    label: String,
    value: SortBy,
    current: SortBy?,
    onSelect: (SortBy) -> Unit
) {
    val selected = value == current
    Surface(
        onClick = { onSelect(value) },
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}