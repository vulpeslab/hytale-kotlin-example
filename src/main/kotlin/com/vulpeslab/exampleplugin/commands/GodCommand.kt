package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.DatabaseManager
import java.util.concurrent.CompletableFuture

/**
 * Toggles god mode (invulnerability) for the player.
 * Usage: /god
 * Permission: vulpeslab.exampleplugin.command.god (auto-generated)
 *
 * State is persisted to database and restored on player join.
 */
class GodCommand : AbstractAsyncPlayerCommand("god", "exampleplugin.commands.god.description") {

    override fun executeAsync(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ): CompletableFuture<Void> {
        val uuid = playerRef.uuid

        // Check if player currently has the Invulnerable component
        val hasInvulnerable = store.getArchetype(ref).contains(Invulnerable.getComponentType())

        // Toggle: if has invulnerable, remove it; otherwise add it
        val enabling = !hasInvulnerable

        // Apply the component change on the world thread
        world.execute {
            if (enabling) {
                store.ensureComponent(ref, Invulnerable.getComponentType())
            } else {
                store.tryRemoveComponent(ref, Invulnerable.getComponentType())
            }
        }

        // Persist state to database and send feedback
        return DatabaseManager.setGodModeAsync(uuid, enabling).thenAccept {
            val messageKey = if (enabling) {
                "exampleplugin.commands.god.enabled"
            } else {
                "exampleplugin.commands.god.disabled"
            }
            playerRef.sendMessage(Message.translation(messageKey))
        }
    }
}
