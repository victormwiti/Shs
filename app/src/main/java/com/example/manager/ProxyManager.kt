package com.example.manager

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.hotspot.HotspotHelper
import com.example.hotspot.HotspotState
import com.example.model.DeviceInfo
import com.example.model.NetworkInterfaceInfo
import com.example.model.TrafficLogEntry
import com.example.service.ProxyServerService
import com.example.util.DeviceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object ProxyManager {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _port = MutableStateFlow(8282)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _primaryIp = MutableStateFlow("127.0.0.1")
    val primaryIp: StateFlow<String> = _primaryIp.asStateFlow()

    private val _interfaces = MutableStateFlow<List<NetworkInterfaceInfo>>(emptyList())
    val interfaces: StateFlow<List<NetworkInterfaceInfo>> = _interfaces.asStateFlow()

    private val _activeClients = MutableStateFlow(0)
    val activeClients: StateFlow<Int> = _activeClients.asStateFlow()

    private val _totalUploaded = MutableStateFlow(0L)
    val totalUploaded: StateFlow<Long> = _totalUploaded.asStateFlow()

    private val _totalDownloaded = MutableStateFlow(0L)
    val totalDownloaded: StateFlow<Long> = _totalDownloaded.asStateFlow()

    private val _uploadSpeed = MutableStateFlow(0L)
    val uploadSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()

    private val _downloadSpeed = MutableStateFlow(0L)
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()

    private val _trafficLogs = MutableStateFlow<List<TrafficLogEntry>>(emptyList())
    val trafficLogs: StateFlow<List<TrafficLogEntry>> = _trafficLogs.asStateFlow()

    private val _hotspotState = MutableStateFlow<HotspotState>(HotspotState.Inactive)
    val hotspotState: StateFlow<HotspotState> = _hotspotState.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private var speedMonitorJob: Job? = null
    private var lastUploaded = 0L
    private var lastDownloaded = 0L
    private var hotspotHelper: HotspotHelper? = null

    fun initialize(context: Context) {
        _deviceInfo.value = DeviceHelper.getDeviceInfo(context)
        hotspotHelper = HotspotHelper(context)
        refreshNetworkInfo()
    }

    fun refreshNetworkInfo() {
        _interfaces.value = DeviceHelper.getNetworkInterfaces()
        _primaryIp.value = DeviceHelper.getPrimaryIpAddress()
    }

    fun setPort(newPort: Int) {
        if (!_isRunning.value && newPort in 1024..65535) {
            _port.value = newPort
        }
    }

    fun startServer(context: Context) {
        refreshNetworkInfo()
        val intent = Intent(context, ProxyServerService::class.java).apply {
            action = ProxyServerService.ACTION_START
            putExtra(ProxyServerService.EXTRA_PORT, _port.value)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopServer(context: Context) {
        val intent = Intent(context, ProxyServerService::class.java).apply {
            action = ProxyServerService.ACTION_STOP
        }
        context.startService(intent)
    }

    // Called by ProxyServerService
    internal fun onServerStarted() {
        _isRunning.value = true
        startSpeedMonitor()
    }

    internal fun onServerStopped() {
        _isRunning.value = false
        _activeClients.value = 0
        _uploadSpeed.value = 0L
        _downloadSpeed.value = 0L
        speedMonitorJob?.cancel()
        speedMonitorJob = null
    }

    internal fun updateStats(clients: Int, uploaded: Long, downloaded: Long) {
        _activeClients.value = clients
        _totalUploaded.value = uploaded
        _totalDownloaded.value = downloaded
    }

    internal fun addLogEntry(entry: TrafficLogEntry) {
        val current = _trafficLogs.value.toMutableList()
        if (current.size > 100) {
            current.removeAt(current.size - 1)
        }
        current.add(0, entry)
        _trafficLogs.value = current
    }

    fun clearLogs() {
        _trafficLogs.value = emptyList()
    }

    private fun startSpeedMonitor() {
        speedMonitorJob?.cancel()
        lastUploaded = _totalUploaded.value
        lastDownloaded = _totalDownloaded.value

        speedMonitorJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isRunning.value) {
                delay(1000)
                val curUp = _totalUploaded.value
                val curDown = _totalDownloaded.value
                val deltaUp = (curUp - lastUploaded).coerceAtLeast(0)
                val deltaDown = (curDown - lastDownloaded).coerceAtLeast(0)
                lastUploaded = curUp
                lastDownloaded = curDown

                _uploadSpeed.value = deltaUp
                _downloadSpeed.value = deltaDown

                // Periodically check IP in case network changed
                _primaryIp.value = DeviceHelper.getPrimaryIpAddress()
            }
        }
    }

    fun startHotspot() {
        hotspotHelper?.startHotspot { state ->
            _hotspotState.value = state
            refreshNetworkInfo()
        }
    }

    fun stopHotspot() {
        hotspotHelper?.stopHotspot { state ->
            _hotspotState.value = state
            refreshNetworkInfo()
        }
    }

    fun openTetheringSettings() {
        hotspotHelper?.openTetheringSettings()
    }
}
