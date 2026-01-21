package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

/**
 * Helper class that manages the armor icons display in the HUD.
 * Shows ItemIcon components for each equipped armor piece (Head, Chest, Hands, Legs)
 * along with durability bars.
 */
class ArmorIconsHud(
    private val ref: Ref<EntityStore>,
    private val store: Store<EntityStore>
) {
    companion object {
        private const val UI_PATH = "Hud/ExamplePluginArmorIconsHud.ui"

        // Armor slot indices matching ItemArmorSlot enum
        private const val SLOT_HEAD = 0
        private const val SLOT_CHEST = 1
        private const val SLOT_HANDS = 2
        private const val SLOT_LEGS = 3
    }

    /**
     * Data class holding armor slot information.
     */
    private data class ArmorSlotInfo(
        val itemId: String?,
        val durabilityPercent: Float
    )

    /**
     * Append the armor icons UI to the command builder.
     */
    fun appendUI(commandBuilder: UICommandBuilder) {
        commandBuilder.append(UI_PATH)
    }

    /**
     * Update the armor icon display with current equipped armor and durability.
     */
    fun updateArmorIcons(commandBuilder: UICommandBuilder) {
        val armorInfo = getEquippedArmorInfo()

        // Head slot
        val head = armorInfo[SLOT_HEAD]
        commandBuilder.set("#ArmorSlotHead.ItemId", head?.itemId ?: "")
        commandBuilder.set("#ArmorSlotHead.Visible", head != null)
        commandBuilder.set("#DurabilityHead.Visible", head != null)
        commandBuilder.set("#DurabilityHead.Value", head?.durabilityPercent ?: 0f)

        // Chest slot
        val chest = armorInfo[SLOT_CHEST]
        commandBuilder.set("#ArmorSlotChest.ItemId", chest?.itemId ?: "")
        commandBuilder.set("#ArmorSlotChest.Visible", chest != null)
        commandBuilder.set("#DurabilityChest.Visible", chest != null)
        commandBuilder.set("#DurabilityChest.Value", chest?.durabilityPercent ?: 0f)

        // Hands slot
        val hands = armorInfo[SLOT_HANDS]
        commandBuilder.set("#ArmorSlotHands.ItemId", hands?.itemId ?: "")
        commandBuilder.set("#ArmorSlotHands.Visible", hands != null)
        commandBuilder.set("#DurabilityHands.Visible", hands != null)
        commandBuilder.set("#DurabilityHands.Value", hands?.durabilityPercent ?: 0f)

        // Legs slot
        val legs = armorInfo[SLOT_LEGS]
        commandBuilder.set("#ArmorSlotLegs.ItemId", legs?.itemId ?: "")
        commandBuilder.set("#ArmorSlotLegs.Visible", legs != null)
        commandBuilder.set("#DurabilityLegs.Visible", legs != null)
        commandBuilder.set("#DurabilityLegs.Value", legs?.durabilityPercent ?: 0f)
    }

    /**
     * Get information about currently equipped armor pieces.
     * Returns a map of slot index to ArmorSlotInfo (null if slot is empty).
     */
    private fun getEquippedArmorInfo(): Map<Int, ArmorSlotInfo?> {
        val result = mutableMapOf<Int, ArmorSlotInfo?>()

        val player = store.getComponent(ref, Player.getComponentType())
        if (player == null) {
            return (0..3).associateWith { null }
        }

        val inventory = player.inventory
        if (inventory == null) {
            return (0..3).associateWith { null }
        }

        val armorContainer = inventory.armor
        if (armorContainer == null) {
            return (0..3).associateWith { null }
        }

        for (i in 0 until armorContainer.capacity.coerceAtMost(4)) {
            val itemStack: ItemStack? = armorContainer.getItemStack(i.toShort())
            if (itemStack == null || itemStack.isEmpty) {
                result[i] = null
                continue
            }

            // Verify it's actually an armor item
            val item = Item.getAssetMap().getAsset(itemStack.itemId)
            if (item?.armor != null) {
                val durability = itemStack.durability
                val maxDurability = itemStack.maxDurability
                val durabilityPercent = if (maxDurability > 0) {
                    (durability / maxDurability).toFloat().coerceIn(0f, 1f)
                } else {
                    1f
                }
                result[i] = ArmorSlotInfo(itemStack.itemId, durabilityPercent)
            } else {
                result[i] = null
            }
        }

        return result
    }
}
