package com.vulpeslab.exampleplugin.services

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Checks for plugin updates from GitHub releases.
 * Caches the result for 15 minutes to avoid overloading the GitHub API.
 */
object UpdateChecker {
    const val PLUGIN_NAME = "ExamplePlugin"
    const val PLUGIN_AUTHOR = "VulpesLab"
    const val GITHUB_REPO = "vulpeslab/hytale-kotlin-example"
    const val GITHUB_URL = "https://github.com/$GITHUB_REPO"
    const val GITHUB_RELEASES_URL = "$GITHUB_URL/releases"

    const val UPDATE_NOTIFY_PERMISSION = "vulpeslab.exampleplugin.updatenotify"

    /**
     * Plugin version, loaded from manifest.json (injected by gradle build).
     */
    val PLUGIN_VERSION: String by lazy {
        try {
            val manifestContent = UpdateChecker::class.java.getResourceAsStream("/manifest.json")?.bufferedReader()?.use {
                it.readText()
            } ?: return@lazy "unknown"
            
            // Parse version from JSON manually to avoid dependencies
            val versionPattern = """"Version"\s*:\s*"([^"]+)"""".toRegex()
            versionPattern.find(manifestContent)?.groupValues?.get(1) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private const val CACHE_DURATION_MINUTES = 15L
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ExamplePlugin-UpdateChecker").apply { isDaemon = true }
    }

    // Cached update info
    private val cachedLatestVersion = AtomicReference<String?>(null)
    private var cacheExpireTime: Long = 0

    data class UpdateInfo(
        val currentVersion: String,
        val latestVersion: String?,
        val isUpdateAvailable: Boolean,
        val error: String? = null
    )

    /**
     * Checks for updates asynchronously.
     * Uses cached result if available and not expired.
     */
    fun checkForUpdateAsync(): CompletableFuture<UpdateInfo> {
        return CompletableFuture.supplyAsync({
            checkForUpdateSync()
        }, executor)
    }

    /**
     * Gets cached update info if available, otherwise returns null.
     * Does not make a network request.
     */
    fun getCachedUpdateInfo(): UpdateInfo? {
        val cachedVersion = cachedLatestVersion.get()
        if (cachedVersion != null && System.currentTimeMillis() < cacheExpireTime) {
            return UpdateInfo(
                currentVersion = PLUGIN_VERSION,
                latestVersion = cachedVersion,
                isUpdateAvailable = isNewerVersion(cachedVersion, PLUGIN_VERSION)
            )
        }
        return null
    }

    /**
     * Checks if an update is available using cached data.
     * Returns null if cache is expired or not available.
     */
    fun isCachedUpdateAvailable(): Boolean? {
        return getCachedUpdateInfo()?.isUpdateAvailable
    }

    private fun checkForUpdateSync(): UpdateInfo {
        // Check cache first
        val cachedVersion = cachedLatestVersion.get()
        if (cachedVersion != null && System.currentTimeMillis() < cacheExpireTime) {
            return UpdateInfo(
                currentVersion = PLUGIN_VERSION,
                latestVersion = cachedVersion,
                isUpdateAvailable = isNewerVersion(cachedVersion, PLUGIN_VERSION)
            )
        }

        // Fetch from GitHub API
        return try {
            val latestVersion = fetchLatestVersionFromGitHub()
            
            // Update cache
            cachedLatestVersion.set(latestVersion)
            cacheExpireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(CACHE_DURATION_MINUTES)

            UpdateInfo(
                currentVersion = PLUGIN_VERSION,
                latestVersion = latestVersion,
                isUpdateAvailable = isNewerVersion(latestVersion, PLUGIN_VERSION)
            )
        } catch (e: Exception) {
            UpdateInfo(
                currentVersion = PLUGIN_VERSION,
                latestVersion = null,
                isUpdateAvailable = false,
                error = e.message
            )
        }
    }

    private fun fetchLatestVersionFromGitHub(): String {
        val url = URI(API_URL).toURL()
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "ExamplePlugin-UpdateChecker")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != 200) {
                throw RuntimeException("GitHub API returned ${connection.responseCode}")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }

            // Parse JSON manually to avoid adding dependencies
            // Looking for "tag_name": "v1.2.3" pattern
            val tagNamePattern = """"tag_name"\s*:\s*"([^"]+)"""".toRegex()
            val match = tagNamePattern.find(response)
                ?: throw RuntimeException("Could not parse tag_name from response")

            val tagName = match.groupValues[1]
            // Remove 'v' prefix if present
            return tagName.removePrefix("v")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Compares two semantic version strings.
     * Returns true if latestVersion is newer than currentVersion.
     */
    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        val latestParts = latestVersion.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val latest = latestParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }
            if (latest > current) return true
            if (latest < current) return false
        }
        return false
    }

    /**
     * Shuts down the executor service.
     */
    fun shutdown() {
        executor.shutdown()
    }
}
