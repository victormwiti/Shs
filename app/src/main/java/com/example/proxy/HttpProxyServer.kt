package com.example.proxy

import android.util.Log
import com.example.model.TrafficLogEntry
import com.example.util.DeviceHelper
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class HttpProxyServer(
    val port: Int = 8282,
    private val onLogEntry: (TrafficLogEntry) -> Unit,
    private val onStatsUpdated: (activeClients: Int, totalUploaded: Long, totalDownloaded: Long) -> Unit
) {
    private val tag = "NetShareProxy"
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val threadPool = Executors.newCachedThreadPool()

    private val activeClients = AtomicInteger(0)
    private val totalUploaded = AtomicLong(0)
    private val totalDownloaded = AtomicLong(0)
    private val activeSockets = ConcurrentHashMap<Socket, Boolean>()
    private val logIdCounter = AtomicLong(1)

    fun start(): Boolean {
        if (isRunning.get()) return true

        return try {
            val ss = ServerSocket()
            ss.reuseAddress = true
            ss.bind(InetSocketAddress("0.0.0.0", port), 256)
            serverSocket = ss
            isRunning.set(true)
            Log.i(tag, "NetShare Proxy Server listening on 0.0.0.0:$port")

            threadPool.execute {
                acceptLoop(ss)
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start proxy on port $port: ${e.message}", e)
            isRunning.set(false)
            false
        }
    }

    private fun acceptLoop(ss: ServerSocket) {
        while (isRunning.get() && !ss.isClosed) {
            try {
                val clientSocket = ss.accept()
                clientSocket.tcpNoDelay = true
                clientSocket.soTimeout = 45000 // 45s socket timeout
                activeSockets[clientSocket] = true
                activeClients.incrementAndGet()
                notifyStats()

                threadPool.execute {
                    handleConnection(clientSocket)
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.w(tag, "Accept exception: ${e.message}")
                }
            }
        }
    }

    private fun handleConnection(clientSocket: Socket) {
        val clientAddress = clientSocket.inetAddress?.hostAddress ?: "Unknown"
        val startTime = System.currentTimeMillis()
        var targetHost = "Unknown"
        var protocol = "HTTP"
        var bytesCount = 0L

        try {
            val clientIn = BufferedInputStream(clientSocket.getInputStream())
            val clientOut = BufferedOutputStream(clientSocket.getOutputStream())

            clientIn.mark(16)
            val firstByte = clientIn.read()
            clientIn.reset()

            if (firstByte == -1) {
                return
            }

            if (firstByte == 0x05) {
                // SOCKS5 Protocol detected
                protocol = "SOCKS5"
                bytesCount = handleSocks5(clientSocket, clientIn, clientOut) { host ->
                    targetHost = host
                }
            } else {
                // HTTP / HTTPS CONNECT detected
                bytesCount = handleHttp(clientSocket, clientIn, clientOut) { host, proto ->
                    targetHost = host
                    protocol = proto
                }
            }

            val duration = System.currentTimeMillis() - startTime
            onLogEntry(
                TrafficLogEntry(
                    id = logIdCounter.getAndIncrement(),
                    timestamp = System.currentTimeMillis(),
                    clientAddress = clientAddress,
                    targetHost = targetHost,
                    protocol = protocol,
                    bytesTransferred = bytesCount,
                    status = "Completed",
                    durationMs = duration
                )
            )
        } catch (e: Exception) {
            if (isRunning.get()) {
                val duration = System.currentTimeMillis() - startTime
                onLogEntry(
                    TrafficLogEntry(
                        id = logIdCounter.getAndIncrement(),
                        timestamp = System.currentTimeMillis(),
                        clientAddress = clientAddress,
                        targetHost = targetHost,
                        protocol = protocol,
                        bytesTransferred = bytesCount,
                        status = "Closed",
                        durationMs = duration
                    )
                )
            }
        } finally {
            activeSockets.remove(clientSocket)
            activeClients.decrementAndGet()
            notifyStats()
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun handleHttp(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        onTargetIdentified: (host: String, proto: String) -> Unit
    ): Long {
        val reader = BufferedReader(InputStreamReader(clientIn))
        val initialLine = reader.readLine() ?: return 0L

        val parts = initialLine.split(" ")
        if (parts.size < 2) return 0L

        val method = parts[0]
        val target = parts[1]

        // 1. Check for PAC file or status requests
        if (method.equals("GET", ignoreCase = true)) {
            val uri = target.substringBefore("?")
            if (uri == "/proxy.pac" || uri.endsWith("wpad.dat")) {
                onTargetIdentified("Local PAC Script", "PAC")
                return servePacFile(clientOut)
            } else if (uri == "/" || uri == "/status" || uri == "/ping") {
                onTargetIdentified("Local Status Dashboard", "HTTP")
                return serveStatusPage(clientOut)
            }
        }

        // 2. HTTPS CONNECT Tunneling
        if (method.equals("CONNECT", ignoreCase = true)) {
            val hostPort = target.split(":")
            val host = hostPort[0]
            val remotePort = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 443 else 443
            onTargetIdentified("$host:$remotePort", "HTTPS CONNECT")

            // Read remaining headers
            while (true) {
                val header = reader.readLine() ?: break
                if (header.isEmpty()) break
            }

            val remoteSocket = Socket()
            remoteSocket.tcpNoDelay = true
            remoteSocket.connect(InetSocketAddress(host, remotePort), 10000)
            remoteSocket.soTimeout = 45000
            activeSockets[remoteSocket] = true

            try {
                // Send 200 Connection Established
                val response = "HTTP/1.1 200 Connection Established\r\nProxy-Agent: NetShare/2.0\r\n\r\n"
                clientOut.write(response.toByteArray(Charsets.US_ASCII))
                clientOut.flush()

                return pipeBidirectional(clientSocket, remoteSocket)
            } finally {
                activeSockets.remove(remoteSocket)
                try { remoteSocket.close() } catch (_: Exception) {}
            }
        }

        // 3. Regular HTTP (GET, POST, etc.)
        onTargetIdentified(target, "HTTP $method")
        var host = ""
        var remotePort = 80

        val headers = mutableListOf<String>()
        var line = reader.readLine()
        while (line != null && line.isNotEmpty()) {
            headers.add(line)
            if (line.startsWith("Host:", ignoreCase = true)) {
                val hostHeader = line.substring(5).trim()
                if (hostHeader.contains(":")) {
                    host = hostHeader.substringBefore(":")
                    remotePort = hostHeader.substringAfter(":").toIntOrNull() ?: 80
                } else {
                    host = hostHeader
                }
            }
            line = reader.readLine()
        }

        if (host.isEmpty()) {
            if (target.startsWith("http://", ignoreCase = true)) {
                val withoutScheme = target.substring(7)
                host = withoutScheme.substringBefore("/").substringBefore(":")
            }
        }

        if (host.isEmpty()) return 0L

        val remoteSocket = Socket()
        remoteSocket.tcpNoDelay = true
        remoteSocket.connect(InetSocketAddress(host, remotePort), 10000)
        remoteSocket.soTimeout = 45000
        activeSockets[remoteSocket] = true

        try {
            val remoteOut = BufferedOutputStream(remoteSocket.getOutputStream())
            // Forward modified request line
            val relativePath = if (target.startsWith("http://", ignoreCase = true)) {
                val pathStart = target.indexOf('/', 7)
                if (pathStart != -1) target.substring(pathStart) else "/"
            } else {
                target
            }

            val forwardedReq = "$method $relativePath HTTP/1.1\r\n"
            remoteOut.write(forwardedReq.toByteArray(Charsets.US_ASCII))
            for (h in headers) {
                remoteOut.write("$h\r\n".toByteArray(Charsets.US_ASCII))
            }
            remoteOut.write("\r\n".toByteArray(Charsets.US_ASCII))
            remoteOut.flush()

            return pipeBidirectional(clientSocket, remoteSocket)
        } finally {
            activeSockets.remove(remoteSocket)
            try { remoteSocket.close() } catch (_: Exception) {}
        }
    }

    private fun handleSocks5(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        onTargetIdentified: (host: String) -> Unit
    ): Long {
        // Handshake: VER (05) + NMETHODS + METHODS
        val ver = clientIn.read()
        val nmethods = clientIn.read()
        val methods = ByteArray(nmethods)
        clientIn.read(methods)

        // Reply: VER (05) + METHOD (00 = NO AUTH)
        clientOut.write(byteArrayOf(0x05, 0x00))
        clientOut.flush()

        // Request: VER (05) + CMD (01=CONNECT) + RSV (00) + ATYP + DST.ADDR + DST.PORT
        val reqVer = clientIn.read()
        val cmd = clientIn.read()
        val rsv = clientIn.read()
        val atyp = clientIn.read()

        if (cmd != 0x01) {
            // Command not supported
            clientOut.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            clientOut.flush()
            return 0L
        }

        val targetHost: String = when (atyp) {
            0x01 -> { // IPv4 (4 bytes)
                val ipBytes = ByteArray(4)
                clientIn.read(ipBytes)
                "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}.${ipBytes[3].toInt() and 0xFF}"
            }
            0x03 -> { // Domain name (1 byte length + domain bytes)
                val len = clientIn.read()
                val domainBytes = ByteArray(len)
                clientIn.read(domainBytes)
                String(domainBytes, Charsets.US_ASCII)
            }
            0x04 -> { // IPv6 (16 bytes)
                val ip6Bytes = ByteArray(16)
                clientIn.read(ip6Bytes)
                "ipv6-target"
            }
            else -> return 0L
        }

        val portHi = clientIn.read()
        val portLo = clientIn.read()
        val targetPort = ((portHi and 0xFF) shl 8) or (portLo and 0xFF)

        onTargetIdentified("$targetHost:$targetPort")

        val remoteSocket = Socket()
        remoteSocket.tcpNoDelay = true
        remoteSocket.connect(InetSocketAddress(targetHost, targetPort), 10000)
        remoteSocket.soTimeout = 45000
        activeSockets[remoteSocket] = true

        try {
            // Reply: VER 05, REP 00 (Success), RSV 00, ATYP 01, BND.ADDR 0.0.0.0, BND.PORT 0
            clientOut.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            clientOut.flush()

            return pipeBidirectional(clientSocket, remoteSocket)
        } finally {
            activeSockets.remove(remoteSocket)
            try { remoteSocket.close() } catch (_: Exception) {}
        }
    }

    private fun pipeBidirectional(client: Socket, remote: Socket): Long {
        val totalTransferred = AtomicLong(0)
        val clientIn = client.getInputStream()
        val clientOut = client.getOutputStream()
        val remoteIn = remote.getInputStream()
        val remoteOut = remote.getOutputStream()

        val uploadThread = Thread {
            val buffer = ByteArray(16384)
            try {
                while (isRunning.get() && !client.isClosed && !remote.isClosed) {
                    val read = clientIn.read(buffer)
                    if (read == -1) break
                    remoteOut.write(buffer, 0, read)
                    remoteOut.flush()
                    totalTransferred.addAndGet(read.toLong())
                    totalUploaded.addAndGet(read.toLong())
                }
            } catch (_: Exception) {}
            try { remote.shutdownOutput() } catch (_: Exception) {}
        }

        val downloadThread = Thread {
            val buffer = ByteArray(16384)
            try {
                while (isRunning.get() && !client.isClosed && !remote.isClosed) {
                    val read = remoteIn.read(buffer)
                    if (read == -1) break
                    clientOut.write(buffer, 0, read)
                    clientOut.flush()
                    totalTransferred.addAndGet(read.toLong())
                    totalDownloaded.addAndGet(read.toLong())
                }
            } catch (_: Exception) {}
            try { client.shutdownOutput() } catch (_: Exception) {}
        }

        uploadThread.start()
        downloadThread.start()

        uploadThread.join()
        downloadThread.join()

        notifyStats()
        return totalTransferred.get()
    }

    private fun servePacFile(out: OutputStream): Long {
        val primaryIp = DeviceHelper.getPrimaryIpAddress()
        val pacContent = """
            function FindProxyForURL(url, host) {
                if (isPlainHostName(host) || host === "127.0.0.1" || host === "localhost") {
                    return "DIRECT";
                }
                return "PROXY $primaryIp:$port; DIRECT";
            }
        """.trimIndent()

        val bytes = pacContent.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/x-ns-proxy-autoconfig\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        out.write(headers.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
        return (headers.length + bytes.size).toLong()
    }

    private fun serveStatusPage(out: OutputStream): Long {
        val primaryIp = DeviceHelper.getPrimaryIpAddress()
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>NetShare Proxy Online</title>
                <style>
                    body { font-family: system-ui, -apple-system, sans-serif; background: #0A1128; color: #E0E1DD; padding: 2rem; }
                    .card { background: #1B263B; border-radius: 12px; padding: 1.5rem; max-width: 500px; margin: auto; box-shadow: 0 4px 20px rgba(0,0,0,0.5); }
                    .badge { background: #00E676; color: #0A1128; font-weight: bold; padding: 4px 12px; border-radius: 20px; display: inline-block; }
                    h1 { color: #00E5FF; margin-top: 0; }
                    code { background: #0D1B2A; padding: 2px 6px; border-radius: 4px; color: #00E5FF; }
                </style>
            </head>
            <body>
                <div class="card">
                    <span class="badge">ONLINE</span>
                    <h1>NetShare Proxy Server</h1>
                    <p>Connected through Android / Chromebook Hotspot</p>
                    <p><strong>Proxy Host:</strong> <code>$primaryIp</code></p>
                    <p><strong>Proxy Port:</strong> <code>$port</code></p>
                    <p><strong>PAC URL:</strong> <code>http://$primaryIp:$port/proxy.pac</code></p>
                    <hr style="border-color: #415A77;">
                    <p style="font-size: 0.9rem; color: #778DA9;">ChromeOS / Chromebook: Set Wi-Fi Manual Proxy to $primaryIp:$port or use the PAC URL above.</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        out.write(headers.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
        return (headers.length + bytes.size).toLong()
    }

    private fun notifyStats() {
        onStatsUpdated(activeClients.get(), totalUploaded.get(), totalDownloaded.get())
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) return

        try {
            serverSocket?.close()
        } catch (_: Exception) {}

        for ((socket, _) in activeSockets) {
            try { socket.close() } catch (_: Exception) {}
        }
        activeSockets.clear()
        activeClients.set(0)
        notifyStats()
        Log.i(tag, "NetShare Proxy Server stopped.")
    }
}
