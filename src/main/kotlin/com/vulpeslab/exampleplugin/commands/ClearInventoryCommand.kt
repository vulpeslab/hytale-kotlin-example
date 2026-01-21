package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

/**
 * Clears the player's inventory.
 * Usage: /clearinventory or /ci
 */
class ClearInventoryCommand : AbstractPlayerCommand("clearinventory", "exampleplugin.commands.clearinventory.description") {

    init {
        addAliases("ci")
    }

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        requireNotNull(player)

        player.inventory.clear()

        playerRef.sendMessage(Message.translation("exampleplugin.commands.clearinventory.success"))
    }
}
