package com.example.model

data class TrafficLogEntry(
    val id: Long,
    val timestamp: Long,
    val clientAddress: String,
    val targetHost: String,
    val protocol: String, // "HTTPS CONNECT", "HTTP", "SOCKS5", "PAC"
    val bytesTransferred: Long,
    val status: String, // "Active", "Closed", "Error"
    val durationMs: Long = 0
)

data class NetworkInterfaceInfo(
    val name: String,
    val displayName: String,
    val ipv4: String?,
    val ipv6: String?,
    val isUp: Boolean,
    val isLoopback: Boolean
)

data class DeviceInfo(
    val isChromebook: Boolean,
    val model: String,
    val brand: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val arcVersion: String?,
    val hasWifi: Boolean,
    val hasWifiDirect: Boolean,
    val hasTelephony: Boolean,
    val notes: String
)

data class ProxyTestResult(
    val success: Boolean,
    val latencyMs: Long,
    val message: String,
    val responseCode: Int? = null,
    val publicIp: String? = null
)
