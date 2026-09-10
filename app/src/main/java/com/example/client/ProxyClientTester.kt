package com.example.client

import com.example.model.ProxyTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

object ProxyClientTester {

    suspend fun testProxyConnection(host: String, port: Int): ProxyTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
            val client = OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .build()

            // 1. Try testing proxy status endpoint first (instant local ping)
            val localRequest = Request.Builder()
                .url("http://$host:$port/status")
                .build()

            val localResponse = try {
                client.newCall(localRequest).execute()
            } catch (_: Exception) { null }

            val localSuccess = localResponse?.isSuccessful == true
            localResponse?.close()

            // 2. Try testing public internet request through proxy (e.g., Google or connectivitycheck)
            val internetRequest = Request.Builder()
                .url("http://connectivitycheck.gstatic.com/generate_204")
                .build()

            val response = client.newCall(internetRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val code = response.code
            response.close()

            if (code == 204 || code == 200) {
                ProxyTestResult(
                    success = true,
                    latencyMs = latency,
                    message = "Connection Verified! Chromebook is routing traffic through NetShare successfully.",
                    responseCode = code
                )
            } else if (localSuccess) {
                ProxyTestResult(
                    success = true,
                    latencyMs = latency,
                    message = "Connected to NetShare proxy server on $host:$port. (External check returned $code).",
                    responseCode = code
                )
            } else {
                ProxyTestResult(
                    success = false,
                    latencyMs = latency,
                    message = "Proxy responded with HTTP $code. Check if host has active internet connection.",
                    responseCode = code
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ProxyTestResult(
                success = false,
                latencyMs = latency,
                message = "Failed to connect: ${e.message ?: "Connection timed out"}. Verify Wi-Fi network and IP ($host:$port)."
            )
        }
    }
}
