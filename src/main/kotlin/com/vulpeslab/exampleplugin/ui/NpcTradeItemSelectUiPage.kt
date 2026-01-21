package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.EnumCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.Value
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.i18n.I18nModule
import com.vulpeslab.exampleplugin.services.TradeData
import com.vulpeslab.exampleplugin.services.TraderNpcManager
import java.util.concurrent.CompletableFuture

/**
 * UI page for selecting items and quantities when configuring a trade.
 * Used for both selecting input and output items.
 */
class NpcTradeItemSelectUiPage(
    playerRef: PlayerRef,
    private val npcId: String,
    private val tradeIndex: Int, // -1 for new trade
    private val selectingInput: Boolean,
    private var currentSearchQuery: String = "",
    private val pendingInputItemId: String? = null,
    private val pendingInputQuantity: Int = 1,
    private var currentSelectedItemId: String? = null
) : InteractiveCustomUIPage<NpcTradeItemSelectUiPage.PageData>(
    playerRef,
    CustomPageLifetime.CanDismissOrCloseThroughInteraction,
    PageData.CODEC
) {

    companion object {
        private val SELECTED_STYLE = Value.ref<String>("Pages/NpcTradeItemSelectItem.ui", "SelectedStyle")
    }

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/NpcTradeItemSelectPage.ui")

        // Set title based on what we're selecting
        if (selectingInput) {
            commandBuilder.set("#TitleLabel.Text", Message.translation("exampleplugin.ui.npc.config.selectinput"))
        } else {
            commandBuilder.set("#TitleLabel.Text", Message.translation("exampleplugin.ui.npc.config.selectoutput"))
        }

        // Restore search value if we had one
        if (currentSearchQuery.isNotEmpty()) {
            commandBuilder.set("#SearchInput.Value", currentSearchQuery)
        }

        // Bind search input
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData()
                .append("Action", Action.Search.name)
                .append("@SearchQuery", "#SearchInput.Value")
        )

        // Bind back button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BackButton",
            EventData().append("Action", Action.Back.name)
        )

        // Load items asynchronously
        refreshItemGrid()
    }

    /**
     * Refreshes the item grid using sendUpdate() to preserve scroll position and focus.
     */
    private fun refreshItemGrid() {
        CompletableFuture.supplyAsync {
            val assetMap = Item.getAssetMap()
            val allItems = assetMap.assetMap.keys.toList()

            // Filter by search query (searches both item ID and translated name)
            if (currentSearchQuery.isNotBlank()) {
                allItems.filter { itemId ->
                    // Check if ID matches
                    if (itemId.contains(currentSearchQuery, ignoreCase = true)) {
                        return@filter true
                    }
                    // Check if translated name matches
                    val item = assetMap.getAsset(itemId)
                    val translationKey = item?.translationKey ?: "server.items.$itemId.name"
                    val translatedName = I18nModule.get().getMessage("en-US", translationKey) ?: ""
                    translatedName.contains(currentSearchQuery, ignoreCase = true)
                }.sorted()
            } else {
                allItems.sorted()
            }
        }.thenAccept { items ->
            val updateBuilder = UICommandBuilder()
            val updateEventBuilder = UIEventBuilder()

            updateBuilder.clear("#ItemGrid")

            if (items.isEmpty()) {
                updateBuilder.appendInline("#ItemGrid", """
                    Group {
                        Anchor: (Full: 20);
                        LayoutMode: Middle;

                        Label {
                            Style: LabelStyle(FontSize: 14, TextColor: #7090a0, HorizontalAlignment: Center);
                            Text: %exampleplugin.ui.kits.browser.noresults;
                        }
                    }
                """.trimIndent())
            } else {
                val itemsPerRow = 6
                var rowIndex = 0
                var itemsInCurrentRow = 0

                items.take(150).forEach { itemId ->
                    // Create new row when needed
                    if (itemsInCurrentRow == 0) {
                        updateBuilder.appendInline("#ItemGrid", "Group { LayoutMode: Left; Anchor: (Bottom: 4); }")
                    }

                    val rowSelector = "#ItemGrid[$rowIndex]"
                    val itemSelector = "$rowSelector[$itemsInCurrentRow]"

                    updateBuilder.append(rowSelector, "Pages/NpcTradeItemSelectItem.ui")
                    updateBuilder.set("$itemSelector #ItemIcon.ItemId", itemId)

                    // Get translated item name
                    val item = Item.getAssetMap().getAsset(itemId)
                    val translationKey = item?.translationKey ?: "server.items.$itemId.name"
                    updateBuilder.set("$itemSelector #ItemNameLabel.Text", Message.translation(translationKey))

                    // Highlight if selected
                    if (itemId == currentSelectedItemId) {
                        updateBuilder.set("$itemSelector.Style", SELECTED_STYLE)
                    }

                    // Bind click to select item
                    updateEventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        itemSelector,
                        EventData()
                            .append("Action", Action.SelectItem.name)
                            .append("ItemId", itemId)
                    )

                    itemsInCurrentRow++
                    if (itemsInCurrentRow >= itemsPerRow) {
                        itemsInCurrentRow = 0
                        rowIndex++
                    }
                }
            }

            // Show/hide quantity editor based on selection
            if (currentSelectedItemId != null) {
                showQuantityEditor(updateBuilder, updateEventBuilder, currentSelectedItemId!!)
            } else {
                updateBuilder.set("#QuantityEditor.Visible", false)
            }

            sendUpdate(updateBuilder, updateEventBuilder, false)
        }
    }

    private fun showQuantityEditor(
        updateBuilder: UICommandBuilder,
        updateEventBuilder: UIEventBuilder,
        itemId: String
    ) {
        updateBuilder.set("#QuantityEditor.Visible", true)
        updateBuilder.set("#SelectedItemIcon.ItemId", itemId)

        val item = Item.getAssetMap().getAsset(itemId)
        val translationKey = item?.translationKey ?: "server.items.$itemId.name"
        updateBuilder.set("#SelectedItemName.Text", Message.translation(translationKey))

        // Default quantity
        updateBuilder.set("#QuantityInput.Value", "1")

        // Bind confirm button
        updateEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ConfirmButton",
            EventData()
                .append("Action", Action.ConfirmSelection.name)
                .append("ItemId", itemId)
                .append("@Quantity", "#QuantityInput.Value")
        )

        // Bind cancel button
        updateEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelButton",
            EventData().append("Action", Action.CancelSelection.name)
        )
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.Search -> {
                // Update search query and refresh (preserves scroll/focus)
                currentSearchQuery = data.searchQuery ?: ""
                currentSelectedItemId = null
                refreshItemGrid()
            }

            Action.SelectItem -> {
                // Update selection and refresh (preserves scroll/focus)
                currentSelectedItemId = data.itemId
                refreshItemGrid()
            }

            Action.ConfirmSelection -> {
                val itemId = data.itemId ?: return
                val quantity = data.quantityStr?.toIntOrNull()?.coerceAtLeast(1) ?: 1

                if (selectingInput) {
                    // Input selected, now select output - need new page for this
                    player.pageManager.openCustomPage(ref, store,
                        NpcTradeItemSelectUiPage(playerRef, npcId, tradeIndex, false, "",
                            itemId, quantity, null))
                } else {
                    // Output selected, create/update the trade
                    val inputItemId = pendingInputItemId ?: return
                    val trade = TradeData(inputItemId, pendingInputQuantity, itemId, quantity)

                    if (tradeIndex >= 0) {
                        // Update existing trade
                        TraderNpcManager.updateTrade(npcId, tradeIndex, trade)
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.npc.trade.updated"))
                    } else {
                        // Add new trade
                        TraderNpcManager.addTrade(npcId, trade)
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.npc.trade.added"))
                    }

                    // Return to config page
                    player.pageManager.openCustomPage(ref, store, NpcTradeConfigUiPage(playerRef, npcId))
                }
            }

            Action.CancelSelection -> {
                // Clear selection and refresh (preserves scroll/focus)
                currentSelectedItemId = null
                refreshItemGrid()
            }

            Action.Back -> {
                if (!selectingInput && pendingInputItemId != null) {
                    // Go back to input selection
                    player.pageManager.openCustomPage(ref, store,
                        NpcTradeItemSelectUiPage(playerRef, npcId, tradeIndex, true, "", null, 1, null))
                } else {
                    // Go back to config page
                    player.pageManager.openCustomPage(ref, store, NpcTradeConfigUiPage(playerRef, npcId))
                }
            }
        }
    }

    enum class Action {
        Search,
        SelectItem,
        ConfirmSelection,
        CancelSelection,
        Back
    }

    class PageData {
        var action: Action = Action.Search
        var searchQuery: String? = null
        var itemId: String? = null
        var quantityStr: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .append(
                    KeyedCodec("@SearchQuery", Codec.STRING),
                    { obj, query -> obj.searchQuery = query },
                    { obj -> obj.searchQuery }
                ).add()
                .append(
                    KeyedCodec("ItemId", Codec.STRING),
                    { obj, id -> obj.itemId = id },
                    { obj -> obj.itemId }
                ).add()
                .append(
                    KeyedCodec("@Quantity", Codec.STRING),
                    { obj, qty -> obj.quantityStr = qty },
                    { obj -> obj.quantityStr }
                ).add()
                .build()
        }
    }
}
