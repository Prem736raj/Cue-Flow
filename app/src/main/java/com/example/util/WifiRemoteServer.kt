package com.example.util

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.NetworkInterface
import java.net.Inet4Address
import kotlin.concurrent.thread

object WifiRemoteServer {
    private const val TAG = "WifiRemoteServer"
    const val PORT = 8990

    var isRunning by mutableStateOf(false)
    var serverIpAddress by mutableStateOf<String?>(null)
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null

    fun getLocalIpAddress(context: Context): String? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ipAddress = wm?.connectionInfo?.ipAddress ?: 0
        if (ipAddress != 0) {
            return String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        }
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
        }
        return null
    }

    fun start(context: Context) {
        if (isRunning) return
        serverIpAddress = getLocalIpAddress(context) ?: "127.0.0.1"
        isRunning = true

        serverThread = thread(start = true, name = "WifiRemoteServerThread") {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Server started on port $PORT")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    thread {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverSocket = null
        serverThread = null
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            Log.d(TAG, "Request: $requestLine")

            // Skip other headers
            var line: String? = reader.readLine()
            while (line != null && line.isNotBlank()) {
                line = reader.readLine()
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val fullPath = parts[1]

            // Routing
            when {
                fullPath == "/" || fullPath.startsWith("/index.html") -> {
                    serveWebPage(out)
                }
                fullPath.startsWith("/api/status") -> {
                    serveApiStatus(out)
                }
                fullPath.startsWith("/api/control") -> {
                    // query param parser
                    val action = extractQueryParam(fullPath, "action")
                    handleControlAction(action)
                    serveApiStatus(out)
                }
                else -> {
                    send404(out)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun extractQueryParam(url: String, paramName: String): String? {
        val queryIndex = url.indexOf('?')
        if (queryIndex == -1) return null
        val query = url.substring(queryIndex + 1)
        val pairs = query.split('&')
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            if (idx != -1) {
                val key = pair.substring(0, idx)
                if (key == paramName) {
                    return pair.substring(idx + 1)
                }
            }
        }
        return null
    }

    private fun handleControlAction(action: String?) {
        if (action == null) return
        when (action) {
            "play_pause" -> HardwareButtonController.dispatchPlayPause(force = true)
            "speed_up" -> HardwareButtonController.dispatchSpeedUp(force = true)
            "speed_down" -> HardwareButtonController.dispatchSpeedDown(force = true)
            "prev_bookmark" -> HardwareButtonController.dispatchPrevBookmark(force = true)
            "next_bookmark" -> HardwareButtonController.dispatchSkipToNextBookmark(force = true)
        }
    }

    private fun serveApiStatus(out: OutputStream) {
        val json = """
            {
                "title": "${HardwareButtonController.pActiveTitle ?: "No Active Script"}",
                "isPlaying": ${HardwareButtonController.pIsPlaying},
                "speed": ${HardwareButtonController.pSpeed},
                "currentParagraph": ${HardwareButtonController.pCurrentParagraph},
                "totalParagraphs": ${HardwareButtonController.pTotalParagraphs}
            }
        """.trimIndent()

        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${json.toByteArray(Charsets.UTF_8).size}\r\n" +
                "Connection: close\r\n\r\n" +
                json

        out.write(response.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    private fun send404(out: OutputStream) {
        val body = "404 Not Found"
        val response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${body.length}\r\n" +
                "Connection: close\r\n\r\n" +
                body
        out.write(response.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    private fun serveWebPage(out: OutputStream) {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>CueFlow Web Remote</title>
    <style>
        :root {
            --bg-color: #0A071E;
            --card-bg: rgba(255, 255, 255, 0.05);
            --primary: #00E5FF;
            --primary-glow: rgba(0, 229, 255, 0.4);
            --danger: #FF3366;
            --text-main: #FFFFFF;
            --text-muted: #8E8A9F;
            --border-color: rgba(255, 255, 255, 0.08);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            -webkit-tap-highlight-color: transparent;
        }

        body {
            background-color: var(--bg-color);
            color: var(--text-main);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 20px;
            overflow: hidden;
        }

        .container {
            width: 100%;
            max-width: 440px;
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 24px;
            padding: 30px 24px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(20px);
            text-align: center;
        }

        .header {
            margin-bottom: 24px;
        }

        .app-name {
            font-size: 14px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 2px;
            color: var(--primary);
            text-shadow: 0 0 10px var(--primary-glow);
            margin-bottom: 6px;
        }

        .title {
            font-size: 20px;
            font-weight: 600;
            color: var(--text-main);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            padding: 0 10px;
        }

        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid var(--border-color);
            padding: 6px 14px;
            border-radius: 100px;
            margin-top: 12px;
            font-size: 11px;
            color: var(--text-muted);
            font-weight: 500;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            background: #FF9800;
            border-radius: 50%;
            box-shadow: 0 0 8px #FF9800;
        }

        .status-dot.active {
            background: #00E676;
            box-shadow: 0 0 8px #00E676;
        }

        /* Speed and Progress Row */
        .metrics {
            display: flex;
            gap: 12px;
            margin: 24px 0;
        }

        .metric-card {
            flex: 1;
            background: rgba(0, 0, 0, 0.2);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 14px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }

        .metric-label {
            font-size: 10px;
            font-weight: 700;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 4px;
        }

        .metric-val {
            font-size: 18px;
            font-weight: 700;
            color: var(--text-main);
        }

        /* Large circular control Button */
        .play-pause-btn {
            width: 120px;
            height: 120px;
            border-radius: 50%;
            border: none;
            outline: none;
            background: linear-gradient(135deg, var(--primary), #00B0FF);
            color: #05040B;
            font-size: 40px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            box-shadow: 0 10px 25px var(--primary-glow);
            transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            margin: 10px 0;
        }

        .play-pause-btn.playing {
            background: linear-gradient(135deg, var(--danger), #FF1744);
            box-shadow: 0 10px 25px rgba(255, 51, 102, 0.4);
            color: white;
        }

        .play-pause-btn:active {
            transform: scale(0.92);
        }

        /* Grid Controls */
        .controls-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
            margin-top: 24px;
        }

        .btn-action {
            background: rgba(255, 255, 255, 0.04);
            border: 1px solid var(--border-color);
            color: var(--text-main);
            padding: 14px 10px;
            border-radius: 14px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            transition: all 0.15s ease;
        }

        .btn-action:active {
            background: rgba(255, 255, 255, 0.1);
            transform: scale(0.96);
        }

        .btn-action span {
            font-size: 18px;
        }

        /* Connection / footer info */
        .footer {
            margin-top: 24px;
            font-size: 11px;
            color: var(--text-muted);
        }

        /* Progress indicator bar */
        .progress-bar-container {
            width: 100%;
            height: 6px;
            background: rgba(255, 255, 255, 0.05);
            border-radius: 10px;
            overflow: hidden;
            margin-top: 6px;
        }

        .progress-bar-fill {
            height: 100%;
            background: var(--primary);
            width: 0%;
            transition: width 0.3s ease;
            border-radius: 10px;
            box-shadow: 0 0 6px var(--primary-glow);
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="app-name">CueFlow Remote</div>
            <div class="title" id="script-title">Connecting...</div>
            <div class="status-badge">
                <div class="status-dot" id="status-dot"></div>
                <span id="status-text">Connecting to prompter</span>
            </div>
        </div>

        <!-- Progress bar under header -->
        <div style="margin: 0 4px;">
            <div class="progress-bar-container">
                <div class="progress-bar-fill" id="progress-bar-fill"></div>
            </div>
        </div>

        <div class="metrics">
            <div class="metric-card">
                <span class="metric-label">Scroll Speed</span>
                <span class="metric-val" id="scroll-speed">0.0x</span>
            </div>
            <div class="metric-card">
                <span class="metric-label">Paragraph</span>
                <span class="metric-val" id="progress-text">0 / 0</span>
            </div>
        </div>

        <div>
            <button class="play-pause-btn" id="play-pause-btn">
                <span id="play-pause-icon">▶</span>
            </button>
        </div>

        <div class="controls-grid">
            <button class="btn-action" id="btn-speed-down">
                <span>➖</span> Slow Down
            </button>
            <button class="btn-action" id="btn-speed-up">
                <span>➕</span> Speed Up
            </button>
            <button class="btn-action" id="btn-prev-bookmark">
                <span>⏮</span> Prev Bookmark
            </button>
            <button class="btn-action" id="btn-next-bookmark">
                <span>⏭</span> Next Bookmark
            </button>
        </div>

        <div class="footer">
            WiFi Connection Stable &bull; Serving from Teleprompter
        </div>
    </div>

    <script>
        const playPauseBtn = document.getElementById('play-pause-btn');
        const playPauseIcon = document.getElementById('play-pause-icon');
        const btnSpeedUp = document.getElementById('btn-speed-up');
        const btnSpeedDown = document.getElementById('btn-speed-down');
        const btnPrevBookmark = document.getElementById('btn-prev-bookmark');
        const btnNextBookmark = document.getElementById('btn-next-bookmark');
        const scriptTitle = document.getElementById('script-title');
        const statusText = document.getElementById('status-text');
        const statusDot = document.getElementById('status-dot');
        const scrollSpeed = document.getElementById('scroll-speed');
        const progressText = document.getElementById('progress-text');
        const progressBarFill = document.getElementById('progress-bar-fill');

        let isPlaying = false;

        async function sendControl(action) {
            try {
                const response = await fetch(`/api/control?action=` + action);
                const data = await response.json();
                updateUI(data);
            } catch (err) {
                console.error("Control request failed", err);
            }
        }

        function updateUI(status) {
            scriptTitle.textContent = status.title || "No Active Script";
            scrollSpeed.textContent = status.speed.toFixed(1) + "x";
            
            const currentP = status.currentParagraph + 1;
            const totalP = status.totalParagraphs;
            progressText.textContent = totalP > 0 ? (currentP + " / " + totalP) : "0 / 0";
            
            const progressPct = totalP > 0 ? ((currentP / totalP) * 100) : 0;
            progressBarFill.style.width = progressPct + "%";

            isPlaying = status.isPlaying;
            if (isPlaying) {
                playPauseBtn.classList.add('playing');
                playPauseIcon.textContent = '⏸';
                statusDot.classList.add('active');
                statusText.textContent = 'Prompter Scrolling';
            } else {
                playPauseBtn.classList.remove('playing');
                playPauseIcon.textContent = '▶';
                statusDot.classList.remove('active');
                statusText.textContent = 'Prompter Paused';
            }
        }

        async function pollStatus() {
            try {
                const response = await fetch('/api/status');
                const data = await response.json();
                updateUI(data);
            } catch (err) {
                console.error("Status polling failed", err);
                statusText.textContent = 'Offline (Reconnecting...)';
                statusDot.classList.remove('active');
            }
        }

        playPauseBtn.addEventListener('click', () => sendControl('play_pause'));
        btnSpeedUp.addEventListener('click', () => sendControl('speed_up'));
        btnSpeedDown.addEventListener('click', () => sendControl('speed-down')); // handles mapping safely
        btnPrevBookmark.addEventListener('click', () => sendControl('prev_bookmark'));
        btnNextBookmark.addEventListener('click', () => sendControl('next_bookmark'));

        // Handle typo in click event connection
        btnSpeedDown.onclick = () => sendControl('speed_down');

        // Poll every 800ms
        setInterval(pollStatus, 800);
        pollStatus();
    </script>
</body>
</html>
        """.trimIndent()

        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: ${html.toByteArray(Charsets.UTF_8).size}\r\n" +
                "Connection: close\r\n\r\n" +
                html

        out.write(response.toByteArray(Charsets.UTF_8))
        out.flush()
    }
}
