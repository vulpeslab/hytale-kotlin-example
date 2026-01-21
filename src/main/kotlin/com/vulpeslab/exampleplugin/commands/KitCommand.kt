package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.KitManager

/**
 * Gives a kit to the player.
 * Usage: /kit <name>
 * Permission: vulpeslab.exampleplugin.command.kit (auto-generated base permission)
 * Permission: vulpeslab.exampleplugin.command.kit.<kitname> (required for each specific kit)
 */
class KitCommand : AbstractPlayerCommand("kit", "exampleplugin.commands.kit.description") {

    companion object {
        private const val PERMISSION_BASE = "vulpeslab.exampleplugin.command.kit"
    }

    private val kitNameArg: RequiredArg<String> = withRequiredArg(
        "name",
        "exampleplugin.commands.kit.name.desc",
        ArgTypes.STRING
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

        val kitName = kitNameArg.get(context)

        // Check kit-specific permission
        val kitPermission = "$PERMISSION_BASE.${kitName.lowercase()}"
        if (!context.sender().hasPermission(kitPermission)) {
            context.sendMessage(Message.translation("exampleplugin.commands.kit.nopermission")
                .param("name", kitName))
            return
        }

        KitManager.getKitAsync(kitName).thenAccept { kit ->
            world.execute {
                if (kit == null) {
                    context.sendMessage(Message.translation("exampleplugin.commands.kit.notfound")
                        .param("name", kitName))
                    return@execute
                }

                if (kit.items.isEmpty()) {
                    context.sendMessage(Message.translation("exampleplugin.commands.kit.empty")
                        .param("name", kit.name))
                    return@execute
                }

                val inventory = player.inventory.combinedHotbarFirst
                var givenCount = 0

                for (kitItem in kit.items) {
                    val itemStack = ItemStack(kitItem.itemId, kitItem.quantity, null)
                    val transaction = inventory.addItemStack(itemStack)
                    val remainder = transaction.remainder
                    if (remainder == null || remainder.isEmpty) {
                        givenCount++
                    }
                }

                context.sendMessage(Message.translation("exampleplugin.commands.kit.received")
                    .param("name", kit.name)
                    .param("count", givenCount))
            }
        }
    }
}
