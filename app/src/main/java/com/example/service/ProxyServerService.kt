package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.manager.ProxyManager
import com.example.proxy.HttpProxyServer
import com.example.util.DeviceHelper

class ProxyServerService : Service() {

    companion object {
        const val ACTION_START = "com.example.netshare.ACTION_START"
        const val ACTION_STOP = "com.example.netshare.ACTION_STOP"
        const val EXTRA_PORT = "EXTRA_PORT"

        private const val CHANNEL_ID = "netshare_proxy_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "ProxyServerService"
    }

    private var proxyServer: HttpProxyServer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            stopProxy()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val port = intent?.getIntExtra(EXTRA_PORT, 8282) ?: 8282
        startProxy(port)

        return START_STICKY
    }

    private fun startProxy(port: Int) {
        if (proxyServer != null) {
            return
        }

        // Acquire WakeLock safely
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NetShare:ProxyWakeLock")?.apply {
                acquire(24 * 60 * 60 * 1000L) // 24 hours max
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquisition warning: ${e.message}")
        }

        // Acquire WifiLock safely (avoid crash on Chromebooks where WifiLock might fail)
        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "NetShare:ProxyWifiLock")?.apply {
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "WifiLock not available or failed on this platform (normal on Chromebook): ${e.message}")
        }

        val notification = buildNotification("Starting NetShare Proxy...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var fgsType = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                }
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}", e)
            startForeground(NOTIFICATION_ID, notification)
        }

        proxyServer = HttpProxyServer(
            port = port,
            onLogEntry = { entry ->
                ProxyManager.addLogEntry(entry)
            },
            onStatsUpdated = { clients, up, down ->
                ProxyManager.updateStats(clients, up, down)
                updateNotification(clients, up, down)
            }
        )

        val success = proxyServer?.start() ?: false
        if (success) {
            ProxyManager.onServerStarted()
            val primaryIp = DeviceHelper.getPrimaryIpAddress()
            updateNotificationText("Active on $primaryIp:$port (Ready for Chromebook & devices)")
        } else {
            stopProxy()
            stopSelf()
        }
    }

    private fun updateNotification(clients: Int, up: Long, down: Long) {
        val total = DeviceHelper.formatBytes(up + down)
        val primaryIp = DeviceHelper.getPrimaryIpAddress()
        val text = "Host: $primaryIp | Clients: $clients | Transferred: $total"
        updateNotificationText(text)
    }

    private fun updateNotificationText(text: String) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildNotification(text))
        } catch (_: Exception) {}
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ProxyServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NetShare Hotspot Proxy")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NetShare Proxy Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status of NetShare proxy server"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopProxy() {
        try {
            proxyServer?.stop()
            proxyServer = null
        } catch (_: Exception) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}

        ProxyManager.onServerStopped()
    }

    override fun onDestroy() {
        stopProxy()
        super.onDestroy()
    }
}
