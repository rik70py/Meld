package com.meld.desktop.player

import com.metrolist.innertube.YouTube
import com.metrolist.innertubex.InnerTubeLogLevel
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.ExtractedStream
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.YtConfigParser
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Desktop stream extractor using InnerTubeExtractor and YouTubeCipherService,
 * matching the robust Android extraction pipeline with local file caching.
 */
object DesktopExtractor {
    private const val TAG = "DesktopExtractor"

    private val bundleMutex = Mutex()
    @Volatile
    private var currentBundle: ExtractionBundle? = null

    private data class ExtractionBundle(
        val transportGeneration: Long,
        val cipherService: YouTubeCipherService,
        val extractor: InnerTubeExtractor,
    )

    private val logger = InnerTubeLogger { event ->
        val details = event.details.entries.joinToString(prefix = " [", postfix = "]") { "${it.key}=${it.value}" }
        val message = event.message + details.takeUnless { event.details.isEmpty() }.orEmpty()
        println("[$TAG][${event.level}] ${event.tag}: $message")
    }

    private val configRepository = object : PlayerConfigRepository {
        private val configFile = File(System.getProperty("user.home"), ".meld/player_config.properties")
        private val props = java.util.Properties().apply {
            if (configFile.exists()) {
                runCatching { configFile.inputStream().use { load(it) } }
            }
        }

        override val enabled: Boolean = true
        override val sourceUrl: String = "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
        override val defaultSourceUrl: String = sourceUrl

        override var cachedJson: String
            get() = props.getProperty("json", "")
            set(value) {
                props.setProperty("json", value)
                save()
            }

        override var cachedAtMs: Long
            get() = props.getProperty("cached_at_ms", "0").toLongOrNull() ?: 0L
            set(value) {
                props.setProperty("cached_at_ms", value.toString())
                save()
            }

        override var cachedSourceUrl: String
            get() = props.getProperty("source_url", "")
            set(value) {
                props.setProperty("source_url", value)
                save()
            }

        override var cachedEtag: String
            get() = props.getProperty("etag", "")
            set(value) {
                props.setProperty("etag", value)
                save()
            }

        private fun save() {
            runCatching {
                configFile.parentFile?.mkdirs()
                configFile.outputStream().use { props.store(it, null) }
            }
        }
    }

    private val noOpTokenProvider = object : TokenProvider {
        override val capabilities = TokenProviderCapabilities(providers = emptySet(), usesWebView = false)
        override suspend fun getPoToken(videoId: String, visitorData: String, cookie: String?): PoTokenResult? = null
        override suspend fun close() {}
    }

    private fun YtConfigParser.withEmbeddedConfigFallback(): YtConfigParser =
        object : YtConfigParser by this {
            override suspend fun fetchConfig(videoId: String, useLoginCookies: Boolean) =
                try {
                    this@withEmbeddedConfigFallback.fetchConfig(videoId, useLoginCookies)
                } catch (_: IllegalStateException) {
                    this@withEmbeddedConfigFallback.fetchEmbeddedConfig(videoId, useLoginCookies = false)
                }
        }

    private suspend fun bundle(): ExtractionBundle {
        val transport = YouTube.extractionTransport()
        currentBundle?.takeIf { it.transportGeneration == transport.generation }?.let { return it }
        return bundleMutex.withLock {
            val latestTransport = YouTube.extractionTransport()
            currentBundle?.takeIf { it.transportGeneration == latestTransport.generation }?.let { return@withLock it }
            try {
                currentBundle?.cipherService?.dispose()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}

            val remoteStore = RemotePlayerConfigStore(latestTransport.httpClient, configRepository, logger)
            val cipherService = YouTubeCipherService(latestTransport.httpClient, remoteStore, logger)
            val extractor = InnerTubeExtractor(
                configParser = YtConfigParserImpl(
                    latestTransport.httpClient,
                    latestTransport.innerTube,
                    remoteStore,
                    logger,
                ).withEmbeddedConfigFallback(),
                cipherService = cipherService,
                innerTube = latestTransport.innerTube,
                tokenProvider = noOpTokenProvider,
                logger = logger,
            )
            ExtractionBundle(latestTransport.generation, cipherService, extractor).also { currentBundle = it }
        }
    }

    data class ExtractedAudio(
        val url: String,
        val headers: Map<String, String>,
        val client: String
    )

    suspend fun extractStream(videoId: String): ExtractedAudio? {
        return try {
            val hints = ContentHints().withStreamCapabilities(
                allowHls = false,
                allowSabr = false,
                allowBoundedRange = false,
            )
            val stream: ExtractedStream? = bundle().extractor.extract(
                videoId = videoId,
                hints = hints,
                excludedClients = emptySet(),
                clientPlaybackNonce = generateClientPlaybackNonce(),
            )
            if (stream?.audioUrl != null) {
                println("[$TAG] Successfully extracted stream for videoId=$videoId via client=${stream.clientName}")
                ExtractedAudio(
                    url = stream.audioUrl,
                    headers = stream.headers,
                    client = stream.clientName
                )
            } else {
                println("[$TAG] Extractor returned no playable stream for videoId=$videoId")
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("[$TAG] Extraction failed for videoId=$videoId: ${e.message}")
            null
        }
    }
}
