package com.meld.desktop.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

object DesktopSpotifyAuth {

    private val json = Json { ignoreUnknownKeys = true }

    fun findBrowserExecutable(): String? {
        val candidates = listOf(
            "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
        )
        return candidates.firstOrNull { File(it).exists() }
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }

    /**
     * Launches a browser in dedicated app mode displaying the Spotify web login page,
     * monitors the browser session via Chrome DevTools Protocol, and automatically
     * retrieves the sp_dc session cookie once the user logs in.
     */
    suspend fun launchDirectLogin(
        onProgress: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val browserExe = findBrowserExecutable()
            ?: return@withContext Result.failure(IllegalStateException("No compatible Chromium browser (Edge or Chrome) found."))

        val port = findFreePort()
        val tempDir = File(System.getProperty("java.io.tmpdir"), "meld_auth_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val loginUrl = "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F"

        val pb = ProcessBuilder(
            browserExe,
            "--app=$loginUrl",
            "--remote-debugging-port=$port",
            "--remote-allow-origins=*",
            "--user-data-dir=${tempDir.absolutePath}",
            "--window-size=500,750"
        )

        val process = try {
            pb.start()
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            return@withContext Result.failure(e)
        }

        onProgress("Spotify login window opened. Please sign in...")

        val httpClient = HttpClient.newHttpClient()
        var spDcCookie: String? = null

        try {
            val timeoutSec = 300 // 5 minutes max wait
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutSec * 1000) {
                if (!process.isAlive) {
                    break
                }

                delay(1500)

                try {
                    val listReq = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:$port/json/list"))
                        .GET()
                        .build()
                    val listResp = httpClient.send(listReq, HttpResponse.BodyHandlers.ofString())

                    val pages = json.parseToJsonElement(listResp.body()).jsonArray
                    val spotifyPage = pages.firstOrNull { page ->
                        val url = page.jsonObject["url"]?.jsonPrimitive?.content ?: ""
                        url.contains("spotify.com")
                    } ?: pages.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.content == "page" }

                    val wsUrl = spotifyPage?.jsonObject?.get("webSocketDebuggerUrl")?.jsonPrimitive?.content

                    if (!wsUrl.isNullOrBlank()) {
                        val cookie = queryCookiesViaWebSocket(httpClient, wsUrl)
                        if (!cookie.isNullOrBlank()) {
                            spDcCookie = cookie
                            println("[DesktopSpotifyAuth] Successfully captured sp_dc cookie!")
                            break
                        }
                    }
                } catch (_: Exception) {
                    // Browser or CDP might still be booting up
                }
            }
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
            }
            try {
                tempDir.deleteRecursively()
            } catch (_: Exception) {}
        }

        if (!spDcCookie.isNullOrBlank()) {
            Result.success(spDcCookie)
        } else {
            Result.failure(IllegalStateException("Login window closed or session not captured."))
        }
    }

    private fun queryCookiesViaWebSocket(client: HttpClient, wsUrl: String): String? {
        val future = CompletableFuture<String?>()
        try {
            client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), object : WebSocket.Listener {
                    override fun onOpen(webSocket: WebSocket) {
                        val msg = """{"id":1,"method":"Network.getCookies","params":{"urls":["https://open.spotify.com"]}}"""
                        webSocket.sendText(msg, true)
                        webSocket.request(1)
                    }

                    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                        try {
                            val element = json.parseToJsonElement(data.toString())
                            val cookies = element.jsonObject["result"]
                                ?.jsonObject?.get("cookies")
                                ?.jsonArray ?: emptyList()

                            val spDc = cookies.firstOrNull {
                                it.jsonObject["name"]?.jsonPrimitive?.content == "sp_dc"
                            }?.jsonObject?.get("value")?.jsonPrimitive?.content

                            future.complete(spDc)
                        } catch (e: Exception) {
                            future.complete(null)
                        }
                        webSocket.request(1)
                        return null
                    }

                    override fun onError(webSocket: WebSocket?, error: Throwable?) {
                        future.complete(null)
                    }
                })
            return future.get(3, TimeUnit.SECONDS)
        } catch (_: Exception) {
            return null
        }
    }
}
