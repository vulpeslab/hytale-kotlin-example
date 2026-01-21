package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.HologramMarker

/**
 * ECS system that rotates holograms to face viewing players (billboard effect).
 *
 * This system runs every tick and updates the rotation of hologram entities
 * so they always face the player looking at them.
 */
class HologramBillboardSystem : EntityTickingSystem<EntityStore>() {

    companion object {
        private val QUERY: Query<EntityStore> by lazy {
            Query.and(
                HologramMarker.getComponentType(),
                TransformComponent.getComponentType(),
                EntityTrackerSystems.Visible.getComponentType()
            )
        }
    }

    override fun getGroup(): SystemGroup<EntityStore>? {
        return EntityTrackerSystems.QUEUE_UPDATE_GROUP
    }

    override fun getQuery(): Query<EntityStore> {
        return QUERY
    }

    override fun tick(
        dt: Float,
        index: Int,
        archetypeChunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val visibleComponent = archetypeChunk.getComponent(index, EntityTrackerSystems.Visible.getComponentType())
            ?: return
        val transformComponent = archetypeChunk.getComponent(index, TransformComponent.getComponentType())
            ?: return

        val holoPosition = transformComponent.position

        // For each viewer, we could update individually, but for simplicity
        // we'll rotate toward the closest/first viewer
        val viewers = visibleComponent.visibleTo.keys
        if (viewers.isEmpty()) return

        // Find the first valid player viewer and rotate toward them
        for (viewerRef in viewers) {
            if (!viewerRef.isValid) continue

            val viewerTransform = store.getComponent(viewerRef, TransformComponent.getComponentType())
                ?: continue
            val player = store.getComponent(viewerRef, Player.getComponentType())
                ?: continue

            val playerPosition = viewerTransform.position

            // Calculate direction from hologram to player
            val relative = Vector3d(
                playerPosition.x - holoPosition.x,
                playerPosition.y - holoPosition.y,
                playerPosition.z - holoPosition.z
            )

            // Calculate rotation that faces the player
            val lookRotation = Vector3f.lookAt(relative)

            // Update hologram rotation (only yaw, keeping it upright)
            val currentRotation = transformComponent.rotation
            if (Math.abs(currentRotation.yaw - lookRotation.yaw) > 0.01f) {
                transformComponent.rotation.setYaw(lookRotation.yaw)
            }

            // Only rotate toward first viewer for simplicity
            break
        }
    }
}
