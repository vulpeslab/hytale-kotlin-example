package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.ui.WaypointsUiPage

/**
 * Opens the waypoints management UI.
 * Usage: /waypoints
 * Permission: vulpeslab.exampleplugin.command.waypoints (auto-generated, admin only)
 */
class WaypointsCommand : AbstractPlayerCommand("waypoints", "exampleplugin.commands.waypoints.description") {

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        requireNotNull(player)

        player.pageManager.openCustomPage(ref, store, WaypointsUiPage(playerRef))
    }
}
