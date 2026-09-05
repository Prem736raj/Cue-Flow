package com.example.util

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Small local-network remote for teleprompter transport controls.
 *
 * Security properties:
 * - A fresh random token is generated for every server session and is required for every request.
 * - The token is carried by the QR/manual URL once, then by a custom request header from the page.
 * - No script title or script text is exposed over the network.
 * - Control actions require POST and CORS is intentionally not enabled.
 * - Request sizes, socket lifetimes and worker concurrency are bounded.
 * - The server automatically shuts down after an idle period.
 *
 * HTTP is intentionally used only on the user's local network because Android devices generally do
 * not have a mutually trusted certificate for an ad-hoc LAN address. The pairing token protects
 * control access, but users should still only enable the remote on networks they trust.
 */
object WifiRemoteServer {
    const val PORT = 8990
    private const val TAG = "WifiRemoteServer"
    private const val ACCEPT_TIMEOUT_MS = 1_000
    private const val CLIENT_TIMEOUT_MS = 5_000
    private const val MAX_REQUEST_LINE = 2_048
    private const val MAX_HEADER_LINE = 2_048
    private const val MAX_HEADERS = 32
    private const val IDLE_TIMEOUT_MS = 2 * 60 * 1_000L

    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var serverIpAddress: String? = null
        private set

    @Volatile
    var pairingToken: String = ""
        private set

    private val secureRandom = SecureRandom()
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private var workerPool: ExecutorService? = null
    @Volatile
    private var lastRequestAtMs: Long = 0L

