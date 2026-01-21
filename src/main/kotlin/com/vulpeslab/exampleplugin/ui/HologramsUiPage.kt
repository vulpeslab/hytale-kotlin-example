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
import com.hypixel.hytale.protocol.packets.interface_.Page
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.HologramManager

/**
 * UI page for managing holograms.
 * Allows viewing all holograms and creating new ones at the player's position.
 */
class HologramsUiPage(
    playerRef: PlayerRef,
    private val world: World,
    private val editingHologramId: String? = null
) : InteractiveCustomUIPage<HologramsUiPage.PageData>(
    playerRef,
    CustomPageLifetime.CanDismiss,
    PageData.CODEC
) {

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/HologramsPage.ui")

        // Bind "Create Hologram" button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CreateButton",
            EventData()
                .append("Action", Action.CreateHologram.name)
                .append("@Text", "#NewHologramInput.Value")
        )

        // Bind Enter key on new hologram input
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#NewHologramInput",
            EventData()
                .append("Action", Action.CreateHologram.name)
                .append("@Text", "#NewHologramInput.Value")
        )

        // Load holograms synchronously (data is in memory)
        val holograms = HologramManager.getAllHolograms()

        holograms.forEachIndexed { index, hologram ->
            val selector = "#HologramList[$index]"
            commandBuilder.append("#HologramList", "Pages/HologramListItem.ui")
            commandBuilder.set("$selector #HologramText.Text", hologram.text)
            commandBuilder.set("$selector #HologramPosition.Text",
                "X: %.1f, Y: %.1f, Z: %.1f".format(hologram.x, hologram.y, hologram.z))

            // Edit button
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #EditButton",
                EventData()
                    .append("Action", Action.ShowEditPopup.name)
                    .append("HologramId", hologram.id)
                    .append("HologramText", hologram.text)
            )

            // Teleport button
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #TeleportButton",
                EventData()
                    .append("Action", Action.Teleport.name)
                    .append("HologramId", hologram.id)
            )

            // Delete button
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                EventData()
                    .append("Action", Action.DeleteHologram.name)
                    .append("HologramId", hologram.id)
            )
        }

        // Show edit popup if editing
        if (editingHologramId != null) {
            val hologram = holograms.find { it.id == editingHologramId }
            if (hologram != null) {
                showEditPopup(commandBuilder, eventBuilder, hologram.id, hologram.text)
            }
        }
    }

    private fun showEditPopup(
        updateBuilder: UICommandBuilder,
        updateEventBuilder: UIEventBuilder,
        hologramId: String,
        currentText: String
    ) {
        updateBuilder.set("#EditPopup.Visible", true)
        updateBuilder.set("#EditInput.Value", currentText)

        // Bind confirm button
        updateEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#EditConfirmButton",
            EventData()
                .append("Action", Action.ConfirmEdit.name)
                .append("HologramId", hologramId)
                .append("@NewText", "#EditInput.Value")
        )

        // Bind Enter key on edit input
        updateEventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#EditInput",
            EventData()
                .append("Action", Action.ConfirmEdit.name)
                .append("HologramId", hologramId)
                .append("@NewText", "#EditInput.Value")
        )

        // Bind cancel button
        updateEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#EditCancelButton",
            EventData().append("Action", Action.CancelEdit.name)
        )
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.CreateHologram -> {
                val text = data.text?.takeIf { it.isNotBlank() } ?: return

                // Get player position
                val transformComponent = store.getComponent(ref, TransformComponent.getComponentType())
                if (transformComponent == null) {
                    playerRef.sendMessage(Message.translation("exampleplugin.ui.holograms.error.position"))
                    return
                }

                val position = transformComponent.position.clone()
                // Spawn hologram at player's position (not above head)

                // Create hologram synchronously (we're already on world thread)
                val hologram = HologramManager.createHologram(text, position, world)
                playerRef.sendMessage(
                    Message.translation("exampleplugin.ui.holograms.created")
                        .param("text", hologram.text)
                )
                player.pageManager.openCustomPage(ref, store, HologramsUiPage(playerRef, world))
            }

            Action.DeleteHologram -> {
                val id = data.hologramId ?: return
                // Delete hologram synchronously (we're already on world thread)
                val success = HologramManager.deleteHologram(id, world)
                if (success) {
                    playerRef.sendMessage(Message.translation("exampleplugin.ui.holograms.deleted"))
                }
                player.pageManager.openCustomPage(ref, store, HologramsUiPage(playerRef, world))
            }

            Action.Teleport -> {
                val id = data.hologramId ?: return
                val hologram = HologramManager.getHologram(id) ?: return

                // Get current rotation to preserve it
                val transformComponent = store.getComponent(ref, TransformComponent.getComponentType())
                val currentRotation = transformComponent?.rotation?.clone()
                    ?: com.hypixel.hytale.math.vector.Vector3f()

                // Create teleport to hologram position
                val targetPosition = com.hypixel.hytale.math.vector.Vector3d(hologram.x, hologram.y, hologram.z)
                val teleport = Teleport.createForPlayer(targetPosition, currentRotation)
                store.addComponent(ref, Teleport.getComponentType(), teleport)

                playerRef.sendMessage(
                    Message.translation("exampleplugin.ui.holograms.teleported")
                        .param("text", hologram.text)
                )

                // Close the UI after teleporting
                player.pageManager.setPage(ref, store, Page.None)
            }

            Action.ShowEditPopup -> {
                val id = data.hologramId ?: return
                player.pageManager.openCustomPage(ref, store, HologramsUiPage(playerRef, world, id))
            }

            Action.ConfirmEdit -> {
                val id = data.hologramId ?: return
                val newText = data.newText?.takeIf { it.isNotBlank() } ?: return

                HologramManager.updateHologramText(id, newText, world)
                playerRef.sendMessage(
                    Message.translation("exampleplugin.ui.holograms.updated")
                        .param("text", newText)
                )
                player.pageManager.openCustomPage(ref, store, HologramsUiPage(playerRef, world))
            }

            Action.CancelEdit -> {
                player.pageManager.openCustomPage(ref, store, HologramsUiPage(playerRef, world))
            }
        }
    }

    enum class Action {
        CreateHologram,
        DeleteHologram,
        Teleport,
        ShowEditPopup,
        ConfirmEdit,
        CancelEdit
    }

    class PageData {
        var action: Action = Action.CreateHologram
        var text: String? = null
        var hologramId: String? = null
        var hologramText: String? = null
        var newText: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .append(
                    KeyedCodec("@Text", Codec.STRING),
                    { obj, text -> obj.text = text },
                    { obj -> obj.text }
                ).add()
                .append(
                    KeyedCodec("HologramId", Codec.STRING),
                    { obj, id -> obj.hologramId = id },
                    { obj -> obj.hologramId }
                ).add()
                .append(
                    KeyedCodec("HologramText", Codec.STRING),
                    { obj, text -> obj.hologramText = text },
                    { obj -> obj.hologramText }
                ).add()
                .append(
                    KeyedCodec("@NewText", Codec.STRING),
                    { obj, text -> obj.newText = text },
                    { obj -> obj.newText }
                ).add()
                .build()
        }
    }
}
