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
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.TraderNpcManager

/**
 * UI page for configuring trades on a trader NPC.
 * Admins can add, edit, and remove trades.
 */
class NpcTradeConfigUiPage(
    playerRef: PlayerRef,
    private val npcId: String
) : InteractiveCustomUIPage<NpcTradeConfigUiPage.PageData>(
    playerRef,
    CustomPageLifetime.CanDismissOrCloseThroughInteraction,
    PageData.CODEC
) {

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/NpcTradeConfigPage.ui")

        val npc = TraderNpcManager.getNpc(npcId)
        if (npc == null) {
            commandBuilder.set("#TitleLabel.Text", Message.translation("exampleplugin.ui.npc.config.title"))
            return
        }

        // Set title with NPC name
        commandBuilder.set("#TitleLabel.Text",
            Message.translation("exampleplugin.ui.npc.config.title")
                .insert(Message.raw(" " + npc.name))
        )

        // Bind add trade button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#AddTradeButton",
            EventData().append("Action", Action.AddTrade.name)
        )

        if (npc.trades.isEmpty()) {
            // Show "no trades" message
            commandBuilder.appendInline("#TradeList", """
                Group {
                    Anchor: (Full: 20);
                    LayoutMode: Middle;

                    Label {
                        Style: LabelStyle(FontSize: 14, TextColor: #7090a0, HorizontalAlignment: Center);
                        Text: %exampleplugin.ui.npc.config.notrades;
                    }
                }
            """.trimIndent())
            return
        }

        // Render trade list
        npc.trades.forEachIndexed { index, trade ->
            val selector = "#TradeList[$index]"
            commandBuilder.append("#TradeList", "Pages/NpcTradeConfigItem.ui")

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

            // Bind edit button (opens item selection for this trade)
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #EditButton",
                EventData()
                    .append("Action", Action.EditTrade.name)
                    .append("TradeIndex", index.toString())
            )

            // Bind remove button
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #RemoveButton",
                EventData()
                    .append("Action", Action.RemoveTrade.name)
                    .append("TradeIndex", index.toString())
            )
        }
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.AddTrade -> {
                // Open item selection UI for a new trade (selecting input item first)
                player.pageManager.openCustomPage(ref, store,
                    NpcTradeItemSelectUiPage(playerRef, npcId, -1, true))
            }

            Action.EditTrade -> {
                val tradeIndex = data.tradeIndex?.toIntOrNull() ?: return
                // Open item selection UI to edit this trade (start with input item)
                player.pageManager.openCustomPage(ref, store,
                    NpcTradeItemSelectUiPage(playerRef, npcId, tradeIndex, true))
            }

            Action.RemoveTrade -> {
                val tradeIndex = data.tradeIndex?.toIntOrNull() ?: return
                TraderNpcManager.removeTrade(npcId, tradeIndex)
                playerRef.sendMessage(Message.translation("exampleplugin.ui.npc.trade.removed"))
                // Refresh UI
                player.pageManager.openCustomPage(ref, store, NpcTradeConfigUiPage(playerRef, npcId))
            }

            Action.Close -> {
                player.pageManager.setPage(ref, store, Page.None)
            }
        }
    }

    enum class Action {
        AddTrade,
        EditTrade,
        RemoveTrade,
        Close
    }

    class PageData {
        var action: Action = Action.AddTrade
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
