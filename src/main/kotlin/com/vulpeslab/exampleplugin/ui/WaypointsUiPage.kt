package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.EnumCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.protocol.packets.interface_.Page
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.WaypointManager

/**
 * UI page for managing server waypoints (admin only).
 * Allows creating, updating, deleting, and teleporting to waypoints.
 */
class WaypointsUiPage(
    playerRef: PlayerRef,
    private val searchQuery: String = ""
) : InteractiveCustomUIPage<WaypointsUiPage.PageData>(
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
        commandBuilder.append("Pages/ExamplePluginWaypointsPage.ui")

        // Restore search value if we had one
        if (searchQuery.isNotEmpty()) {
            commandBuilder.set("#SearchInput.Value", searchQuery)
        }

        // Bind "Add Waypoint" button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#AddWaypointButton",
            EventData()
                .append("Action", Action.AddWaypoint.name)
                .append("@WaypointName", "#NewWaypointNameInput.Value")
        )

        // Bind Enter key on new waypoint name input
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#NewWaypointNameInput",
            EventData()
                .append("Action", Action.AddWaypoint.name)
                .append("@WaypointName", "#NewWaypointNameInput.Value")
        )

        // Bind Enter key on search input
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#SearchInput",
            EventData()
                .append("Action", Action.Search.name)
                .append("@SearchQuery", "#SearchInput.Value")
        )

        // Bind search button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SearchButton",
            EventData()
                .append("Action", Action.Search.name)
                .append("@SearchQuery", "#SearchInput.Value")
        )

        // Load waypoints asynchronously and update the UI
        WaypointManager.getAllWaypointsAsync().thenAccept { allWaypoints ->
            val updateBuilder = UICommandBuilder()
            val updateEventBuilder = UIEventBuilder()

            // Filter waypoints by search query
            val waypoints = if (searchQuery.isNotBlank()) {
                allWaypoints.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                allWaypoints
            }

            updateBuilder.clear("#WaypointList")

            if (waypoints.isEmpty()) {
                updateBuilder.appendInline("#WaypointList",
                    "Label { Style: LabelStyle(FontSize: 14, TextColor: #607080); Padding: (Full: 20); Text: %exampleplugin.ui.waypoints.empty; }")
            } else {
                waypoints.forEachIndexed { index, waypoint ->
                    val selector = "#WaypointList[$index]"
                    updateBuilder.append("#WaypointList", "Pages/ExamplePluginWaypointListItem.ui")
                    updateBuilder.set("$selector #WaypointNameLabel.Text", waypoint.name)
                    updateBuilder.set("$selector #WaypointCoordsLabel.Text",
                        "X: ${waypoint.x.toInt()}, Y: ${waypoint.y.toInt()}, Z: ${waypoint.z.toInt()}")
                    updateBuilder.set("$selector #WaypointWorldLabel.Text", waypoint.worldName)

                    // Teleport button (left-click)
                    updateEventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "$selector #TeleportButton",
                        EventData()
                            .append("Action", Action.TeleportToWaypoint.name)
                            .append("WaypointName", waypoint.name)
                    )

                    // Update button (set to current position)
                    updateEventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "$selector #UpdateButton",
                        EventData()
                            .append("Action", Action.UpdateWaypoint.name)
                            .append("WaypointName", waypoint.name)
                    )

                    // Delete button
                    updateEventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "$selector #DeleteButton",
                        EventData()
                            .append("Action", Action.DeleteWaypoint.name)
                            .append("WaypointName", waypoint.name)
                    )
                }
            }

            sendUpdate(updateBuilder, updateEventBuilder, false)
        }
    }

    private fun showError(message: Message) {
        val commandBuilder = UICommandBuilder()
        commandBuilder.set("#ErrorLabel.TextSpans", message)
        commandBuilder.set("#ErrorLabel.Visible", true)
        sendUpdate(commandBuilder, null, false)
    }

    private fun hideError() {
        val commandBuilder = UICommandBuilder()
        commandBuilder.set("#ErrorLabel.Visible", false)
        sendUpdate(commandBuilder, null, false)
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        val world = player.world ?: return

        when (data.action) {
            Action.AddWaypoint -> {
                val waypointName = data.waypointName?.takeIf { it.isNotBlank() }
                if (waypointName == null) {
                    showError(Message.translation("exampleplugin.ui.waypoints.emptyname"))
                    return
                }

                // Hide any previous error
                hideError()

                // Get player's current position and rotation
                val transform = store.getComponent(ref, TransformComponent.getComponentType()) ?: return
                val position = transform.position
                val headRotation = store.getComponent(ref, HeadRotation.getComponentType())
                val yaw = headRotation?.rotation?.yaw ?: transform.rotation.yaw

                WaypointManager.createWaypointAsync(
                    waypointName,
                    world.name,
                    position.x,
                    position.y,
                    position.z,
                    yaw
                ).thenAccept { waypoint ->
                    if (waypoint != null) {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.waypoints.created")
                            .param("name", waypointName))
                    } else {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.waypoints.exists")
                            .param("name", waypointName))
                    }
                    player.pageManager.openCustomPage(ref, store, WaypointsUiPage(playerRef, searchQuery))
                }
            }

            Action.TeleportToWaypoint -> {
                val waypointName = data.waypointName ?: return

                // Close the UI immediately before teleporting
                player.pageManager.setPage(ref, store, Page.None)

                WaypointManager.getWaypointAsync(waypointName).thenAccept { waypoint ->
                    world.execute {
                        if (waypoint == null) {
                            playerRef.sendMessage(Message.translation("exampleplugin.commands.waypoint.notfound")
                                .param("name", waypointName))
                            return@execute
                        }

                        // Check if same world
                        if (world.name != waypoint.worldName) {
                            playerRef.sendMessage(Message.translation("exampleplugin.commands.waypoint.differentworld")
                                .param("world", waypoint.worldName))
                            return@execute
                        }

                        // Teleport the player
                        val position = Vector3d(waypoint.x, waypoint.y, waypoint.z)
                        val rotation = Vector3f(0f, waypoint.yaw, 0f)
                        val teleport = Teleport.createForPlayer(position, rotation)
                        store.addComponent(ref, Teleport.getComponentType(), teleport)

                        playerRef.sendMessage(Message.translation("exampleplugin.commands.waypoint.teleported")
                            .param("name", waypoint.name))
                    }
                }
            }

            Action.UpdateWaypoint -> {
                val waypointName = data.waypointName ?: return

                // Get player's current position and rotation
                val transform = store.getComponent(ref, TransformComponent.getComponentType()) ?: return
                val position = transform.position
                val headRotation = store.getComponent(ref, HeadRotation.getComponentType())
                val yaw = headRotation?.rotation?.yaw ?: transform.rotation.yaw

                WaypointManager.updateWaypointAsync(
                    waypointName,
                    world.name,
                    position.x,
                    position.y,
                    position.z,
                    yaw
                ).thenAccept { success ->
                    if (success) {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.waypoints.updated")
                            .param("name", waypointName))
                    }
                    player.pageManager.openCustomPage(ref, store, WaypointsUiPage(playerRef, searchQuery))
                }
            }

            Action.DeleteWaypoint -> {
                val waypointName = data.waypointName ?: return
                WaypointManager.deleteWaypointAsync(waypointName).thenAccept { success ->
                    if (success) {
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.waypoints.deleted")
                            .param("name", waypointName))
                    }
                    player.pageManager.openCustomPage(ref, store, WaypointsUiPage(playerRef, searchQuery))
                }
            }

            Action.Search -> {
                val query = data.searchQuery ?: ""
                player.pageManager.openCustomPage(ref, store, WaypointsUiPage(playerRef, query))
            }
        }
    }

    enum class Action {
        AddWaypoint,
        TeleportToWaypoint,
        UpdateWaypoint,
        DeleteWaypoint,
        Search
    }

    class PageData {
        var action: Action = Action.AddWaypoint
        var waypointName: String? = null
        var searchQuery: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .append(
                    KeyedCodec("WaypointName", Codec.STRING),
                    { obj, name -> obj.waypointName = name },
                    { obj -> obj.waypointName }
                ).add()
                .append(
                    KeyedCodec("@WaypointName", Codec.STRING),
                    { obj, name -> obj.waypointName = name },
                    { obj -> obj.waypointName }
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
