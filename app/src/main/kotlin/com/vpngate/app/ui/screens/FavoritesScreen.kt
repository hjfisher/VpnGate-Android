package com.vpngate.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vpngate.app.R
import com.vpngate.app.ServerListState
import com.vpngate.app.VpnGateViewModel
import com.vpngate.app.connection.VpnConnector
import com.vpngate.app.data.VpnServer
import com.vpngate.app.ui.components.EmptyState
import com.vpngate.app.ui.components.ServerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: ServerListState,
    vm: VpnGateViewModel,
    onServerClick: (VpnServer) -> Unit
) {
    val ctx = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_favorites)) }) }
    ) { padding ->
        val favorites = state.servers.filter { it.hostName in state.favorites }

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Filled.Favorite,
                    message = stringResource(R.string.empty_favorites)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(favorites, key = { it.hostName }) { server ->
                    ServerCard(
                        server = server,
                        isFavorite = true,
                        measuredPing = state.pingResults[server.hostName],
                        onClick = { onServerClick(server) },
                        onConnect = { VpnConnector.connect(ctx, server) },
                        onToggleFavorite = { vm.toggleFavorite(server.hostName) }
                    )
                }
            }
        }
    }
}