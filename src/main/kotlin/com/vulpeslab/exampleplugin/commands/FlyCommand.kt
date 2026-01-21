package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.DatabaseManager
import java.util.concurrent.CompletableFuture

/**
 * Toggles flight mode for the player.
 * Usage: /fly
 * Permission: vulpeslab.exampleplugin.command.fly (auto-generated)
 *
 * State is persisted to database and restored on player join.
 * Fall damage is automatically prevented while flying via FlyFallDamageFilterSystem.
 */
class FlyCommand : AbstractAsyncPlayerCommand("fly", "exampleplugin.commands.fly.description") {

    override fun executeAsync(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ): CompletableFuture<Void> {
        val uuid = playerRef.uuid

        // Get movement manager to toggle flight
        val movementManager = store.getComponent(ref, MovementManager.getComponentType())
            ?: return CompletableFuture.completedFuture(null)

        // Toggle canFly
        val settings = movementManager.settings
        settings.canFly = !settings.canFly
        val enabling = settings.canFly

        // Send updated settings to client
        movementManager.update(playerRef.packetHandler)

        // Send feedback message
        val messageKey = if (enabling) {
            "exampleplugin.commands.fly.enabled"
        } else {
            "exampleplugin.commands.fly.disabled"
        }
        playerRef.sendMessage(Message.translation(messageKey))

        // Persist state to database
        return DatabaseManager.setFlyModeAsync(uuid, enabling).thenAccept { }
    }
}