    @Synchronized
    fun start(context: Context) {
        if (isRunning) return

        val ip = findLocalIpv4Address()
        if (ip == null) {
            Log.w(TAG, "No non-loopback IPv4 address available; remote not started")
            return
        }

        try {
            val socket = ServerSocket(PORT).apply {
                reuseAddress = true
                soTimeout = ACCEPT_TIMEOUT_MS
            }
            serverSocket = socket
            serverIpAddress = ip
            pairingToken = newPairingToken()
            lastRequestAtMs = System.currentTimeMillis()
            workerPool = Executors.newFixedThreadPool(3)
            isRunning = true

            acceptThread = thread(name = "CueFlow-WifiRemote", isDaemon = true) {
                acceptLoop(socket)
            }
            Log.i(TAG, "Local remote started on $ip:$PORT")
        } catch (error: Exception) {
            Log.e(TAG, "Unable to start local remote", error)
            stop()
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        acceptThread?.interrupt()
        acceptThread = null

        workerPool?.shutdownNow()
        try {
            workerPool?.awaitTermination(250, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        workerPool = null
        serverIpAddress = null
        pairingToken = ""
    }

    private fun acceptLoop(socket: ServerSocket) {
        try {
            while (isRunning) {
                if (System.currentTimeMillis() - lastRequestAtMs > IDLE_TIMEOUT_MS) {
                    Log.i(TAG, "Stopping idle local remote")
                    stop()
                    break
                }

                try {
                    val client = socket.accept()
                    val pool = workerPool
                    if (pool == null || pool.isShutdown) {
                        client.close()
                    } else {
                        pool.execute { handleClient(client) }
                    }
                } catch (_: SocketTimeoutException) {
                    // Wake periodically to enforce the idle timeout.
                }
            }
        } catch (error: Exception) {
            if (isRunning) Log.e(TAG, "Remote accept loop failed", error)
        } finally {
            if (isRunning) stop()
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            try {
                client.soTimeout = CLIENT_TIMEOUT_MS
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))

                val requestLine = reader.readLine() ?: return
                if (requestLine.length > MAX_REQUEST_LINE) {
                    writeResponse(writer, 414, "text/plain; charset=utf-8", "Request URI too long")
                    return
                }

                val parts = requestLine.split(' ', limit = 3)
                if (parts.size != 3) {
                    writeResponse(writer, 400, "text/plain; charset=utf-8", "Bad request")
                    return
                }
                val method = parts[0].uppercase()
                val target = parts[1]

                val headers = linkedMapOf<String, String>()
                var headerCount = 0
                while (headerCount < MAX_HEADERS) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    if (line.length > MAX_HEADER_LINE) {
                        writeResponse(writer, 431, "text/plain; charset=utf-8", "Request header too large")
                        return
                    }
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
                    }
                    headerCount++
                }

                lastRequestAtMs = System.currentTimeMillis()
                route(method, target, headers, writer)
            } catch (error: Exception) {
                Log.d(TAG, "Remote client closed: ${error.message}")
            }
        }
    }

    private fun route(
        method: String,
        target: String,
        headers: Map<String, String>,
        writer: BufferedWriter,
    ) {
        val path = target.substringBefore('?')
        val query = parseQuery(target.substringAfter('?', ""))

        when {
            method == "GET" && path == "/" -> {
                if (!constantTimeEquals(query["token"].orEmpty(), pairingToken)) {
                    writeResponse(writer, 403, "text/plain; charset=utf-8", "Invalid pairing link")
                    return
                }
                writeResponse(writer, 200, "text/html; charset=utf-8", remoteHtml(pairingToken))
            }

            method == "GET" && path == "/api/status" -> {
                if (!authorized(headers)) {
                    writeResponse(writer, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
                    return
                }
                val state = HardwareButtonController
                val body = buildString {
                    append('{')
                    append("\"playing\":").append(state.pIsPlaying)
                    append(",\"speed\":").append(state.pSpeed.coerceIn(0f, 100f))
                    append(",\"currentParagraph\":").append(state.pCurrentParagraph.coerceAtLeast(0))
                    append(",\"totalParagraphs\":").append(state.pTotalParagraphs.coerceAtLeast(0))
                    append('}')
                }
                writeResponse(writer, 200, "application/json; charset=utf-8", body)
            }

            method == "POST" && path == "/api/control" -> {
                if (!authorized(headers)) {
                    writeResponse(writer, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
                    return
                }
                val action = query["action"].orEmpty()
                val accepted = when (action) {
                    "play_pause" -> HardwareButtonController.dispatchPlayPause(force = true).let { true }
                    "speed_up" -> HardwareButtonController.dispatchSpeedUp(force = true).let { true }
                    "speed_down" -> HardwareButtonController.dispatchSpeedDown(force = true).let { true }
                    "next_bookmark" -> HardwareButtonController.dispatchSkipToNextBookmark(force = true).let { true }
                    "prev_bookmark" -> HardwareButtonController.dispatchPrevBookmark(force = true).let { true }
                    else -> false
                }
                if (accepted) {
                    writeResponse(writer, 200, "application/json; charset=utf-8", "{\"ok\":true}")
                } else {
                    writeResponse(writer, 400, "application/json; charset=utf-8", "{\"error\":\"unknown_action\"}")
                }
            }

            else -> writeResponse(writer, 404, "text/plain; charset=utf-8", "Not found")
        }
    }

    private fun authorized(headers: Map<String, String>): Boolean =
        pairingToken.isNotEmpty() && constantTimeEquals(headers["x-cueflow-token"].orEmpty(), pairingToken)

    private fun parseQuery(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split('&').mapNotNull { item ->
            val idx = item.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val key = URLDecoder.decode(item.substring(0, idx), StandardCharsets.UTF_8.name())
            val value = URLDecoder.decode(item.substring(idx + 1), StandardCharsets.UTF_8.name())
            key to value
        }.toMap()
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        val a = left.toByteArray(StandardCharsets.UTF_8)
        val b = right.toByteArray(StandardCharsets.UTF_8)
        var diff = a.size xor b.size
        val max = maxOf(a.size, b.size)
        for (index in 0 until max) {
            val av = if (index < a.size) a[index].toInt() else 0
            val bv = if (index < b.size) b[index].toInt() else 0
            diff = diff or (av xor bv)
        }
        return diff == 0
    }

    private fun writeResponse(
        writer: BufferedWriter,
        status: Int,
        contentType: String,
        body: String,
    ) {
        val reason = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            414 -> "URI Too Long"
            431 -> "Request Header Fields Too Large"
            else -> "Error"
        }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        writer.write("HTTP/1.1 $status $reason\r\n")
        writer.write("Content-Type: $contentType\r\n")
        writer.write("Content-Length: ${bytes.size}\r\n")
        writer.write("Cache-Control: no-store\r\n")
        writer.write("X-Content-Type-Options: nosniff\r\n")
        writer.write("Referrer-Policy: no-referrer\r\n")
        writer.write("Content-Security-Policy: default-src 'self'; script-src 'unsafe-inline'; style-src 'unsafe-inline'\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(body)
        writer.flush()
    }

    private fun newPairingToken(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun findLocalIpv4Address(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.asSequence()
                ?.filter { it.isUp && !it.isLoopback }
                ?.flatMap { it.inetAddresses.toList().asSequence() }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        } catch (error: Exception) {
            Log.w(TAG, "Unable to determine LAN address", error)
            null
        }
    }

    private fun remoteHtml(token: String): String = """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
          <meta name="referrer" content="no-referrer">
          <title>CueFlow Remote</title>
          <style>
            :root{color-scheme:dark;font-family:system-ui,sans-serif}body{margin:0;background:#090a0f;color:#f8fafc;min-height:100vh;display:grid;place-items:center}.card{width:min(92vw,420px);background:#12131c;border:1px solid #2a2d3a;border-radius:24px;padding:24px;box-sizing:border-box}.muted{color:#94a3b8;font-size:13px}.status{display:flex;justify-content:space-between;margin:18px 0;padding:12px;background:#0b0c12;border-radius:12px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}button{min-height:56px;border:0;border-radius:14px;background:#242735;color:#fff;font-size:16px;font-weight:700}button.primary{grid-column:1/-1;background:#38bdf8;color:#071018}button:active{transform:scale(.98)}.warning{margin-top:18px;font-size:12px;color:#fbbf24}
          </style>
        </head>
        <body>
          <main class="card">
            <h1>CueFlow Remote</h1>
            <p class="muted">Paired for this teleprompter session only.</p>
            <div class="status"><span id="state">Connecting…</span><span id="speed"></span></div>
            <div class="grid">
              <button onclick="control('speed_down')">Slower</button>
              <button onclick="control('speed_up')">Faster</button>
              <button class="primary" onclick="control('play_pause')">Play / Pause</button>
              <button onclick="control('prev_bookmark')">Previous mark</button>
              <button onclick="control('next_bookmark')">Next mark</button>
            </div>
            <p class="warning">Use only on a Wi-Fi network you trust. Pairing expires when the remote is stopped or idle.</p>
          </main>
          <script>
            const token = '$token';
            async function request(path, options={}) {
              const headers = Object.assign({}, options.headers || {}, {'X-CueFlow-Token': token});
              return fetch(path, Object.assign({}, options, {headers, cache:'no-store'}));
            }
            async function control(action) {
              try { await request('/api/control?action=' + encodeURIComponent(action), {method:'POST'}); await refresh(); } catch (_) {}
            }
            async function refresh() {
              try {
                const response = await request('/api/status');
                if (!response.ok) return;
                const data = await response.json();
                document.getElementById('state').textContent = data.playing ? 'Playing' : 'Paused';
                document.getElementById('speed').textContent = Number(data.speed).toFixed(1) + 'x';
              } catch (_) {
                document.getElementById('state').textContent = 'Disconnected';
              }
            }
            refresh(); setInterval(refresh, 1500);
          </script>
        </body>
        </html>
    """.trimIndent()
}
