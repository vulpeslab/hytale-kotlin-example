package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.Page
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow
import com.hypixel.hytale.server.core.inventory.container.DelegateItemContainer
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

/**
 * View and optionally modify another player's inventory.
 * Usage: /invsee <player>
 * Permission: vulpeslab.exampleplugin.command.invsee (auto-generated, view only)
 * Permission: vulpeslab.exampleplugin.command.invsee.modify (required to move items)
 */
class InvSeeCommand : AbstractPlayerCommand("invsee", "exampleplugin.commands.invsee.description") {

    companion object {
        private const val PERMISSION_MODIFY = "vulpeslab.exampleplugin.command.invsee.modify"
    }

    private val targetPlayerArg: RequiredArg<PlayerRef> = withRequiredArg(
        "player",
        "exampleplugin.commands.invsee.player.desc",
        ArgTypes.PLAYER_REF
    )

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        requireNotNull(player)

        val targetPlayerRef = targetPlayerArg.get(context)
        val targetRef = targetPlayerRef.reference

        if (targetRef == null || !targetRef.isValid) {
            context.sendMessage(Message.translation("exampleplugin.commands.invsee.notfound"))
            return
        }

        val targetStore = targetRef.store
        val targetWorld = targetStore.externalData.world

        // Execute on target's world thread for thread safety
        targetWorld.execute {
            val targetPlayer = targetStore.getComponent(targetRef, Player.getComponentType())
            if (targetPlayer == null) {
                context.sendMessage(Message.translation("exampleplugin.commands.invsee.notfound"))
                return@execute
            }

            // Get target's combined inventory (hotbar + storage)
            var targetInventory: ItemContainer = targetPlayer.inventory.combinedHotbarFirst

            // If player doesn't have modify permission, make it read-only
            if (!context.sender().hasPermission(PERMISSION_MODIFY)) {
                val readOnlyContainer = DelegateItemContainer(targetPlayer.inventory.combinedHotbarFirst)
                readOnlyContainer.setGlobalFilter(FilterType.DENY_ALL)
                targetInventory = readOnlyContainer
            }

            // Open the inventory view using the Bench page
            player.pageManager.setPageWithWindows(
                ref,
                store,
                Page.Bench,
                true,
                ContainerWindow(targetInventory)
            )
        }
    }
}
