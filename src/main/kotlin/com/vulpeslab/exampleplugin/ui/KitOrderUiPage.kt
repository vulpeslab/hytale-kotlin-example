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
import com.vulpeslab.exampleplugin.services.KitManager

/**
 * UI page for reordering items in a kit using up/down arrows.
 */
class KitOrderUiPage(
    playerRef: PlayerRef,
    private val kitName: String
) : InteractiveCustomUIPage<KitOrderUiPage.PageData>(
    playerRef,
    CustomPageLifetime.CanDismiss,
    PageData.CODEC
) {

    companion object {
        private val ARROW_DISABLED_STYLE = Value.ref<String>("Pages/ExamplePluginKitOrderItem.ui", "ArrowDisabledStyle")
    }

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/ExamplePluginKitOrderPage.ui")

        // Set the kit name in title
        commandBuilder.set("#KitNameLabel.Text", kitName)

        // Bind back button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BackButton",
            EventData().append("Action", Action.Back.name)
        )

        // Load kit items
        KitManager.getKitAsync(kitName).thenAccept { kit ->
            if (kit == null) {
                playerRef.sendMessage(Message.translation("exampleplugin.commands.editkit.notfound")
                    .param("name", kitName))
                return@thenAccept
            }

            val updateBuilder = UICommandBuilder()
            val updateEventBuilder = UIEventBuilder()

            updateBuilder.clear("#ItemList")

            if (kit.items.isEmpty()) {
                updateBuilder.appendInline("#ItemList", 
                    "Label { Style: LabelStyle(FontSize: 14, TextColor: #8090a0); Padding: (Full: 20); Text: \"No items in kit.\"; }")
            } else {
                kit.items.forEachIndexed { index, item ->
                    val selector = "#ItemList[$index]"
                    updateBuilder.append("#ItemList", "Pages/ExamplePluginKitOrderItem.ui")

                    // Set index (1-based for display)
                    updateBuilder.set("$selector #IndexLabel.Text", "${index + 1}.")

                    // Set item icon
                    updateBuilder.set("$selector #ItemIcon.ItemId", item.itemId)

                    // Set item name (translated)
                    val itemAsset = Item.getAssetMap().getAsset(item.itemId)
                    val translationKey = itemAsset?.translationKey ?: "server.items.${item.itemId}.name"
                    updateBuilder.set("$selector #ItemNameLabel.Text", Message.translation(translationKey))

                    // Set quantity
                    updateBuilder.set("$selector #QuantityLabel.Text", "x${item.quantity}")

                    // Move up button (disabled for first item)
                    if (index == 0) {
                        updateBuilder.set("$selector #MoveUpButton.Style", ARROW_DISABLED_STYLE)
                    } else {
                        updateEventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "$selector #MoveUpButton",
                            EventData()
                                .append("Action", Action.MoveUp.name)
                                .append("Index", index.toString())
                        )
                    }

                    // Move down button (disabled for last item)
                    if (index == kit.items.lastIndex) {
                        updateBuilder.set("$selector #MoveDownButton.Style", ARROW_DISABLED_STYLE)
                    } else {
                        updateEventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "$selector #MoveDownButton",
                            EventData()
                                .append("Action", Action.MoveDown.name)
                                .append("Index", index.toString())
                        )
                    }
                }
            }

            sendUpdate(updateBuilder, updateEventBuilder, false)
        }
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.MoveUp -> {
                val index = data.index?.toIntOrNull() ?: return
                if (index > 0) {
                    KitManager.moveItemInKitAsync(kitName, index, index - 1).thenAccept {
                        // Update list in-place to preserve scrollbar position
                        refreshItemList()
                    }
                }
            }
            Action.MoveDown -> {
                val index = data.index?.toIntOrNull() ?: return
                KitManager.moveItemInKitAsync(kitName, index, index + 1).thenAccept {
                    // Update list in-place to preserve scrollbar position
                    refreshItemList()
                }
            }
            Action.Back -> {
                player.pageManager.openCustomPage(ref, store, ItemBrowserUiPage(playerRef, kitName))
            }
        }
    }

    private fun refreshItemList() {
        KitManager.getKitAsync(kitName).thenAccept { kit ->
            if (kit == null) {
                playerRef.sendMessage(Message.translation("exampleplugin.commands.editkit.notfound")
                    .param("name", kitName))
                return@thenAccept
            }

            val updateBuilder = UICommandBuilder()
            val updateEventBuilder = UIEventBuilder()

            updateBuilder.clear("#ItemList")

            if (kit.items.isEmpty()) {
                updateBuilder.appendInline("#ItemList",
                    "Label { Style: LabelStyle(FontSize: 14, TextColor: #8090a0); Padding: (Full: 20); Text: \"No items in kit.\"; }")
            } else {
                kit.items.forEachIndexed { index, item ->
                    val selector = "#ItemList[$index]"
                    updateBuilder.append("#ItemList", "Pages/ExamplePluginKitOrderItem.ui")

                    updateBuilder.set("$selector #IndexLabel.Text", "${index + 1}.")
                    updateBuilder.set("$selector #ItemIcon.ItemId", item.itemId)

                    val itemAsset = Item.getAssetMap().getAsset(item.itemId)
                    val translationKey = itemAsset?.translationKey ?: "server.items.${item.itemId}.name"
                    updateBuilder.set("$selector #ItemNameLabel.Text", Message.translation(translationKey))

                    updateBuilder.set("$selector #QuantityLabel.Text", "x${item.quantity}")

                    if (index == 0) {
                        updateBuilder.set("$selector #MoveUpButton.Style", ARROW_DISABLED_STYLE)
                    } else {
                        updateEventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "$selector #MoveUpButton",
                            EventData()
                                .append("Action", Action.MoveUp.name)
                                .append("Index", index.toString())
                        )
                    }

                    if (index == kit.items.lastIndex) {
                        updateBuilder.set("$selector #MoveDownButton.Style", ARROW_DISABLED_STYLE)
                    } else {
                        updateEventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "$selector #MoveDownButton",
                            EventData()
                                .append("Action", Action.MoveDown.name)
                                .append("Index", index.toString())
                        )
                    }
                }
            }

            sendUpdate(updateBuilder, updateEventBuilder, false)
        }
    }

    enum class Action {
        MoveUp,
        MoveDown,
        Back
    }

    class PageData {
        var action: Action = Action.Back
        var index: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .append(
                    KeyedCodec("Index", Codec.STRING),
                    { obj, index -> obj.index = index },
                    { obj -> obj.index }
                ).add()
                .build()
        }
    }
}
