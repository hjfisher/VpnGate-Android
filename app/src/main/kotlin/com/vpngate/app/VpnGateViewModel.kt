package com.vpngate.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vpngate.app.connection.ExportOutcome
import com.vpngate.app.connection.OvnExporter
import com.vpngate.app.data.VpnGateApi
import com.vpngate.app.data.VpnGateParser
import com.vpngate.app.data.VpnGateRepository
import com.vpngate.app.data.VpnServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortBy { SCORE, PING, SPEED, SESSIONS }

data class ServerFilters(
    val search: String = "",
    val country: String? = null,
    val sortBy: SortBy? = null,          // null → use settings default
    val ascending: Boolean = false,
    val favoritesOnly: Boolean = false,
    val minScore: Long = 0L
)

data class ServerListState(
    val servers: List<VpnServer>,
    val filters: ServerFilters,
    val countries: List<String>,
    val isRefreshing: Boolean,
    val lastUpdateText: String,
    val offline: Boolean,
    val favorites: Set<String>,
    val pingResults: Map<String, Long>,
    val selectionMode: Boolean,
    val selectedHosts: Set<String>
)

class VpnGateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VpnGateRepository(application)

    private val _filters = MutableStateFlow(ServerFilters())
    private val _allServers = MutableStateFlow<List<VpnServer>>(emptyList())
    private val _countries = MutableStateFlow<List<String>>(emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    private val _lastUpdate = MutableStateFlow("")
    private val _offline = MutableStateFlow(false)
    private val _hardError = MutableStateFlow(false)
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    private val _pingResults = MutableStateFlow<Map<String, Long>>(emptyMap())

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    private val _selectedHosts = MutableStateFlow<Set<String>>(emptySet())

    private val _uiState = MutableStateFlow(
        ServerListState(
            emptyList(), ServerFilters(), emptyList(), false, "", false,
            emptySet(), emptyMap(), false, emptySet()
        )
    )
    val uiState: StateFlow<ServerListState> = _uiState.asStateFlow()

    val hardError: StateFlow<Boolean> = _hardError.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            _settings.value = repository.settingsFlow().first()
            watchSettings()

            // Show every saved config instantly so the UI is never blank
            val saved = repository.loadServers()
            if (saved.isNotEmpty()) {
                _allServers.value = saved
                _countries.value = buildCountries(saved)
                _lastUpdate.value = repository.lastFetchTime()?.let { formatTime(it) } ?: ""
                _offline.value = true
            }
            recompute()

            // Network refresh in background
            refreshInternal(showSpinner = saved.isEmpty())
        }
    }

    private fun watchSettings() {
        viewModelScope.launch {
            repository.settingsFlow().collect { s ->
                val prev = _settings.value
                _settings.value = s
                recompute()
                if (prev.autoRefreshMinutes != s.autoRefreshMinutes) {
                    restartAutoRefresh(s.autoRefreshMinutes)
                }
            }
        }
    }

    private fun restartAutoRefresh(minutes: Int) {
        autoRefreshJob?.cancel()
        if (minutes <= 0) return
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(minutes * 60_000L)
                refresh()
            }
        }
    }

    fun updateSettings(new: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(new)
            _settings.value = new
            recompute()
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshInternal(showSpinner = true) }
    }

    private suspend fun refreshInternal(showSpinner: Boolean) {
        if (_isRefreshing.value) return
        _isRefreshing.value = showSpinner
        _hardError.value = false
        recompute()
        try {
            val csv = withContext(Dispatchers.IO) {
                VpnGateApi.fetchRawCsv(useMirror = _settings.value.useMirror)
            }
            val fresh = VpnGateParser.parse(csv)
            if (fresh.isNotEmpty()) {
                // Merge: keep previously saved configs, only append new unique ones.
                val merged = repository.mergeServers(fresh)
                _allServers.value = merged
                _countries.value = buildCountries(merged)
                _lastUpdate.value = formatTime(System.currentTimeMillis())
                _offline.value = false
                _hardError.value = false
            } else {
                _offline.value = true
            }
        } catch (e: Exception) {
            if (_allServers.value.isEmpty()) {
                _hardError.value = true
            } else {
                _offline.value = true
            }
        } finally {
            _isRefreshing.value = false
            recompute()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _allServers.value = emptyList()
            _countries.value = emptyList()
            _favorites.value = emptySet()
            _lastUpdate.value = ""
            _selectionMode.value = false
            _selectedHosts.value = emptySet()
            recompute()
        }
    }

    fun setSelectionMode(on: Boolean) {
        _selectionMode.value = on
        if (!on) _selectedHosts.value = emptySet()
        recompute()
    }

    fun selectAll(hostNames: List<String>) {
        _selectedHosts.value = hostNames.toSet()
        recompute()
    }

    fun toggleSelect(hostName: String) {
        val cur = _selectedHosts.value
        _selectedHosts.value = if (hostName in cur) cur - hostName else cur + hostName
        recompute()
    }

    fun clearSelection() {
        _selectedHosts.value = emptySet()
        recompute()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ips = _selectedHosts.value.mapNotNull { findServer(it)?.ip }.toSet()
            if (ips.isEmpty()) return@launch
            val remaining = repository.deleteServers(ips)
            _allServers.value = remaining
            _countries.value = buildCountries(remaining)
            _selectedHosts.value = emptySet()
            _selectionMode.value = false
            recompute()
        }
    }

    fun deleteServer(hostName: String) {
        viewModelScope.launch {
            val server = _allServers.value.firstOrNull { it.hostName == hostName } ?: return@launch
            val remaining = repository.deleteServers(setOf(server.ip))
            _allServers.value = remaining
            _countries.value = buildCountries(remaining)
            _selectedHosts.value = _selectedHosts.value - hostName
            recompute()
        }
    }

    suspend fun exportSelected(): ExportOutcome {
        val selected = _selectedHosts.value.mapNotNull { findServer(it) }
        if (selected.isEmpty()) {
            return ExportOutcome(0, "", "no selection")
        }
        return OvnExporter.export(getApplication(), selected)
    }

    fun setSearch(query: String) { _filters.value = _filters.value.copy(search = query.trim()); recompute() }
    fun setCountry(country: String?) { _filters.value = _filters.value.copy(country = country); recompute() }
    fun setSort(sortBy: SortBy?) { _filters.value = _filters.value.copy(sortBy = sortBy); recompute() }
    fun toggleAscending() {
        val f = _filters.value
        _filters.value = f.copy(ascending = !f.ascending)
        recompute()
    }

    fun toggleFavoritesOnly() {
        val f = _filters.value
        _filters.value = f.copy(favoritesOnly = !f.favoritesOnly)
        recompute()
    }

    fun toggleFavorite(hostName: String) {
        val cur = _favorites.value
        _favorites.value = if (hostName in cur) cur - hostName else cur + hostName
        recompute()
    }

    fun findServer(hostName: String): VpnServer? =
        _allServers.value.firstOrNull { it.hostName == hostName }

    fun recordPing(hostName: String, ms: Long) {
        val m = _pingResults.value.toMutableMap()
        m[hostName] = ms
        _pingResults.value = m
        recompute()
    }

    private fun recompute() {
        _uiState.value = ServerListState(
            servers = filterAndSort(
                _allServers.value,
                _filters.value,
                _favorites.value,
                effectiveSortBy()
            ),
            filters = _filters.value,
            countries = _countries.value,
            isRefreshing = _isRefreshing.value,
            lastUpdateText = _lastUpdate.value,
            offline = _offline.value,
            favorites = _favorites.value,
            pingResults = _pingResults.value,
            selectionMode = _selectionMode.value,
            selectedHosts = _selectedHosts.value
        )
    }

    private fun effectiveSortBy(): SortBy {
        _filters.value.sortBy?.let { return it }
        return when (_settings.value.sortBy) {
            "ping" -> SortBy.PING
            "speed" -> SortBy.SPEED
            "sessions" -> SortBy.SESSIONS
            else -> SortBy.SCORE
        }
    }

    private fun buildCountries(servers: List<VpnServer>): List<String> =
        servers.map { it.countryLong }.distinct().sorted()

    private fun filterAndSort(
        servers: List<VpnServer>,
        filters: ServerFilters,
        favorites: Set<String>,
        sort: SortBy
    ): List<VpnServer> {
        var result = servers
        if (filters.favoritesOnly) result = result.filter { it.hostName in favorites }
        if (filters.country != null) result = result.filter { it.countryLong == filters.country }
        if (filters.minScore > 0) result = result.filter { it.score >= filters.minScore }
        if (filters.search.isNotEmpty()) {
            val q = filters.search
            result = result.filter {
                it.hostName.contains(q, true) || it.ip.contains(q, true) ||
                    it.countryLong.contains(q, true) || it.countryShort.contains(q, true) ||
                    it.operator.contains(q, true)
            }
        }
        val c = when (sort) {
            SortBy.SCORE -> compareBy<VpnServer> { it.score }
            SortBy.PING -> compareBy<VpnServer> { it.ping }
            SortBy.SPEED -> compareBy<VpnServer> { it.speed }
            SortBy.SESSIONS -> compareBy<VpnServer> { it.numVpnSessions }
        }
        result = result.sortedWith(if (filters.ascending) c else c.reversed())
        return result
    }

    private fun formatTime(ms: Long): String {
        val diff = System.currentTimeMillis() - ms
        val minutes = diff / 60_000
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes} min"
            minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
            else -> "${minutes / 1440}d"
        }
    }
}