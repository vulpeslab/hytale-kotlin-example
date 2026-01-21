package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager
import com.hypixel.hytale.server.core.util.PositionUtil
import com.vulpeslab.exampleplugin.services.WaypointManager

/**
 * Provides map markers for waypoints.
 * Players only see waypoint markers for waypoints they have permission to use.
 */
class WaypointMapMarkerProvider : WorldMapManager.MarkerProvider {

    companion object {
        val INSTANCE = WaypointMapMarkerProvider()
        private const val MARKER_IMAGE = "Waypoint.png"
        private const val PERMISSION_BASE = "vulpeslab.exampleplugin.waypoint"
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

        // Get all waypoints for this world
        val waypoints = WaypointManager.getAllWaypoints().filter { it.worldName == world.name }

        for (waypoint in waypoints) {
            // Check if player has permission to see this waypoint
            val permission = "$PERMISSION_BASE.${waypoint.name.lowercase()}"
            if (!player.hasPermission(permission)) {
                continue
            }

            val markerId = "waypoint_${waypoint.name.lowercase()}"
            val position = Vector3d(waypoint.x, waypoint.y, waypoint.z)
            // Use fixed rotation (0) so markers always point upward on the map
            val rotation = Vector3f(0f, 0f, 0f)

            tracker.trySendMarker(
                chunkViewRadius,
                playerChunkX,
                playerChunkZ,
                position,
                0f, // Fixed rotation for map display
                markerId,
                waypoint.name,
                position
            ) { id, name, pos ->
                MapMarker(
                    id,
                    name,
                    MARKER_IMAGE,
                    PositionUtil.toTransformPacket(Transform(pos, rotation)),
                    null
                )
            }
        }
    }
}
