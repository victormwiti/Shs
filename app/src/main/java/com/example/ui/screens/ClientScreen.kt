package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.ProxyClientTester
import com.example.manager.ProxyManager
import com.example.model.ProxyTestResult
import com.example.ui.components.DeviceHeaderBadge
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.NetAmberWarning
import com.example.ui.theme.NetCyanPrimary
import com.example.ui.theme.NetGreenActive
import com.example.ui.theme.NetRedError
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

@Composable
fun ClientScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deviceInfo by ProxyManager.deviceInfo.collectAsState()
    val defaultIp by ProxyManager.primaryIp.collectAsState()
    val defaultPort by ProxyManager.port.collectAsState()
    val isRunning by ProxyManager.isRunning.collectAsState()

    var targetHost by remember { mutableStateOf("192.168.49.1") }
    var targetPort by remember { mutableStateOf("8282") }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ProxyTestResult?>(null) }

    // Synchronize host default if local IP is valid
    remember(defaultIp) {
        if (defaultIp != "127.0.0.1" && targetHost == "192.168.49.1") {
            targetHost = defaultIp
        }
    }

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
                primaryIp = defaultIp,
                isRunning = isRunning
            )
        }

        // Dedicated Chromebook Guide Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        NetCyanPrimary.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NetCyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LaptopChromebook,
                                contentDescription = null,
                                tint = NetCyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Chromebook Connection Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Zero-crash tethering guide for ChromeOS & Android",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Because ChromeOS isolates Wi-Fi hardware inside ARC, hotspot apps crash when trying to access Wi-Fi Direct directly. NetShare solves this by routing via high-speed HTTP & SOCKS proxy without touching restricted hardware!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Live Connection Tester Card
        item {
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
                        Text(
                            text = "Test Proxy Connection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = NetCyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Verify that this device can communicate with the NetShare host proxy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = targetHost,
                            onValueChange = { targetHost = it.trim() },
                            label = { Text("Host IP") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("client_host_input")
                        )

                        OutlinedTextField(
                            value = targetPort,
                            onValueChange = { targetPort = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("client_port_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val portNum = targetPort.toIntOrNull() ?: 8282
                            isTesting = true
                            testResult = null
                            scope.launch {
                                val res = ProxyClientTester.testProxyConnection(targetHost, portNum)
                                testResult = res
                                isTesting = false
                            }
                        },
                        enabled = !isTesting && targetHost.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_proxy_button")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...")
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Proxy Connection")
                        }
                    }

                    // Test Results Display
                    testResult?.let { result ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (result.success) NetGreenActive.copy(alpha = 0.12f)
                            else NetRedError.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (result.success) NetGreenActive else NetRedError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (result.success) "SUCCESS • ${result.latencyMs}ms" else "FAILED",
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.success) NetGreenActive else NetRedError
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Step-by-Step ChromeOS Instructions
        item {
            Text(
                text = "How to Configure Chromebook (ChromeOS)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }

        item {
            ChromeOsStepCard(
                step = "1",
                title = "Connect to Phone's Wi-Fi",
                description = "On your Chromebook, open Wi-Fi menu and connect to your Android phone's Wi-Fi network (or Wi-Fi Hotspot)."
            )
        }

        item {
            ChromeOsStepCard(
                step = "2",
                title = "Open ChromeOS Wi-Fi Settings",
                description = "Click the bottom-right system tray (clock) on your Chromebook, click the Settings gear ⚙️, go to 'Network' → 'Wi-Fi', then click the network you just connected to."
            )
        }

        item {
            ChromeOsStepCard(
                step = "3",
                title = "Configure Proxy Settings",
                description = "Scroll down and expand the 'Proxy' dropdown section. Select 'Manual proxy configuration'.",
                actionContent = {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyToClipboard(context, "Host IP", targetHost) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy IP: $targetHost", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { copyToClipboard(context, "Port", targetPort) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Port: $targetPort", fontSize = 11.sp)
                            }
                        }
                    }
                }
            )
        }

        item {
            ChromeOsStepCard(
                step = "4",
                title = "Alternative: Automatic PAC Proxy",
                description = "Alternatively in ChromeOS, select 'Automatic proxy configuration' and paste the PAC URL below:",
                actionContent = {
                    val pacUrl = "http://$targetHost:$targetPort/proxy.pac"
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "PAC URL", pacUrl) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy PAC: $pacUrl", fontSize = 11.sp)
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ChromeOsStepCard(
    step: String,
    title: String,
    description: String,
    actionContent: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NetCyanPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A0F1D),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                actionContent?.invoke()
            }
        }
    }
}
