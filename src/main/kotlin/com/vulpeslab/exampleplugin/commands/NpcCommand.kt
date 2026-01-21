package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.NpcEditModeManager
import com.vulpeslab.exampleplugin.services.TraderNpcManager

/**
 * Main NPC command collection for managing trader NPCs.
 * Usage: /npc <subcommand>
 * Subcommands: create, edit, clear
 */
class NpcCommand : AbstractCommandCollection("npc", "exampleplugin.commands.npc.description") {

    init {
        addSubCommand(CreateCommand())
        addSubCommand(EditCommand())
        addSubCommand(ClearCommand())
    }

    /**
     * Creates a new trader NPC at the player's current location.
     * Usage: /npc create <name>
     */
    private class CreateCommand : AbstractPlayerCommand("create", "exampleplugin.commands.npc.create.description") {
        private val nameArg: RequiredArg<String> = withRequiredArg(
            "name",
            "exampleplugin.commands.npc.create.name.desc",
            ArgTypes.STRING
        )

        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType()) ?: return
            val transform = store.getComponent(ref, TransformComponent.getComponentType()) ?: return

            // Strip quotes if user wrapped the name in them
            val rawName = nameArg.get(context)
            val name = rawName.trim('"', '\'')
            val position = Vector3d(transform.position.x, transform.position.y, transform.position.z)

            val npc = TraderNpcManager.createNpc(name, position, world)

            playerRef.sendMessage(
                Message.translation("exampleplugin.commands.npc.created")
                    .param("name", npc.name)
                    .param("id", npc.id)
            )
        }
    }

    /**
     * Toggles NPC edit mode for the player.
     * Usage: /npc edit
     */
    private class EditCommand : AbstractPlayerCommand("edit", "exampleplugin.commands.npc.edit.description") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType()) ?: return
            val uuid = player.uuid ?: return

            val nowEnabled = NpcEditModeManager.toggleEditMode(uuid)
            if (nowEnabled) {
                playerRef.sendMessage(Message.translation("exampleplugin.commands.npc.editmode.enabled"))
            } else {
                playerRef.sendMessage(Message.translation("exampleplugin.commands.npc.editmode.disabled"))
            }
        }
    }

    /**
     * Removes all trader NPCs from the world.
     * Usage: /npc clear
     */
    private class ClearCommand : AbstractPlayerCommand("clear", "exampleplugin.commands.npc.clear.description") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val count = TraderNpcManager.clearAllNpcs(world)

            playerRef.sendMessage(
                Message.translation("exampleplugin.commands.npc.cleared")
                    .param("count", count.toString())
            )
        }
    }
}
