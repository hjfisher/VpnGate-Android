package com.vpngate.app

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vpngate.app.data.VpnGateRepository
import com.vpngate.app.data.VpnServer
import com.vpngate.app.ui.screens.FavoritesScreen
import com.vpngate.app.ui.screens.ServerDetailScreen
import com.vpngate.app.ui.screens.ServerListScreen
import com.vpngate.app.ui.screens.SettingsScreen
import com.vpngate.app.ui.theme.VpnGateTheme
import com.vpngate.app.util.LocaleManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking {
            try {
                VpnGateRepository(newBase.applicationContext).settingsFlow().first().language
            } catch (e: Exception) {
                "system"
            }
        }
        super.attachBaseContext(wrapLocale(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: VpnGateViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val darkTheme = rememberDarkTheme(settings.themeMode)

            VpnGateTheme(darkTheme = darkTheme) {
                AppNavigator(vm = vm)
            }
        }
    }

    @Composable
    private fun rememberDarkTheme(mode: String): Boolean {
        val systemDark = LocalContext.current.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return when (mode) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
    }

    private fun wrapLocale(base: Context, language: String): Context {
        if (language == "system" || language == Locale.getDefault().language) return base
        val locale = if (language == "fa") Locale("fa") else Locale("en")
        return LocaleContextWrapper(base, locale)
    }

    /** Forces the app resources to use the selected locale regardless of system. */
    class LocaleContextWrapper(
        base: Context,
        private val locale: Locale
    ) : android.content.ContextWrapper(base) {
        override fun getApplicationContext(): Context = this

        private fun localizedResources(): Resources {
            val config = Configuration(baseContext.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            val res = baseContext.resources
            res.updateConfiguration(config, res.displayMetrics)
            return res
        }

        override fun getResources(): Resources = localizedResources()
    }
}

@Composable
private fun AppNavigator(vm: VpnGateViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var selectedServer by remember { mutableStateOf<VpnServer?>(null) }

    if (selectedServer != null) {
        val server = selectedServer!!
        ServerDetailScreen(
            server = server,
            isFavorite = server.hostName in state.favorites,
            measuredPing = state.pingResults[server.hostName],
            onBack = { selectedServer = null },
            onToggleFavorite = { vm.toggleFavorite(server.hostName) },
            onRecordPing = vm::recordPing,
            onDelete = {
                vm.deleteServer(server.hostName)
                selectedServer = null
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Public, contentDescription = null) },
                    label = { Text(stringResource(com.vpngate.app.R.string.tab_servers)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    label = { Text(stringResource(com.vpngate.app.R.string.tab_favorites)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(com.vpngate.app.R.string.tab_settings)) }
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier.padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> ServerListScreen(
                    state = state,
                    vm = vm,
                    onServerClick = { selectedServer = it },
                    hardError = vm.hardError.collectAsStateWithLifecycle().value
                )
                1 -> FavoritesScreen(
                    state = state,
                    vm = vm,
                    onServerClick = { selectedServer = it }
                )
                else -> SettingsScreen(
                    settings = vm.settings.collectAsStateWithLifecycle().value,
                    onUpdate = vm::updateSettings,
                    onClearCache = vm::clearCache,
                    onLanguageChanged = { lang ->
                        LocaleManager.apply(context, lang)
                        (context as? AppCompatActivity)?.recreate()
                    }
                )
            }
        }
    }
}