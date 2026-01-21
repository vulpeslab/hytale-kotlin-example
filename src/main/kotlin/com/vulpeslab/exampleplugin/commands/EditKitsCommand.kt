package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.ui.KitListUiPage

/**
 * Opens the kit list UI.
 * Usage: /editkits
 * Permission: vulpeslab.exampleplugin.command.editkits (auto-generated)
 */
class EditKitsCommand : AbstractPlayerCommand("editkits", "exampleplugin.commands.editkits.description") {

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        requireNotNull(player)

        player.pageManager.openCustomPage(ref, store, KitListUiPage(playerRef))
    }
}
