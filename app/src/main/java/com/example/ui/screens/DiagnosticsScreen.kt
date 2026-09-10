package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manager.ProxyManager
import com.example.model.NetworkInterfaceInfo
import com.example.ui.theme.NetAmberWarning
import com.example.ui.theme.NetCyanPrimary
import com.example.ui.theme.NetGreenActive
import com.example.ui.theme.TextSecondaryDark

@Composable
fun DiagnosticsScreen(
    modifier: Modifier = Modifier
) {
    val deviceInfo by ProxyManager.deviceInfo.collectAsState()
    val interfaces by ProxyManager.interfaces.collectAsState()
    val primaryIp by ProxyManager.primaryIp.collectAsState()
    val port by ProxyManager.port.collectAsState()
    val isRunning by ProxyManager.isRunning.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (deviceInfo?.isChromebook == true) Icons.Default.LaptopChromebook else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = NetCyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Device & Platform Environment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    DiagRow(label = "Platform Type", value = if (deviceInfo?.isChromebook == true) "ChromeOS (ARC Container)" else "Native Android Device")
                    DiagRow(label = "Device Model", value = "${deviceInfo?.manufacturer} ${deviceInfo?.model}")
                    DiagRow(label = "Android Version", value = "${deviceInfo?.androidVersion} (API ${deviceInfo?.sdkInt})")
                    DiagRow(label = "Active Proxy Port", value = "$port (HTTP & SOCKS5)")
                    DiagRow(label = "Primary Resolved IP", value = primaryIp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = if (deviceInfo?.isChromebook == true) NetAmberWarning.copy(alpha = 0.12f)
                        else NetGreenActive.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (deviceInfo?.isChromebook == true) NetAmberWarning else NetGreenActive,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = deviceInfo?.notes ?: "Running safely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Network Interfaces Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Network Interfaces (${interfaces.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = { ProxyManager.refreshNetworkInfo() },
                    modifier = Modifier.testTag("diag_refresh_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh", fontSize = 12.sp)
                }
            }
        }

        items(interfaces, key = { it.name }) { iface ->
            NetworkInterfaceCard(iface = iface, isPrimary = iface.ipv4 == primaryIp)
        }

        // Chromebook Crash Fix Explanation
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NetCyanPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Why NetShare Works on Chromebook",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Safe Feature Probing: Traditional tethering apps crash on Chromebook because they attempt to initialize telephony or low-level Wi-Fi Direct drivers. NetShare declares all hardware features optional and never makes unsafe hardware calls.\n\n" +
                                "2. Dual Protocol Proxy: Supports both HTTP CONNECT (HTTPS tunneling) and SOCKS5 on port 8282. ChromeOS, Chrome browser, and Android container apps can connect seamlessly.\n\n" +
                                "3. Low Android Version Support: Fully compatible from Android 7.0 (API 24) upwards, covering all legacy and modern Chromebook ARC versions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun NetworkInterfaceCard(iface: NetworkInterfaceInfo, isPrimary: Boolean) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPrimary) 1.5.dp else 0.5.dp,
                color = if (isPrimary) NetCyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SettingsEthernet,
                        contentDescription = null,
                        tint = if (isPrimary) NetCyanPrimary else TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = iface.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = NetCyanPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PRIMARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NetCyanPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Surface(
                    color = if (iface.isUp) NetGreenActive.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (iface.isUp) "UP" else "DOWN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (iface.isUp) NetGreenActive else Color.Gray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (iface.ipv4 != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "IPv4: ", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                    Text(
                        text = iface.ipv4,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (iface.ipv6 != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "IPv6: ", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                    Text(
                        text = iface.ipv6,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
