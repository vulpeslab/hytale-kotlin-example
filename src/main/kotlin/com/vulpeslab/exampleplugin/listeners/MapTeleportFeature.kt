package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapSettings
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Enables map marker teleportation for players with the mapteleport permission.
 * This allows clicking on ANY map marker to teleport, even in Adventure mode.
 *
 * Permission: vulpeslab.exampleplugin.mapteleport
 */
class MapTeleportFeature : WorldMapManager.MarkerProvider {

    companion object {
        val INSTANCE = MapTeleportFeature()
        private const val PERMISSION = "vulpeslab.exampleplugin.mapteleport"

        // Track players who have been sent the teleport override
        private val playersWithTeleportEnabled = ConcurrentHashMap.newKeySet<UUID>()

        fun clearPlayer(uuid: UUID) {
            playersWithTeleportEnabled.remove(uuid)
        }

        fun clearAll() {
            playersWithTeleportEnabled.clear()
        }
    }

    override fun update(
        world: World,
        gameplayConfig: GameplayConfig,
        tracker: WorldMapTracker,
        chunkViewRadius: Int,
        playerChunkX: Int,
        playerChunkZ: Int
    ) {
        val player = tracker.player
        @Suppress("DEPRECATION")
        val playerUuid = player.getUuid() ?: return

        // Enable teleport to markers for players with the mapteleport permission
        // This overrides the Adventure mode restriction but affects ALL markers
        if (player.hasPermission(PERMISSION) && !playersWithTeleportEnabled.contains(playerUuid)) {
            playersWithTeleportEnabled.add(playerUuid)

            // Get current world map settings and enable marker teleportation
            val worldMapSettings = world.worldMapManager.worldMapSettings
            val settingsPacket = UpdateWorldMapSettings(worldMapSettings.settingsPacket)
            settingsPacket.allowTeleportToMarkers = true

            @Suppress("DEPRECATION")
            player.getPlayerConnection().write(settingsPacket)
        }
    }
}
