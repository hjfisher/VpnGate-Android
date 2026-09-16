package com.vpngate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vpngate.app.R
import com.vpngate.app.ServerListState
import com.vpngate.app.VpnGateViewModel
import com.vpngate.app.connection.VpnConnector
import com.vpngate.app.data.VpnServer
import com.vpngate.app.ui.components.EmptyState
import com.vpngate.app.ui.components.FilterBar
import com.vpngate.app.ui.components.OfflineBanner
import com.vpngate.app.ui.components.ServerCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    state: ServerListState,
    vm: VpnGateViewModel,
    onServerClick: (VpnServer) -> Unit,
    hardError: Boolean = false,
    showFavoritesChip: Boolean = false
) {
    val ctx = LocalContext.current
    val refreshLabel = stringResource(R.string.refresh)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.selectionMode) {
                        Text(stringResource(R.string.selected_count, state.selectedHosts.size))
                    } else {
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    if (state.selectionMode) {
                        IconButton(onClick = {
                            vm.setSelectionMode(false)
                            vm.clearSelection()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.exit_selection))
                        }
                        IconButton(onClick = { vm.selectAll(state.servers.map { it.hostName }) }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_selected))
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val outcome = vm.exportSelected()
                                val msg = if (outcome.error == null) {
                                    ctx.getString(R.string.export_done, outcome.count, outcome.folder)
                                } else {
                                    ctx.getString(R.string.export_failed)
                                }
                                snackbar.showSnackbar(msg)
                            }
                        }) {
                            Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.export_ovpn))
                        }
                    } else {
                        IconButton(onClick = { vm.setSelectionMode(true) }) {
                            Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.selection_mode))
                        }
                        IconButton(onClick = vm::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = refreshLabel)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.isRefreshing && !state.selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = vm::refresh,
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    text = { Text(refreshLabel) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val hasServers = state.servers.isNotEmpty()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isRefreshing && state.servers.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                hardError && state.servers.isEmpty() -> {
                    FullErrorScreen(
                        message = stringResource(R.string.no_internet),
                        onRetry = vm::refresh
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.offline && hasServers && !state.isRefreshing) {
                            OfflineBanner(
                                message = stringResource(R.string.refresh_failed),
                                onRetry = vm::refresh
                            )
                        }

                        var query by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                vm.setSearch(it)
                            },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        FilterBar(
                            countries = state.countries,
                            selectedCountry = state.filters.country,
                            sortBy = state.filters.sortBy,
                            ascending = state.filters.ascending,
                            favoritesOnly = state.filters.favoritesOnly,
                            showFavoritesChip = showFavoritesChip,
                            onCountrySelect = vm::setCountry,
                            onSortChange = { vm.setSort(it) },
                            onToggleOrder = vm::toggleAscending,
                            onToggleFavorites = vm::toggleFavoritesOnly
                        )

                        Text(
                            text = "${stringResource(R.string.servers_count, state.servers.size)}  •  " +
                                stringResource(R.string.last_update, state.lastUpdateText),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        if (state.servers.isEmpty()) {
                            EmptyState(
                                icon = if (state.filters.favoritesOnly) Icons.Filled.List else Icons.Filled.Public,
                                message = if (state.filters.favoritesOnly)
                                    stringResource(R.string.empty_favorites)
                                else stringResource(R.string.empty_list)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(
                                    start = 12.dp, end = 12.dp, bottom = 90.dp, top = 2.dp
                                )
                            ) {
                                items(state.servers, key = { it.hostName }) { server ->
                                    ServerCard(
                                        server = server,
                                        isFavorite = server.hostName in state.favorites,
                                        measuredPing = state.pingResults[server.hostName],
                                        selectionMode = state.selectionMode,
                                        isSelected = server.hostName in state.selectedHosts,
                                        onToggleSelect = { vm.toggleSelect(server.hostName) },
                                        onClick = { onServerClick(server) },
                                        onConnect = {
                                            VpnConnector.connect(ctx, server)
                                        },
                                        onToggleFavorite = { vm.toggleFavorite(server.hostName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_selected)) },
            text = { Text(stringResource(R.string.delete_selected_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSelected()
                    showDeleteDialog = false
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
fun FullErrorScreen(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Surface(
            onClick = onRetry,
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.retry),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}