package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.EnumCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.protocol.packets.interface_.Page
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.ui.Value
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.TraderNpcManager

/**
 * UI page for executing trades with a trader NPC.
 * Shows available trades and allows players to exchange items.
 */
class NpcTradeExecuteUiPage(
    playerRef: PlayerRef,
    private val npcId: String
) : InteractiveCustomUIPage<NpcTradeExecuteUiPage.PageData>(
    playerRef,
    CustomPageLifetime.CanDismissOrCloseThroughInteraction,
    PageData.CODEC
) {

    companion object {
        private val CAN_AFFORD_STYLE = Value.ref<String>("Pages/NpcTradeExecuteItem.ui", "TradeButtonCanAffordStyle")
        private val CANNOT_AFFORD_STYLE = Value.ref<String>("Pages/NpcTradeExecuteItem.ui", "TradeButtonCannotAffordStyle")
    }

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/NpcTradeExecutePage.ui")

        val npc = TraderNpcManager.getNpc(npcId)
        if (npc == null) {
            commandBuilder.set("#TitleLabel.Text", Message.translation("exampleplugin.ui.npc.trade.title"))
            return
        }

        // Set title with NPC name
        commandBuilder.set("#TitleLabel.Text",
            Message.translation("exampleplugin.ui.npc.trade.title")
                .param("name", npc.name)
        )

        if (npc.trades.isEmpty()) {
            // Show "no trades" message
            commandBuilder.appendInline("#TradeList", """
                Group {
                    Anchor: (Full: 20);
                    LayoutMode: Middle;

                    Label {
                        Style: LabelStyle(FontSize: 14, TextColor: #7090a0, HorizontalAlignment: Center);
                        Text: %exampleplugin.ui.npc.trade.notrades;
                    }
                }
            """.trimIndent())
            return
        }

        // Get player inventory to check affordability
        val player = store.getComponent(ref, Player.getComponentType())
        val inventory = player?.inventory?.combinedHotbarFirst

        // Render trade list
        npc.trades.forEachIndexed { index, trade ->
            val selector = "#TradeList[$index]"
            commandBuilder.append("#TradeList", "Pages/NpcTradeExecuteItem.ui")

            // Set input item
            commandBuilder.set("$selector #InputItemIcon.ItemId", trade.inputItemId)
            commandBuilder.set("$selector #InputQuantityLabel.Text", "x${trade.inputQuantity}")

            // Get translated item names
            val inputItem = Item.getAssetMap().getAsset(trade.inputItemId)
            val inputKey = inputItem?.translationKey ?: "server.items.${trade.inputItemId}.name"
            commandBuilder.set("$selector #InputItemName.Text", Message.translation(inputKey))

            // Set output item
            commandBuilder.set("$selector #OutputItemIcon.ItemId", trade.outputItemId)
            commandBuilder.set("$selector #OutputQuantityLabel.Text", "x${trade.outputQuantity}")

            val outputItem = Item.getAssetMap().getAsset(trade.outputItemId)
            val outputKey = outputItem?.translationKey ?: "server.items.${trade.outputItemId}.name"
            commandBuilder.set("$selector #OutputItemName.Text", Message.translation(outputKey))

            // Check if player can afford this trade and set button color
            val canAfford = if (inventory != null) {
                val count = inventory.countItemStacks { it.itemId == trade.inputItemId }
                count >= trade.inputQuantity
            } else false

            commandBuilder.set("$selector #TradeButton.Style",
                if (canAfford) CAN_AFFORD_STYLE else CANNOT_AFFORD_STYLE)

            // Bind trade button
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #TradeButton",
                EventData()
                    .append("Action", Action.ExecuteTrade.name)
                    .append("TradeIndex", index.toString())
            )
        }
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.ExecuteTrade -> {
                val tradeIndex = data.tradeIndex?.toIntOrNull() ?: return
                val npc = TraderNpcManager.getNpc(npcId) ?: return

                if (tradeIndex < 0 || tradeIndex >= npc.trades.size) return
                val trade = npc.trades[tradeIndex]

                val inventory = player.inventory.combinedHotbarFirst

                // Create item stacks using itemId strings
                val inputStack = ItemStack(trade.inputItemId, trade.inputQuantity, null)
                val outputStack = ItemStack(trade.outputItemId, trade.outputQuantity, null)

                // Check if player has space for output first
                if (!inventory.canAddItemStack(outputStack)) {
                    playerRef.sendMessage(Message.translation("exampleplugin.ui.npc.trade.nospace"))
                    // Refresh UI to clear loading state
                    player.pageManager.openCustomPage(ref, store, NpcTradeExecuteUiPage(playerRef, npcId))
                    return
                }

                // Try to remove input items (allOrNothing=true ensures we only remove if we have enough)
                val removeTransaction = inventory.removeItemStack(inputStack, true, true)
                if (!removeTransaction.succeeded()) {
                    playerRef.sendMessage(Message.translation("exampleplugin.ui.npc.trade.noitems"))
                    // Refresh UI to clear loading state
                    player.pageManager.openCustomPage(ref, store, NpcTradeExecuteUiPage(playerRef, npcId))
                    return
                }

                // Add output items
                inventory.addItemStack(outputStack)

                playerRef.sendMessage(Message.translation("exampleplugin.ui.npc.trade.success"))

                // Refresh the UI
                player.pageManager.openCustomPage(ref, store, NpcTradeExecuteUiPage(playerRef, npcId))
            }

            Action.Close -> {
                player.pageManager.setPage(ref, store, Page.None)
            }
        }
    }

    enum class Action {
        ExecuteTrade,
        Close
    }

    class PageData {
        var action: Action = Action.ExecuteTrade
        var tradeIndex: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .append(
                    KeyedCodec("TradeIndex", Codec.STRING),
                    { obj, index -> obj.tradeIndex = index },
                    { obj -> obj.tradeIndex }
                ).add()
                .build()
        }
    }
}
