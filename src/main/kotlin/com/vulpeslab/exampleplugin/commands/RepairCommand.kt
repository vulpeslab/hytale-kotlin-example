package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

/**
 * Repairs the item in the player's hand to full durability if it has durability.
 * Usage: /repair
 */
class RepairCommand : AbstractPlayerCommand("repair", "exampleplugin.commands.repair.description") {

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        requireNotNull(player)

        val inventory = player.inventory
        val inHand = inventory.activeHotbarItem

        if (inHand == null || ItemStack.isEmpty(inHand)) {
            playerRef.sendMessage(Message.translation("exampleplugin.commands.repair.notHoldingItem"))
            return
        }

        if (inHand.isUnbreakable() || inHand.maxDurability <= 0.0) {
            playerRef.sendMessage(Message.translation("exampleplugin.commands.repair.notDurable"))
            return
        }

        val max = inHand.maxDurability
        val repaired = inHand.withRestoredDurability(max)
        inventory.hotbar.replaceItemStackInSlot(inventory.activeHotbarSlot.toInt().toShort(), inHand, repaired)

        playerRef.sendMessage(
            Message.translation("exampleplugin.commands.repair.success")
                .param("item", inHand.itemId)
        )
    }
}
