package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefChangeSystem
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.DeathPositionManager

/**
 * Listens for player deaths and saves their death position.
 * This allows the /back command to teleport players to where they died.
 */
class PlayerDeathPositionSystem : RefChangeSystem<EntityStore, DeathComponent>() {

    override fun getQuery(): Query<EntityStore> {
        return Player.getComponentType()
    }

    override fun componentType(): ComponentType<EntityStore, DeathComponent> {
        return DeathComponent.getComponentType()
    }

    override fun onComponentAdded(
        ref: Ref<EntityStore>,
        component: DeathComponent,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        // Get player component to access UUID
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        @Suppress("DEPRECATION")
        val uuid = player.getUuid() ?: return

        // Get the player's position at death
        val transform = store.getComponent(ref, TransformComponent.getComponentType()) ?: return
        val position = transform.position

        // Get the player's head rotation for facing direction
        val headRotation = store.getComponent(ref, HeadRotation.getComponentType())
        val rotation = headRotation?.rotation ?: transform.rotation

        // Get the world
        val world = commandBuffer.externalData.world

        // Save the death position
        DeathPositionManager.saveDeathPosition(uuid, world, position, rotation)
    }

    override fun onComponentSet(
        ref: Ref<EntityStore>,
        oldComponent: DeathComponent?,
        newComponent: DeathComponent,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        // Not needed - we only care about initial death
    }

    override fun onComponentRemoved(
        ref: Ref<EntityStore>,
        component: DeathComponent,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        // Not needed
    }
}
