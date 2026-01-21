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
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.Kit
import com.vulpeslab.exampleplugin.services.KitManager

/**
 * UI page showing a list of all kits with options to edit, delete, rename, or create new kits.
 * Displays item preview icons for each kit.
 */
class KitListUiPage(
    playerRef: PlayerRef,
    private val searchQuery: String = "",
    private val renamingKit: String? = null
) : InteractiveCustomUIPage<KitListUiPage.PageData>(
    playerRef,
    CustomPageLifetime.CanDismiss,
    PageData.CODEC
) {

    companion object {
        private const val MAX_PREVIEW_ITEMS = 12
    }

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/ExamplePluginKitListPage.ui")

        // Restore search value if we had one
        if (searchQuery.isNotEmpty()) {
            commandBuilder.set("#SearchInput.Value", searchQuery)
        }

        // Bind "Add Kit" button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#AddKitButton",
            EventData()
                .append("Action", Action.AddKit.name)
                .append("@KitName", "#NewKitNameInput.Value")
        )

        // Bind Enter key on new kit name input
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#NewKitNameInput",
            EventData()
                .append("Action", Action.AddKit.name)
                .append("@KitName", "#NewKitNameInput.Value")
        )

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

        // Load kits asynchronously and update the UI
        KitManager.getAllKitsAsync().thenAccept { allKits ->
            val updateBuilder = UICommandBuilder()
            val updateEventBuilder = UIEventBuilder()

            // Filter kits by search query
            val kits = if (searchQuery.isNotBlank()) {
                allKits.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                allKits
            }

            updateBuilder.clear("#KitList")
            kits.forEachIndexed { index, kit ->
                val selector = "#KitList[$index]"
                updateBuilder.append("#KitList", "Pages/ExamplePluginKitListItem.ui")
                updateBuilder.set("$selector #KitNameLabel.Text", kit.name)

                // Add item preview icons
                buildItemPreview(updateBuilder, selector, kit)

                // Edit button
                updateEventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #EditButton",
                    EventData()
                        .append("Action", Action.EditKit.name)
                        .append("KitName", kit.name)
                )

                // Rename button - shows the rename popup
                updateEventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #RenameButton",
                    EventData()
                        .append("Action", Action.ShowRenamePopup.name)
                        .append("KitName", kit.name)
                )

                // Delete button
                updateEventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #DeleteButton",
                    EventData()
                        .append("Action", Action.DeleteKit.name)
                        .append("KitName", kit.name)
                )
            }

            // Show rename popup if renaming
            if (renamingKit != null) {
                showRenamePopup(updateBuilder, updateEventBuilder, renamingKit)
            }

            sendUpdate(updateBuilder, updateEventBuilder, false)
        }
    }

    private fun buildItemPreview(updateBuilder: UICommandBuilder, selector: String, kit: Kit) {
        val previewSelector = "$selector #ItemPreviewRow"
        val itemsToShow = kit.items.take(MAX_PREVIEW_ITEMS)

        itemsToShow.forEachIndexed { itemIndex, item ->
            val itemSelector = "$previewSelector[$itemIndex]"
            updateBuilder.append(previewSelector, "Pages/ExamplePluginKitListItemPreview.ui")
            updateBuilder.set("$itemSelector #ItemIcon.ItemId", item.itemId)
        }

        // Show "+N more" label if there are more items
        if (kit.items.size > MAX_PREVIEW_ITEMS) {
            val moreCount = kit.items.size - MAX_PREVIEW_ITEMS
            updateBuilder.appendInline(previewSelector,
                "Label { Style: LabelStyle(FontSize: 12, TextColor: #8090a0); Padding: (Left: 8); Text: \"+$moreCount\"; }")
        }
    }

    private fun showRenamePopup(
        updateBuilder: UICommandBuilder,
        updateEventBuilder: UIEventBuilder,
        kitName: String
    ) {
        // Show the popup and set the current kit name
        updateBuilder.set("#RenamePopup.Visible", true)
        updateBuilder.set("#RenameInput.Value", kitName)

        // Bind confirm button
        updateEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RenameConfirmButton",
            EventData()
                .append("Action", Action.ConfirmRename.name)
                .append("KitName", kitName)
                .append("@NewName", "#RenameInput.Value")
        )

        // Bind Enter key on rename input
        updateEventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#RenameInput",
            EventData()
                .append("Action", Action.ConfirmRename.name)
                .append("KitName", kitName)
                .append("@NewName", "#RenameInput.Value")
        )

        // Bind cancel button
        updateEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RenameCancelButton",
            EventData().append("Action", Action.CancelRename.name)
        )
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.EditKit -> {
                val kitName = data.kitName ?: return
                player.pageManager.openCustomPage(ref, store, ItemBrowserUiPage(playerRef, kitName))
            }
            Action.DeleteKit -> {
                val kitName = data.kitName ?: return
                KitManager.deleteKitAsync(kitName).thenAccept { success ->
                    if (success) {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.kits.deleted")
                            .param("name", kitName))
                    }
                    player.pageManager.openCustomPage(ref, store, KitListUiPage(playerRef, searchQuery))
                }
            }
            Action.AddKit -> {
                val kitName = data.kitName?.takeIf { it.isNotBlank() } ?: return
                KitManager.createKitAsync(kitName).thenAccept { kit ->
                    if (kit != null) {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.kits.created")
                            .param("name", kitName))
                        player.pageManager.openCustomPage(ref, store, ItemBrowserUiPage(playerRef, kit.name))
                    } else {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.kits.exists")
                            .param("name", kitName))
                        player.pageManager.openCustomPage(ref, store, KitListUiPage(playerRef, searchQuery))
                    }
                }
            }
            Action.Search -> {
                val query = data.searchQuery ?: ""
                // Update list in-place to preserve scroll position
                KitManager.getAllKitsAsync().thenAccept { allKits ->
                    val updateBuilder = UICommandBuilder()
                    val updateEventBuilder = UIEventBuilder()

                    val kits = if (query.isNotBlank()) {
                        allKits.filter { it.name.contains(query, ignoreCase = true) }
                    } else {
                        allKits
                    }

                    updateBuilder.clear("#KitList")
                    kits.forEachIndexed { index, kit ->
                        val selector = "#KitList[$index]"
                        updateBuilder.append("#KitList", "Pages/ExamplePluginKitListItem.ui")
                        updateBuilder.set("$selector #KitNameLabel.Text", kit.name)

                        // Item preview icons
                        buildItemPreview(updateBuilder, selector, kit)

                        // Edit button
                        updateEventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "$selector #EditButton",
                            EventData()
                                .append("Action", Action.EditKit.name)
                                .append("KitName", kit.name)
                        )

                        // Rename button
                        updateEventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "$selector #RenameButton",
                            EventData()
                                .append("Action", Action.ShowRenamePopup.name)
                                .append("KitName", kit.name)
                        )

                        // Delete button
                        updateEventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "$selector #DeleteButton",
                            EventData()
                                .append("Action", Action.DeleteKit.name)
                                .append("KitName", kit.name)
                        )
                    }

                    sendUpdate(updateBuilder, updateEventBuilder, false)
                }
            }
            Action.ShowRenamePopup -> {
                val kitName = data.kitName ?: return
                player.pageManager.openCustomPage(ref, store, KitListUiPage(playerRef, searchQuery, kitName))
            }
            Action.ConfirmRename -> {
                val oldName = data.kitName ?: return
                val newName = data.newName?.takeIf { it.isNotBlank() } ?: return
                KitManager.renameKitAsync(oldName, newName).thenAccept { success ->
                    if (success) {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.kits.renamed")
                            .param("name", newName))
                    } else {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.kits.exists")
                            .param("name", newName))
                    }
                    player.pageManager.openCustomPage(ref, store, KitListUiPage(playerRef, searchQuery))
                }
            }
            Action.CancelRename -> {
                player.pageManager.openCustomPage(ref, store, KitListUiPage(playerRef, searchQuery))
            }
        }
    }

    enum class Action {
        EditKit,
        DeleteKit,
        AddKit,
        Search,
        ShowRenamePopup,
        ConfirmRename,
        CancelRename
    }

    class PageData {
        var action: Action = Action.EditKit
        var kitName: String? = null
        var newName: String? = null
        var searchQuery: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .append(
                    KeyedCodec("KitName", Codec.STRING),
                    { obj, kitName -> obj.kitName = kitName },
                    { obj -> obj.kitName }
                ).add()
                .append(
                    KeyedCodec("@KitName", Codec.STRING),
                    { obj, kitName -> obj.kitName = kitName },
                    { obj -> obj.kitName }
                ).add()
                .append(
                    KeyedCodec("@NewName", Codec.STRING),
                    { obj, newName -> obj.newName = newName },
                    { obj -> obj.newName }
                ).add()
                .append(
                    KeyedCodec("@SearchQuery", Codec.STRING),
                    { obj, query -> obj.searchQuery = query },
                    { obj -> obj.searchQuery }
                ).add()
                .build()
        }
    }
}
