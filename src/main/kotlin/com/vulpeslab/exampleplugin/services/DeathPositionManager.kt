package com.vulpeslab.exampleplugin.services

import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.universe.world.World
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores the last death position for each player.
 * Used by the /back command to teleport players to their death location.
 */
object DeathPositionManager {

    /**
     * Represents a saved death location.
     */
    data class DeathLocation(
        val worldName: String,
        val position: Vector3d,
        val rotation: Vector3f
    )

    private val deathPositions = ConcurrentHashMap<UUID, DeathLocation>()

    /**
     * Saves a player's death position.
     */
    fun saveDeathPosition(uuid: UUID, world: World, position: Vector3d, rotation: Vector3f) {
        deathPositions[uuid] = DeathLocation(
            worldName = world.name,
            position = Vector3d(position.x, position.y, position.z),
            rotation = Vector3f(rotation.pitch, rotation.yaw, rotation.roll)
        )
    }

    /**
     * Gets a player's last death position, if any.
     */
    fun getDeathPosition(uuid: UUID): DeathLocation? {
        return deathPositions[uuid]
    }

    /**
     * Clears a player's death position (e.g., after teleporting back).
     */
    fun clearDeathPosition(uuid: UUID) {
        deathPositions.remove(uuid)
    }

    /**
     * Clears all stored death positions.
     */
    fun clear() {
        deathPositions.clear()
    }
}
