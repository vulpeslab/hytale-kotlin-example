package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.permissions.PermissionsModule
import com.hypixel.hytale.server.core.universe.PlayerRef

/**
 * Chat listener that formats messages differently for OPs and regular players.
 * - OPs: Red, bold text with "[OP] Name: Text" format
 * - Players: White text with "[Player] Name: Text" format
 */
object ChatFormatListener {

    private val OP_FORMATTER = PlayerChatEvent.Formatter { playerRef: PlayerRef, message: String ->
        Message.raw("[OP] ${playerRef.username}: $message")
            .color("#FF0000")
            .bold(true)
    }

    private val PLAYER_FORMATTER = PlayerChatEvent.Formatter { playerRef: PlayerRef, message: String ->
        Message.raw("[Player] ${playerRef.username}: $message")
            .color("#FFFFFF")
    }

    /**
     * Handles the chat event and sets the appropriate formatter based on OP status.
     */
    fun onPlayerChat(event: PlayerChatEvent) {
        val sender = event.sender
        val uuid = sender.uuid

        val groups = PermissionsModule.get().getGroupsForUser(uuid)
        val isOp = groups.contains("OP")

        event.formatter = if (isOp) OP_FORMATTER else PLAYER_FORMATTER
    }
}
