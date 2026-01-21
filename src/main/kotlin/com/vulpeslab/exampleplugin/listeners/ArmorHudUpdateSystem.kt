package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.ArmorHudManager

/**
 * System that periodically updates the armor HUD for players.
 */
class ArmorHudUpdateSystem : EntityTickingSystem<EntityStore>() {

    companion object {
        private val QUERY: Query<EntityStore> = Query.and(
            Player.getComponentType(),
            PlayerRef.getComponentType()
        )
        private const val UPDATE_INTERVAL = 10
    }

    private var tickCounter = 0

    override fun getQuery(): Query<EntityStore> = QUERY

    override fun tick(
        dt: Float,
        index: Int,
        archetypeChunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        tickCounter++
        if (tickCounter < UPDATE_INTERVAL) return
        tickCounter = 0

        val ref = archetypeChunk.getReferenceTo(index)
        val player = archetypeChunk.getComponent(index, Player.getComponentType()) ?: return
        val playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType()) ?: return
        @Suppress("DEPRECATION")
        val uuid = player.getUuid() ?: return

        if (!ArmorHudManager.isHudEnabled(uuid)) {
            ArmorHudManager.enableHud(uuid, playerRef, ref, store)
        } else {
            ArmorHudManager.refreshHud(uuid)
        }
    }
}
