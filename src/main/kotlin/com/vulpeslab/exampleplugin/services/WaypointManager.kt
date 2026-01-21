package com.vulpeslab.exampleplugin.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a waypoint location that players can teleport to.
 */
data class Waypoint(
    val name: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f
)

/**
 * Manages waypoint data storage and retrieval.
 * Waypoints are stored in a JSON file in the plugin data directory.
 */
object WaypointManager {
    private lateinit var waypointsFile: File
    private val waypoints = ConcurrentHashMap<String, Waypoint>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Initializes the WaypointManager with the plugin's data directory.
     */
    fun initialize(dataDirectory: Path) {
        waypointsFile = dataDirectory.resolve("waypoints.json").toFile()
        loadWaypoints()
    }

    /**
     * Shuts down the WaypointManager.
     */
    fun shutdown() {
        // Save any pending changes
        saveWaypoints()
    }

    /**
     * Loads waypoints from the JSON file.
     */
    private fun loadWaypoints() {
        if (!waypointsFile.exists()) {
            return
        }
        try {
            val type = object : TypeToken<Map<String, Waypoint>>() {}.type
            val loadedWaypoints: Map<String, Waypoint>? = gson.fromJson(waypointsFile.readText(), type)
            loadedWaypoints?.let {
                waypoints.clear()
                waypoints.putAll(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves waypoints to the JSON file.
     */
    private fun saveWaypoints() {
        try {
            waypointsFile.parentFile?.mkdirs()
            waypointsFile.writeText(gson.toJson(waypoints))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gets all waypoints synchronously.
     * Used by map marker provider which runs on the world map thread.
     */
    fun getAllWaypoints(): List<Waypoint> = waypoints.values.toList()

    /**
     * Gets all waypoints asynchronously.
     */
    fun getAllWaypointsAsync(): CompletableFuture<List<Waypoint>> {
        return CompletableFuture.supplyAsync { getAllWaypoints() }
    }

    /**
     * Gets a waypoint by name.
     */
    fun getWaypoint(name: String): Waypoint? = waypoints[name.lowercase()]

    /**
     * Gets a waypoint by name asynchronously.
     */
    fun getWaypointAsync(name: String): CompletableFuture<Waypoint?> {
        return CompletableFuture.supplyAsync { getWaypoint(name) }
    }

    /**
     * Creates a new waypoint.
     * Returns null if a waypoint with that name already exists.
     */
    fun createWaypoint(name: String, worldName: String, x: Double, y: Double, z: Double, yaw: Float): Waypoint? {
        val key = name.lowercase()
        if (waypoints.containsKey(key)) {
            return null
        }
        val waypoint = Waypoint(name, worldName, x, y, z, yaw)
        waypoints[key] = waypoint
        saveWaypoints()
        return waypoint
    }

    /**
     * Creates a new waypoint asynchronously.
     */
    fun createWaypointAsync(name: String, worldName: String, x: Double, y: Double, z: Double, yaw: Float): CompletableFuture<Waypoint?> {
        return CompletableFuture.supplyAsync { createWaypoint(name, worldName, x, y, z, yaw) }
    }

    /**
     * Updates an existing waypoint's position.
     * Returns true if the waypoint was found and updated.
     */
    fun updateWaypoint(name: String, worldName: String, x: Double, y: Double, z: Double, yaw: Float): Boolean {
        val key = name.lowercase()
        if (!waypoints.containsKey(key)) {
            return false
        }
        val waypoint = Waypoint(name, worldName, x, y, z, yaw)
        waypoints[key] = waypoint
        saveWaypoints()
        return true
    }

    /**
     * Updates an existing waypoint asynchronously.
     */
    fun updateWaypointAsync(name: String, worldName: String, x: Double, y: Double, z: Double, yaw: Float): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { updateWaypoint(name, worldName, x, y, z, yaw) }
    }

    /**
     * Deletes a waypoint by name.
     * Returns true if the waypoint was found and deleted.
     */
    fun deleteWaypoint(name: String): Boolean {
        val removed = waypoints.remove(name.lowercase()) != null
        if (removed) {
            saveWaypoints()
        }
        return removed
    }

    /**
     * Deletes a waypoint asynchronously.
     */
    fun deleteWaypointAsync(name: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { deleteWaypoint(name) }
    }

    /**
     * Checks if a waypoint exists.
     */
    fun waypointExists(name: String): Boolean = waypoints.containsKey(name.lowercase())
}
