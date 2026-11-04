package com.meld.desktop.state

import com.metrolist.spotify.Spotify
import com.metrolist.spotify.SpotifyAuth
import com.metrolist.spotify.models.SpotifyUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages desktop Spotify authentication and session persistence.
 * Uses the sp_dc cookie to fetch internal Spotify tokens without
 * requiring developer API keys.
 */
class SpotifySession(private val scope: CoroutineScope) {

    private val configFile = File(System.getProperty("user.home"), ".meld/spotify_session.txt")

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<SpotifyUser?>(null)
    val currentUser: StateFlow<SpotifyUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSavedSession()
    }

    private fun loadSavedSession() {
        if (configFile.exists()) {
            val savedSpDc = configFile.readText().trim()
            if (savedSpDc.isNotBlank()) {
                connectWithSpDc(savedSpDc)
            }
        }
    }

    fun connectWithSpDc(spDcCookie: String) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val tokenResult = withContext(Dispatchers.IO) {
                    SpotifyAuth.fetchAccessToken(spDcCookie).getOrNull()
                }

                if (tokenResult != null) {
                    Spotify.accessToken = tokenResult.accessToken
                    _isLoggedIn.value = true

                    // Persist for next launch
                    withContext(Dispatchers.IO) {
                        configFile.parentFile?.mkdirs()
                        configFile.writeText(spDcCookie)
                    }

                    // Fetch user profile
                    val profile = withContext(Dispatchers.IO) {
                        Spotify.me().getOrNull()
                    }
                    _currentUser.value = profile
                    println("[SpotifySession] Successfully authenticated: ${profile?.displayName ?: "User"}")
                } else {
                    _errorMessage.value = "Failed to obtain token from Spotify. Please check the sp_dc cookie."
                    _isLoggedIn.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error connecting: ${e.message}"
                _isLoggedIn.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        Spotify.accessToken = null
        _isLoggedIn.value = false
        _currentUser.value = null
        if (configFile.exists()) {
            configFile.delete()
        }
    }
}
