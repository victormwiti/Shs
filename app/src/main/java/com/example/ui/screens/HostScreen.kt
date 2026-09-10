package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hotspot.HotspotState
import com.example.manager.ProxyManager
import com.example.model.TrafficLogEntry
import com.example.ui.components.CopyableInfoCard
import com.example.ui.components.DeviceHeaderBadge
import com.example.ui.theme.NetAmberWarning
import com.example.ui.theme.NetCardBorderDark
import com.example.ui.theme.NetCyanPrimary
import com.example.ui.theme.NetGreenActive
import com.example.ui.theme.NetRedError
import com.example.ui.theme.TextSecondaryDark
import com.example.util.DeviceHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HostScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRunning by ProxyManager.isRunning.collectAsState()
    val port by ProxyManager.port.collectAsState()
    val primaryIp by ProxyManager.primaryIp.collectAsState()
    val activeClients by ProxyManager.activeClients.collectAsState()
    val totalUploaded by ProxyManager.totalUploaded.collectAsState()
    val totalDownloaded by ProxyManager.totalDownloaded.collectAsState()
    val uploadSpeed by ProxyManager.uploadSpeed.collectAsState()
    val downloadSpeed by ProxyManager.downloadSpeed.collectAsState()
    val trafficLogs by ProxyManager.trafficLogs.collectAsState()
    val hotspotState by ProxyManager.hotspotState.collectAsState()
    val deviceInfo by ProxyManager.deviceInfo.collectAsState()

    var showPortDialog by remember { mutableStateOf(false) }
    var portInput by remember { mutableStateOf(port.toString()) }
    var showShareModal by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            DeviceHeaderBadge(
                isChromebook = deviceInfo?.isChromebook == true,
                primaryIp = primaryIp,
                isRunning = isRunning
            )
        }

        // Hero Power Button & Status Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = if (isRunning) listOf(NetCyanPrimary, NetGreenActive)
                            else listOf(MaterialTheme.colorScheme.outline, Color.Transparent)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing circular power button
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .then(if (isRunning) Modifier.scale(pulseScale) else Modifier)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) Brush.radialGradient(
                                    listOf(NetGreenActive.copy(alpha = 0.3f), Color.Transparent)
                                )
                                else Brush.radialGradient(
                                    listOf(Color.Gray.copy(alpha = 0.1f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = {
                                if (isRunning) {
                                    ProxyManager.stopServer(context)
                                } else {
                                    ProxyManager.startServer(context)
                                }
                            },
                            shape = CircleShape,
                            color = if (isRunning) NetGreenActive else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (isRunning) 10.dp else 2.dp,
                            modifier = Modifier
                                .size(76.dp)
                                .testTag("proxy_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = if (isRunning) "Stop Proxy" else "Start Proxy",
                                    tint = if (isRunning) Color(0xFF0A0F1D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isRunning) "PROXY SERVER ONLINE" else "SERVER STOPPED",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) NetGreenActive else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRunning) "Ready for Chromebook & devices on $primaryIp:$port"
                        else "Tap above to start sharing connection via local proxy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { ProxyManager.refreshNetworkInfo() },
                            modifier = Modifier.weight(1f).testTag("refresh_ips_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh IP")
                        }

                        Button(
                            onClick = { showShareModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).testTag("share_config_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Config")
                        }
                    }
                }
            }
        }

        // Live Network Metrics Dashboard
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Bandwidth & Traffic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = NetCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$activeClients Clients",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NetCyanPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Download Speed
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = NetGreenActive,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DeviceHelper.formatSpeed(downloadSpeed),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NetGreenActive
                                )
                                Text(
                                    text = "Total: ${DeviceHelper.formatBytes(totalDownloaded)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Upload Speed
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = NetCyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DeviceHelper.formatSpeed(uploadSpeed),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NetCyanPrimary
                                )
                                Text(
                                    text = "Total: ${DeviceHelper.formatBytes(totalUploaded)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Connection Details Section
        item {
            Text(
                text = "Proxy Credentials for Chromebook / Clients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        item {
            CopyableInfoCard(
                title = "Host IP Address",
                value = primaryIp,
                label = "Enter this in ChromeOS Wi-Fi manual proxy 'Host' field",
                icon = Icons.Default.Dns
            )
        }

        item {
            CopyableInfoCard(
                title = "Proxy Port",
                value = port.toString(),
                label = "Supports HTTP CONNECT (HTTPS) and SOCKS5 (tap to change)",
                icon = Icons.Default.Language,
                onValueClick = {
                    if (!isRunning) {
                        portInput = port.toString()
                        showPortDialog = true
                    }
                }
            )
        }

        item {
            CopyableInfoCard(
                title = "Automatic PAC URL",
                value = "http://$primaryIp:$port/proxy.pac",
                label = "Or select 'Automatic proxy configuration' in Chromebook",
                icon = Icons.Default.Code
            )
        }

        // Hotspot Control Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = NetCyanPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wi-Fi Hotspot Sharing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (val state = hotspotState) {
                        is HotspotState.Active -> {
                            Surface(
                                color = NetGreenActive.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Hotspot Active: ${state.ssid}", fontWeight = FontWeight.Bold, color = NetGreenActive)
                                    if (state.passphrase != null) {
                                        Text("Password: ${state.passphrase}", fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { ProxyManager.stopHotspot() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Stop Hotspot")
                            }
                        }
                        is HotspotState.Starting -> {
                            Text("Starting local hotspot...", color = NetCyanPrimary)
                        }
                        is HotspotState.Failed -> {
                            Text("Notice: ${state.reason}", color = NetAmberWarning, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { ProxyManager.openTetheringSettings() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Android Hotspot Settings")
                            }
                        }
                        is HotspotState.ChromebookNotice -> {
                            Surface(
                                color = NetAmberWarning.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Chromebook Compatibility Notice", fontWeight = FontWeight.Bold, color = NetAmberWarning)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        HotspotState.Inactive -> {
                            Text(
                                text = if (deviceInfo?.isChromebook == true)
                                    "On Chromebook, connect to your Android phone's Wi-Fi network."
                                else
                                    "Start local Wi-Fi hotspot or connect Chromebook to the same Wi-Fi network as this phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (deviceInfo?.isChromebook != true) {
                                    Button(
                                        onClick = { ProxyManager.startHotspot() },
                                        modifier = Modifier.weight(1f).testTag("start_hotspot_button")
                                    ) {
                                        Text("Start Hotspot")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { ProxyManager.openTetheringSettings() },
                                    modifier = Modifier.weight(1f).testTag("open_hotspot_settings_button")
                                ) {
                                    Text("Hotspot Settings")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Traffic Connection Log
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Connection Log (${trafficLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (trafficLogs.isNotEmpty()) {
                    IconButton(onClick = { ProxyManager.clearLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = NetCyanPrimary)
                    }
                }
            }
        }

        if (trafficLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRunning) "Waiting for connections from Chromebook..." else "Start server to view real-time traffic",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(trafficLogs, key = { it.id }) { entry ->
                TrafficLogItem(entry = entry)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Port Configuration Dialog
    if (showPortDialog) {
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = { Text("Change Proxy Port") },
            text = {
                Column {
                    Text("Select a port between 1024 and 65535 (default is 8282):", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("port_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = portInput.toIntOrNull()
                        if (parsed != null && parsed in 1024..65535) {
                            ProxyManager.setPort(parsed)
                            showPortDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Share Configuration Modal
    if (showShareModal) {
        val shareText = """
            NetShare Hotspot Configuration:
            Host IP: $primaryIp
            Proxy Port: $port
            PAC URL: http://$primaryIp:$port/proxy.pac
            
            Chromebook Setup:
            1. Connect to Wi-Fi.
            2. Settings > Network > Wi-Fi > Proxy > Manual proxy.
            3. Enter Host: $primaryIp and Port: $port.
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showShareModal = false },
            title = { Text("Share Proxy Configuration") },
            text = {
                Column {
                    Text(
                        text = "Share connection details to your Chromebook or other devices:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = shareText,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share NetShare Config")
                        context.startActivity(shareIntent)
                        showShareModal = false
                    }
                ) {
                    Text("Send via Share Sheet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TrafficLogItem(entry: TrafficLogEntry) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = timeFormatter.format(Date(entry.timestamp))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Protocol badge
                    Surface(
                        color = when (entry.protocol) {
                            "HTTPS CONNECT" -> NetCyanPrimary.copy(alpha = 0.15f)
                            "SOCKS5" -> NetGreenActive.copy(alpha = 0.15f)
                            else -> NetAmberWarning.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = entry.protocol,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (entry.protocol) {
                                "HTTPS CONNECT" -> NetCyanPrimary
                                "SOCKS5" -> NetGreenActive
                                else -> NetAmberWarning
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = entry.targetHost,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Text(
                    text = "Client: ${entry.clientAddress} • ${entry.durationMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DeviceHelper.formatBytes(entry.bytesTransferred),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.status == "Completed" || entry.status == "Active") NetGreenActive else NetRedError,
                    fontSize = 10.sp
                )
            }
        }
    }
}
