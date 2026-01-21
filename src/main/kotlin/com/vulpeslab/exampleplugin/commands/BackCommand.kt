package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.DeathPositionManager

/**
 * Teleports the player back to their last death location.
 * Usage: /back
 */
class BackCommand : AbstractPlayerCommand("back", "exampleplugin.commands.back.description") {

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        // Get player UUID
        val player = store.getComponent(ref, Player.getComponentType())
        @Suppress("DEPRECATION")
        val uuid = player?.getUuid()

        if (uuid == null) {
            playerRef.sendMessage(Message.translation("exampleplugin.commands.back.error"))
            return
        }

        // Get death position
        val deathLocation = DeathPositionManager.getDeathPosition(uuid)

        if (deathLocation == null) {
            playerRef.sendMessage(Message.translation("exampleplugin.commands.back.nodeath"))
            return
        }

        // Check if same world - cross-world teleport would require additional handling
        if (world.name != deathLocation.worldName) {
            playerRef.sendMessage(Message.translation("exampleplugin.commands.back.differentworld")
                .param("world", deathLocation.worldName))
            return
        }

        // Teleport the player to their death location
        val teleport = Teleport.createForPlayer(
            deathLocation.position,
            deathLocation.rotation
        )

        // Add teleport component to trigger the teleport
        store.addComponent(ref, Teleport.getComponentType(), teleport)

        // Clear the death position after teleporting
        DeathPositionManager.clearDeathPosition(uuid)

        // Send success message
        playerRef.sendMessage(Message.translation("exampleplugin.commands.back.success"))
    }
}
