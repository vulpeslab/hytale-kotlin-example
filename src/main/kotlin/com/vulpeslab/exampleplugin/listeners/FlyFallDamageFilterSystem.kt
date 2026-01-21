package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

/**
 * Filters fall damage for players who have flight enabled via /fly command.
 *
 * This system runs in the FilterDamageGroup and cancels fall damage
 * when a player has canFly enabled in their movement settings.
 */
class FlyFallDamageFilterSystem : DamageEventSystem() {

    companion object {
        private val QUERY: Query<EntityStore> = Query.and(
            Player.getComponentType(),
            MovementManager.getComponentType()
        )
    }

    override fun getGroup(): SystemGroup<EntityStore>? {
        return DamageModule.get().filterDamageGroup
    }

    override fun getQuery(): Query<EntityStore> {
        return QUERY
    }

    override fun handle(
        index: Int,
        archetypeChunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        damage: Damage
    ) {
        // Only filter fall damage
        @Suppress("DEPRECATION")
        val damageCause = damage.getCause() ?: return
        @Suppress("DEPRECATION")
        if (damageCause !== DamageCause.FALL) return

        // Check if player has flight enabled
        val movementManager = archetypeChunk.getComponent(index, MovementManager.getComponentType())
            ?: return

        if (movementManager.settings.canFly) {
            damage.isCancelled = true
        }
    }
}
