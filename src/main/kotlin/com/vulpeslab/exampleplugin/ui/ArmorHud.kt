package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import kotlin.math.roundToInt

/**
 * Custom HUD that displays the player's defense value as a progress bar above the health bar,
 * along with armor icons showing currently equipped armor pieces.
 * Defense is calculated from Physical damage resistance of equipped armor.
 */
class ArmorHud(
    playerRef: PlayerRef,
    private val ref: Ref<EntityStore>,
    private val store: Store<EntityStore>
) : CustomUIHud(playerRef) {

    private val armorIconsHud = ArmorIconsHud(ref, store)

    override fun build(commandBuilder: UICommandBuilder) {
        commandBuilder.append("Hud/ExamplePluginArmorHud.ui")
        updateArmorDisplay(commandBuilder)

        armorIconsHud.appendUI(commandBuilder)
        armorIconsHud.updateArmorIcons(commandBuilder)
    }

    fun refresh() {
        val commandBuilder = UICommandBuilder()
        updateArmorDisplay(commandBuilder)
        armorIconsHud.updateArmorIcons(commandBuilder)
        update(false, commandBuilder)
    }

    private fun updateArmorDisplay(commandBuilder: UICommandBuilder) {
        val defensePercent = calculateDefensePercent()
        commandBuilder.set("#DefenseBar.Value", (defensePercent / 100.0).toFloat())
        commandBuilder.set("#DefenseValue.Text", "${defensePercent}%")
    }

    private fun calculateDefensePercent(): Int {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return 0
        val inventory = player.inventory ?: return 0
        val armorContainer = inventory.armor ?: return 0

        var totalResistance = 0.0

        for (i in 0 until armorContainer.capacity) {
            val itemStack: ItemStack = armorContainer.getItemStack(i.toShort()) ?: continue
            if (itemStack.isEmpty) continue

            val item = Item.getAssetMap().getAsset(itemStack.itemId) ?: continue
            val armor = item.armor ?: continue

            val damageResistance = armor.damageResistanceValues ?: continue

            for ((cause, modifiers) in damageResistance) {
                val causeId = try {
                    cause.javaClass.getDeclaredField("id").let { field ->
                        field.isAccessible = true
                        field.get(cause) as? String
                    }
                } catch (e: Exception) { null }

                if (causeId == "Physical") {
                    for (modifier in modifiers) {
                        val amount = modifier.amount.toDouble()
                        when (modifier.calculationType) {
                            StaticModifier.CalculationType.MULTIPLICATIVE -> {
                                totalResistance += amount
                            }
                            StaticModifier.CalculationType.ADDITIVE -> {
                                totalResistance += amount
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        return (totalResistance * 100).roundToInt().coerceIn(0, 100)
    }
}
