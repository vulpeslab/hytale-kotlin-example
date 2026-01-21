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
import com.vulpeslab.exampleplugin.services.KitItem
import com.vulpeslab.exampleplugin.services.KitManager
import java.util.concurrent.CompletableFuture

/**
 * UI page for browsing and selecting items to add to a kit.
 * Features an inline editor panel on the right side for quantity editing.
 */
class ItemBrowserUiPage(
    playerRef: PlayerRef,
    private val kitName: String,
    private val searchQuery: String = "",
    private val selectedItemId: String? = null,
    private val categoryFilter: String? = null
) : InteractiveCustomUIPage<ItemBrowserUiPage.PageData>(
    playerRef,
    CustomPageLifetime.CanDismissOrCloseThroughInteraction,
    PageData.CODEC
) {

    companion object {
        private const val ITEMS_PER_PAGE = 100
        private val ADDED_STYLE = Value.ref<String>("Pages/ExamplePluginItemBrowserItem.ui", "AddedStyle")
        private val FILTER_ACTIVE_STYLE = Value.ref<String>("Pages/ExamplePluginItemBrowserFilter.ui", "FilterActiveStyle")
        
        // Category filters with their display names and matching category IDs
        // Categories use dot notation like "Items.Weapons", "Blocks.Rocks", etc.
        private val CATEGORY_FILTERS = listOf(
            "All" to null,
            "Weapons" to "Items.Weapons",
            "Armor" to "Items.Armors",
            "Tools" to "Items.Tools",
            "Food" to "Items.Foods",
            "Ingredients" to "Items.Ingredients",
            "Blocks" to "Blocks.",  // Prefix match for all block types
            "Furniture" to "Furniture."  // Prefix match for all furniture types
        )
    }

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/ExamplePluginItemBrowserPage.ui")

        // Set the kit name in the title
        commandBuilder.set("#KitNameLabel.Text", kitName)

        // Restore search value if we had one
        if (searchQuery.isNotEmpty()) {
            commandBuilder.set("#SearchInput.Value", searchQuery)
        }

        // Bind Enter key on search input (header search)
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#SearchInput",
            EventData()
                .append("Action", Action.Search.name)
                .append("@SearchQuery", "#SearchInput.Value")
        )

        // Live search while typing
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

        // Bind order kit button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#OrderKitButton",
            EventData().append("Action", Action.OrderKit.name)
        )

        // Build filter bar
        CATEGORY_FILTERS.forEachIndexed { index, (displayName, categoryId) ->
            val filterSelector = "#FilterBar[$index]"
            commandBuilder.append("#FilterBar", "Pages/ExamplePluginItemBrowserFilter.ui")
            commandBuilder.set("$filterSelector #FilterLabel.Text", displayName)
            
            // Highlight active filter
            if (categoryId == categoryFilter) {
                commandBuilder.set("$filterSelector.Style", FILTER_ACTIVE_STYLE)
            }
            
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                filterSelector,
                EventData()
                    .append("Action", Action.Filter.name)
                    .append("Category", categoryId ?: "")
            )
        }

        // Load items and kit data asynchronously
        CompletableFuture.supplyAsync {
            val assetMap = Item.getAssetMap()
            val allItems = assetMap.assetMap.keys.toList()
            
            // Filter by search query
            var filteredItems = if (searchQuery.isNotBlank()) {
                allItems.filter { it.contains(searchQuery, ignoreCase = true) }
            } else {
                allItems
            }
            
            // Filter by category
            if (categoryFilter != null) {
                filteredItems = filteredItems.filter { itemId ->
                    val item = assetMap.getAsset(itemId)
                    val categories = item?.categories ?: return@filter false
                    // Support prefix matching (e.g., "Blocks." matches "Blocks.Rocks", "Blocks.Plants", etc.)
                    if (categoryFilter.endsWith(".")) {
                        categories.any { it.startsWith(categoryFilter) }
                    } else {
                        categories.contains(categoryFilter)
                    }
                }
            }

            // Get kit and map for quick lookup
            val kit = KitManager.getKit(kitName)
            val kitItemsMap = kit?.items?.associateBy { it.itemId } ?: emptyMap()

            // Sort: kit items first in the kit's explicit order, then remaining items alphabetically
            val kitOrderDistinct = kit?.items?.map { it.itemId }?.distinct() ?: emptyList()
            val inKitOrdered = kitOrderDistinct.filter { filteredItems.contains(it) }
            val notInKit = filteredItems.filter { it !in kitOrderDistinct }.sorted()
            val sortedItems = inKitOrdered + notInKit

            Pair(sortedItems, kitItemsMap)
        }.thenAccept { (items, kitItemsMap) ->
            val updateBuilder = UICommandBuilder()
            val updateEventBuilder = UIEventBuilder()

            updateBuilder.clear("#ItemGrid")

            if (items.isEmpty()) {
                updateBuilder.append("#ItemGrid", "Pages/ExamplePluginItemBrowserEmpty.ui")
            } else {
                val itemsPerRow = 6
                var rowIndex = 0
                var itemsInCurrentRow = 0

                items.forEach { itemId ->
                    // Create new row when needed
                    if (itemsInCurrentRow == 0) {
                        updateBuilder.appendInline("#ItemGrid", "Group { LayoutMode: Left; Anchor: (Bottom: 4); }")
                    }

                    val rowSelector = "#ItemGrid[$rowIndex]"
                    val itemSelector = "$rowSelector[$itemsInCurrentRow]"

                    updateBuilder.append(rowSelector, "Pages/ExamplePluginItemBrowserItem.ui")
                    updateBuilder.set("$itemSelector #ItemIcon.ItemId", itemId)
                    
                    // Get translated item name
                    val item = Item.getAssetMap().getAsset(itemId)
                    val translationKey = item?.translationKey ?: "server.items.$itemId.name"
                    updateBuilder.set("$itemSelector #ItemNameLabel.Text", Message.translation(translationKey))

                    val kitItem = kitItemsMap[itemId]
                    if (kitItem != null) {
                        // Green style and show quantity badge for items in kit
                        updateBuilder.set("$itemSelector.Style", ADDED_STYLE)
                        updateBuilder.set("$itemSelector #QuantityBadge.Visible", true)
                        updateBuilder.set("$itemSelector #QuantityLabel.Text", "x${kitItem.quantity}")
                    }

                    // Click to select item for editing
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

            // Show editor panel if an item is selected
            if (selectedItemId != null) {
                val kitItem = kitItemsMap[selectedItemId]
                showEditorPanel(updateBuilder, updateEventBuilder, selectedItemId, kitItem)
            }

            sendUpdate(updateBuilder, updateEventBuilder, false)
        }
    }

    private fun showEditorPanel(
        updateBuilder: UICommandBuilder,
        updateEventBuilder: UIEventBuilder,
        itemId: String,
        kitItem: KitItem?
    ) {
        // Hide placeholder, show editor
        updateBuilder.set("#NoSelectionPlaceholder.Visible", false)
        updateBuilder.set("#SelectedItemDisplay.Visible", true)

        // Set item info
        updateBuilder.set("#SelectedItemIcon.ItemId", itemId)
        
        // Get translated item name for the editor panel
        val item = Item.getAssetMap().getAsset(itemId)
        val translationKey = item?.translationKey ?: "server.items.$itemId.name"
        updateBuilder.set("#SelectedItemName.Text", Message.translation(translationKey))

        if (kitItem != null) {
            // Item is in kit - show quantity and allow editing
            updateBuilder.set("#QuantityInput.Value", kitItem.quantity.toString())
            
            // Show remove button for items in kit
            updateBuilder.set("#RemoveFromKitButton.Visible", true)

            // Bind save quantity button
            updateEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#QuantityConfirmButton",
                EventData()
                    .append("Action", Action.SaveQuantity.name)
                    .append("ItemId", itemId)
                    .append("@Quantity", "#QuantityInput.Value")
            )

            // Bind remove button
            updateEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#RemoveFromKitButton",
                EventData()
                    .append("Action", Action.RemoveItem.name)
                    .append("ItemId", itemId)
            )
        } else {
            // Item not in kit - default quantity to 1, save will add it
            updateBuilder.set("#QuantityInput.Value", "1")

            // Bind add button (uses same confirm button)
            updateEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#QuantityConfirmButton",
                EventData()
                    .append("Action", Action.AddItem.name)
                    .append("ItemId", itemId)
                    .append("@Quantity", "#QuantityInput.Value")
            )

            // Hide remove button for items not in kit
            updateBuilder.set("#RemoveFromKitButton.Visible", false)
        }
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.SelectItem -> {
                val itemId = data.itemId ?: return
                // Update just the editor panel without reopening the page (preserves scroll position)
                val updateBuilder = UICommandBuilder()
                val updateEventBuilder = UIEventBuilder()
                
                // Get kit item info
                val kit = KitManager.getKit(kitName)
                val kitItem = kit?.items?.find { it.itemId == itemId }
                
                showEditorPanel(updateBuilder, updateEventBuilder, itemId, kitItem)
                sendUpdate(updateBuilder, updateEventBuilder, false)
            }
            Action.AddItem -> {
                val itemId = data.itemId ?: return
                val quantity = data.quantityStr?.toIntOrNull() ?: 1

                KitManager.addItemToKitAsync(kitName, itemId, quantity).thenAccept {
                    playerRef.sendMessage(Message.translation("exampleplugin.ui.kits.item.added")
                        .param("item", itemId))
                    player.pageManager.openCustomPage(ref, store, ItemBrowserUiPage(playerRef, kitName, searchQuery, itemId, categoryFilter))
                }
            }
            Action.SaveQuantity -> {
                val itemId = data.itemId ?: return
                val quantity = data.quantityStr?.toIntOrNull() ?: 1

                KitManager.setItemQuantityAsync(kitName, itemId, quantity).thenAccept {
                    playerRef.sendMessage(Message.translation("exampleplugin.ui.kits.item.quantity")
                        .param("item", itemId)
                        .param("quantity", quantity.toString()))
                    player.pageManager.openCustomPage(ref, store, ItemBrowserUiPage(playerRef, kitName, searchQuery, itemId, categoryFilter))
                }
            }
            Action.RemoveItem -> {
                val itemId = data.itemId ?: return
                KitManager.removeItemFromKitByItemIdAsync(kitName, itemId).thenAccept {
                    playerRef.sendMessage(Message.translation("exampleplugin.ui.kits.item.removed"))
                    player.pageManager.openCustomPage(ref, store, ItemBrowserUiPage(playerRef, kitName, searchQuery, null, categoryFilter))
                }
            }
            Action.Search -> {
                val query = data.searchQuery ?: ""
                // Rebuild item grid in-place to preserve scroll position
                CompletableFuture.supplyAsync {
                    val assetMap = Item.getAssetMap()
                    val allItems = assetMap.assetMap.keys.toList()

                    var filteredItems = if (query.isNotBlank()) {
                        allItems.filter { it.contains(query, ignoreCase = true) }
                    } else {
                        allItems
                    }

                    if (categoryFilter != null) {
                        filteredItems = filteredItems.filter { itemId ->
                            val item = assetMap.getAsset(itemId)
                            val categories = item?.categories ?: return@filter false
                            if (categoryFilter.endsWith(".")) {
                                categories.any { it.startsWith(categoryFilter) }
                            } else {
                                categories.contains(categoryFilter)
                            }
                        }
                    }

                    val kit = KitManager.getKit(kitName)
                    val kitItemsMap = kit?.items?.associateBy { it.itemId } ?: emptyMap()

                    val kitOrderDistinct = kit?.items?.map { it.itemId }?.distinct() ?: emptyList()
                    val inKitOrdered = kitOrderDistinct.filter { filteredItems.contains(it) }
                    val notInKit = filteredItems.filter { it !in kitOrderDistinct }.sorted()
                    val sortedItems = inKitOrdered + notInKit

                    Pair(sortedItems, kitItemsMap)
                }.thenAccept { (items, kitItemsMap) ->
                    val updateBuilder = UICommandBuilder()
                    val updateEventBuilder = UIEventBuilder()

                    updateBuilder.clear("#ItemGrid")

                    if (items.isEmpty()) {
                        updateBuilder.append("#ItemGrid", "Pages/ExamplePluginItemBrowserEmpty.ui")
                    } else {
                        val itemsPerRow = 6
                        var rowIndex = 0
                        var itemsInCurrentRow = 0

                        items.forEach { itemId ->
                            if (itemsInCurrentRow == 0) {
                                updateBuilder.appendInline("#ItemGrid", "Group { LayoutMode: Left; Anchor: (Bottom: 4); }")
                            }

                            val rowSelector = "#ItemGrid[$rowIndex]"
                            val itemSelector = "$rowSelector[$itemsInCurrentRow]"

                            updateBuilder.append(rowSelector, "Pages/ExamplePluginItemBrowserItem.ui")
                            updateBuilder.set("$itemSelector #ItemIcon.ItemId", itemId)

                            val item = Item.getAssetMap().getAsset(itemId)
                            val translationKey = item?.translationKey ?: "server.items.$itemId.name"
                            updateBuilder.set("$itemSelector #ItemNameLabel.Text", Message.translation(translationKey))

                            val kitItem = kitItemsMap[itemId]
                            if (kitItem != null) {
                                updateBuilder.set("$itemSelector.Style", ADDED_STYLE)
                                updateBuilder.set("$itemSelector #QuantityBadge.Visible", true)
                                updateBuilder.set("$itemSelector #QuantityLabel.Text", "x${kitItem.quantity}")
                            }

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

                    // Preserve editor panel state if selection exists
                    if (selectedItemId != null) {
                        val kitItem = kitItemsMap[selectedItemId]
                        showEditorPanel(updateBuilder, updateEventBuilder, selectedItemId, kitItem)
                    }

                    sendUpdate(updateBuilder, updateEventBuilder, false)
                }
            }
            Action.Filter -> {
                val category = data.category?.takeIf { it.isNotBlank() }
                player.pageManager.openCustomPage(ref, store, ItemBrowserUiPage(playerRef, kitName, searchQuery, null, category))
            }
            Action.Back -> {
                player.pageManager.openCustomPage(ref, store, KitListUiPage(playerRef))
            }
            Action.OrderKit -> {
                player.pageManager.openCustomPage(ref, store, KitOrderUiPage(playerRef, kitName))
            }
        }
    }

    enum class Action {
        SelectItem,
        AddItem,
        SaveQuantity,
        RemoveItem,
        Search,
        Filter,
        Back,
        OrderKit
    }

    class PageData {
        var action: Action = Action.SelectItem
        var itemId: String? = null
        var searchQuery: String? = null
        var quantityStr: String? = null
        var category: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .append(
                    KeyedCodec("ItemId", Codec.STRING),
                    { obj, itemId -> obj.itemId = itemId },
                    { obj -> obj.itemId }
                ).add()
                .append(
                    KeyedCodec("@SearchQuery", Codec.STRING),
                    { obj, query -> obj.searchQuery = query },
                    { obj -> obj.searchQuery }
                ).add()
                .append(
                    KeyedCodec("@Quantity", Codec.STRING),
                    { obj, qty -> obj.quantityStr = qty },
                    { obj -> obj.quantityStr }
                ).add()
                .append(
                    KeyedCodec("Category", Codec.STRING),
                    { obj, cat -> obj.category = cat },
                    { obj -> obj.category }
                ).add()
                .build()
        }
    }
}
