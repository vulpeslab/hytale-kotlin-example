package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.WaypointManager

/**
 * Teleports the player to a named waypoint.
 * Usage: /waypoint <name>
 * Permission: vulpeslab.exampleplugin.command.waypoint (auto-generated base permission)
 * Permission: vulpeslab.exampleplugin.waypoint.<name> (required for each specific waypoint)
 */
class WaypointCommand : AbstractPlayerCommand("waypoint", "exampleplugin.commands.waypoint.description") {

    companion object {
        private const val PERMISSION_BASE = "vulpeslab.exampleplugin.waypoint"
    }

    private val waypointNameArg: RequiredArg<String> = withRequiredArg(
        "name",
        "exampleplugin.commands.waypoint.name.desc",
        ArgTypes.STRING
    )

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        requireNotNull(player)

        val waypointName = waypointNameArg.get(context)

        // Check waypoint-specific permission
        val waypointPermission = "$PERMISSION_BASE.${waypointName.lowercase()}"
        if (!context.sender().hasPermission(waypointPermission)) {
            context.sendMessage(Message.translation("exampleplugin.commands.waypoint.nopermission")
                .param("name", waypointName))
            return
        }

        WaypointManager.getWaypointAsync(waypointName).thenAccept { waypoint ->
            world.execute {
                if (waypoint == null) {
                    context.sendMessage(Message.translation("exampleplugin.commands.waypoint.notfound")
                        .param("name", waypointName))
                    return@execute
                }

                // Check if same world - cross-world teleport would require additional handling
                if (world.name != waypoint.worldName) {
                    context.sendMessage(Message.translation("exampleplugin.commands.waypoint.differentworld")
                        .param("world", waypoint.worldName))
                    return@execute
                }

                // Teleport the player to the waypoint
                val position = Vector3d(waypoint.x, waypoint.y, waypoint.z)
                val rotation = Vector3f(0f, waypoint.yaw, 0f)
                val teleport = Teleport.createForPlayer(position, rotation)

                // Add teleport component to trigger the teleport
                store.addComponent(ref, Teleport.getComponentType(), teleport)

                // Send success message
                context.sendMessage(Message.translation("exampleplugin.commands.waypoint.teleported")
                    .param("name", waypoint.name))
            }
        }
    }
}
