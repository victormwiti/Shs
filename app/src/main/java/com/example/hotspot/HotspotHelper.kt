package com.example.hotspot

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.example.util.DeviceHelper

sealed class HotspotState {
    data object Inactive : HotspotState()
    data object Starting : HotspotState()
    data class Active(val ssid: String, val passphrase: String?) : HotspotState()
    data class Failed(val reason: String) : HotspotState()
    data class ChromebookNotice(val message: String) : HotspotState()
}

class HotspotHelper(private val context: Context) {

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun startHotspot(
        onStateChanged: (HotspotState) -> Unit
    ) {
        if (DeviceHelper.isChromebook(context)) {
            onStateChanged(
                HotspotState.ChromebookNotice(
                    "Running on Chromebook (ChromeOS). Wi-Fi hardware tethering is managed by ChromeOS or your Android phone. " +
                            "To share connection, run NetShare Host on your Android phone and connect your Chromebook to it."
                )
            )
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onStateChanged(HotspotState.Failed("Automatic LocalHotspot requires Android 8.0+. Please enable hotspot manually in Settings."))
            return
        }

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager == null) {
                onStateChanged(HotspotState.Failed("Wi-Fi service is not available on this device."))
                return
            }

            onStateChanged(HotspotState.Starting)

            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation?) {
                    super.onStarted(res)
                    reservation = res
                    val config = res?.wifiConfiguration
                    val ssid = config?.SSID ?: "NetShare-Hotspot"
                    val password = config?.preSharedKey
                    onStateChanged(HotspotState.Active(ssid, password))
                }

                override fun onStopped() {
                    super.onStopped()
                    reservation = null
                    onStateChanged(HotspotState.Inactive)
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    reservation = null
                    val reasonStr = when (reason) {
                        ERROR_NO_CHANNEL -> "No channel available"
                        ERROR_GENERIC -> "Generic hotspot failure (often already tethered)"
                        ERROR_INCOMPATIBLE_MODE -> "Incompatible Wi-Fi mode"
                        ERROR_TETHERING_DISALLOWED -> "Tethering disallowed by carrier/admin"
                        else -> "Hotspot error code $reason"
                    }
                    onStateChanged(HotspotState.Failed(reasonStr))
                }
            }, null)
        } catch (e: SecurityException) {
            onStateChanged(HotspotState.Failed("Hotspot permission missing: ${e.message}. Use manual Hotspot in settings."))
        } catch (e: UnsupportedOperationException) {
            onStateChanged(HotspotState.Failed("Local Hotspot not supported by this hardware. Use standard Hotspot in Android Settings."))
        } catch (e: Exception) {
            onStateChanged(HotspotState.Failed("Unable to start hotspot: ${e.message}"))
        }
    }

    fun stopHotspot(onStateChanged: (HotspotState) -> Unit) {
        try {
            reservation?.close()
            reservation = null
        } catch (_: Exception) {}
        onStateChanged(HotspotState.Inactive)
    }

    fun openTetheringSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
