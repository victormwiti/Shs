package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.model.DeviceInfo
import com.example.model.NetworkInterfaceInfo
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object DeviceHelper {

    /**
     * Accurately detects if the app is running in the ChromeOS Android runtime (ARC/ARCVM).
     * Chromebooks run Android inside a container or VM, lacking physical telephony and direct Wi-Fi AP control.
     */
    fun isChromebook(context: Context): Boolean {
        val pm = context.packageManager
        val hasArcFeature = pm.hasSystemFeature("org.chromium.arc") ||
                pm.hasSystemFeature("org.chromium.arc.device_management")
        val isChromiumBrand = Build.BRAND.contains("chromium", ignoreCase = true) ||
                Build.MANUFACTURER.contains("chromium", ignoreCase = true) ||
                Build.DEVICE.contains("cheets", ignoreCase = true) ||
                Build.PRODUCT.contains("cheets", ignoreCase = true)
        val isChromeOsUser = System.getProperty("os.name")?.contains("Chrome", ignoreCase = true) == true
        return hasArcFeature || isChromiumBrand || isChromeOsUser
    }

    fun getDeviceInfo(context: Context): DeviceInfo {
        val isArc = isChromebook(context)
        val pm = context.packageManager

        val hasWifi = try {
            pm.hasSystemFeature(PackageManager.FEATURE_WIFI)
        } catch (_: Exception) { false }

        val hasWifiDirect = try {
            pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
        } catch (_: Exception) { false }

        val hasTelephony = try {
            pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        } catch (_: Exception) { false }

        val arcVersion = if (isArc) {
            System.getProperty("ro.boot.container") ?: "ARC container active"
        } else null

        val notes = if (isArc) {
            "Running on ChromeOS (Chromebook). Network hardware management is handled by ChromeOS. " +
                    "NetShare runs smoothly in client and proxy host mode via standard IP sockets."
        } else {
            "Running on Android device. Can act as full Wi-Fi hotspot sharer and proxy host."
        }

        return DeviceInfo(
            isChromebook = isArc,
            model = Build.MODEL,
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            arcVersion = arcVersion,
            hasWifi = hasWifi,
            hasWifiDirect = hasWifiDirect,
            hasTelephony = hasTelephony,
            notes = notes
        )
    }

    /**
     * Enumerate all active network interfaces and their IPv4 / IPv6 addresses safely.
     */
    fun getNetworkInterfaces(): List<NetworkInterfaceInfo> {
        val result = mutableListOf<NetworkInterfaceInfo>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                var ipv4: String? = null
                var ipv6: String? = null

                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (addr is Inet4Address) {
                        ipv4 = addr.hostAddress
                    } else if (addr is Inet6Address) {
                        if (ipv6 == null) {
                            ipv6 = addr.hostAddress?.substringBefore("%")
                        }
                    }
                }

                if (ipv4 != null || ipv6 != null) {
                    result.add(
                        NetworkInterfaceInfo(
                            name = intf.name,
                            displayName = intf.displayName,
                            ipv4 = ipv4,
                            ipv6 = ipv6,
                            isUp = try { intf.isUp } catch (_: Exception) { true },
                            isLoopback = try { intf.isLoopback } catch (_: Exception) { false }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Resolves the best candidate IPv4 address to display to the user for connecting other devices.
     * Prefers Wi-Fi direct (192.168.49.x), Android hotspot (192.168.43.x), or Wi-Fi wlan0.
     */
    fun getPrimaryIpAddress(): String {
        val interfaces = getNetworkInterfaces()

        // 1. Check for Wi-Fi direct address (standard 192.168.49.x or p2p interface)
        val p2p = interfaces.firstOrNull { it.name.contains("p2p", ignoreCase = true) && it.ipv4 != null }
        if (p2p?.ipv4 != null) return p2p.ipv4

        val p2pIp = interfaces.firstOrNull { it.ipv4?.startsWith("192.168.49.") == true }
        if (p2pIp?.ipv4 != null) return p2pIp.ipv4

        // 2. Check for hotspot address (192.168.43.x or ap0 / swlan0)
        val ap = interfaces.firstOrNull {
            (it.name.contains("ap", ignoreCase = true) || it.name.contains("wlan1", ignoreCase = true)) &&
                    it.ipv4 != null
        }
        if (ap?.ipv4 != null) return ap.ipv4

        val hotspotIp = interfaces.firstOrNull { it.ipv4?.startsWith("192.168.43.") == true }
        if (hotspotIp?.ipv4 != null) return hotspotIp.ipv4

        // 3. Check for standard Wi-Fi wlan0
        val wlan = interfaces.firstOrNull { it.name.startsWith("wlan", ignoreCase = true) && it.ipv4 != null }
        if (wlan?.ipv4 != null) return wlan.ipv4

        // 4. Check for ethernet or ARC virtual bridge (eth0, arc0, veth)
        val eth = interfaces.firstOrNull {
            (it.name.startsWith("eth", ignoreCase = true) || it.name.startsWith("arc", ignoreCase = true)) &&
                    it.ipv4 != null
        }
        if (eth?.ipv4 != null) return eth.ipv4

        // 5. Any non-loopback IPv4
        val anyNonLoopback = interfaces.firstOrNull { !it.isLoopback && it.ipv4 != null }
        if (anyNonLoopback?.ipv4 != null) return anyNonLoopback.ipv4

        return "127.0.0.1"
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun formatSpeed(bytesPerSec: Long): String {
        val kbps = bytesPerSec / 1024.0
        if (kbps < 1024) {
            return String.format(Locale.US, "%.1f KB/s", kbps)
        }
        val mbps = kbps / 1024.0
        return String.format(Locale.US, "%.2f MB/s", mbps)
    }
}
