package com.rtwifianalyzer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtwifianalyzer.app.data.DeviceDatabase
import com.rtwifianalyzer.app.data.DeviceEntity
import com.rtwifianalyzer.app.data.WifiRepository
import com.rtwifianalyzer.app.domain.AccessPoint
import com.rtwifianalyzer.app.domain.NetworkDevice
import com.rtwifianalyzer.app.domain.WifiSnapshot
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyzerState(val wifi: WifiSnapshot = WifiSnapshot(), val points: List<AccessPoint> = emptyList(), val history: List<Int> = emptyList(), val loading: Boolean = false, val message: String? = null, val publicIpLoading: Boolean = false)

@HiltViewModel
class AnalyzerViewModel @Inject constructor(private val repo: WifiRepository, private val db: DeviceDatabase) : ViewModel() {
    private val _state = MutableStateFlow(AnalyzerState())
    val state: StateFlow<AnalyzerState> = _state.asStateFlow()
    val devices = db.deviceDao().observeAll().map { rows -> rows.map { NetworkDevice(it.ip, it.mac, it.name, it.manufacturer, it.online, it.lastSeen) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rootAvailable = MutableStateFlow(false)
    init { refreshConnection(); viewModelScope.launch { rootAvailable.value = runCatching { Shell.getShell().isRoot }.getOrDefault(false) } }
    fun refreshConnection() {
        val w = repo.snapshot()
        val needsPublicIp = _state.value.wifi.publicIp == "—"
        _state.update { previous -> previous.copy(wifi = w.copy(publicIp = previous.wifi.publicIp), history = (previous.history + w.rssi).takeLast(60)) }
        if (needsPublicIp) viewModelScope.launch {
            _state.update { it.copy(publicIpLoading = true) }
            val ip = repo.publicIp()
            _state.update { it.copy(wifi = it.wifi.copy(publicIp = ip), publicIpLoading = false) }
        }
    }
    fun scan() { viewModelScope.launch { _state.update { it.copy(loading = true, message = null) }; val allowed = repo.requestScan(); _state.update { it.copy(points = repo.scan(), loading = false, message = if (allowed) null else "Scan request throttled by Android; showing last available results.") } } }
    fun devices() { viewModelScope.launch { _state.update { it.copy(loading = true) }; val found = repo.arpDevices(); found.forEach { d -> db.deviceDao().upsert(DeviceEntity(d.mac, d.ip, d.name, d.manufacturer, System.currentTimeMillis(), System.currentTimeMillis(), true)) }; _state.update { it.copy(loading = false, message = if (found.isEmpty()) "No ARP entries available. Connect to Wi-Fi and try again." else null) } } }
}
