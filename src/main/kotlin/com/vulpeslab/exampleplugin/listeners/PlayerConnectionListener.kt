package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.vulpeslab.exampleplugin.services.UpdateChecker

/**
 * Listener that broadcasts join/leave messages when players connect/disconnect.
 * Also notifies players with permission about available updates.
 */
object PlayerConnectionListener {

    /**
     * Called when a player is ready in the world.
     * Broadcasts the configured join message to all players.
     * Notifies players with update permission about available updates.
     */
    fun onPlayerReady(event: PlayerReadyEvent) {
        val player = event.player
        if (player.hasPermission(UpdateChecker.UPDATE_NOTIFY_PERMISSION)) {
            checkAndNotifyUpdate(event)
        }
    }

    /**
     * Checks for updates and notifies the player if an update is available.
     * Uses cached result if available, otherwise fetches from GitHub.
     */
    private fun checkAndNotifyUpdate(event: PlayerReadyEvent) {
        val player = event.player

        // Try cached result first for immediate response
        val cachedInfo = UpdateChecker.getCachedUpdateInfo()
        if (cachedInfo != null) {
            if (cachedInfo.isUpdateAvailable) {
                @Suppress("DEPRECATION")
                sendUpdateNotification(player.getPlayerConnection(), cachedInfo.latestVersion!!)
            }
            return
        }

        // Fetch update info asynchronously
        UpdateChecker.checkForUpdateAsync().thenAccept { updateInfo ->
            if (updateInfo.isUpdateAvailable && updateInfo.latestVersion != null) {
                @Suppress("DEPRECATION")
                sendUpdateNotification(player.getPlayerConnection(), updateInfo.latestVersion)
            }
        }
    }

    /**
     * Sends an update notification message to a player.
     */
    private fun sendUpdateNotification(packetHandler: com.hypixel.hytale.server.core.io.PacketHandler, latestVersion: String) {
        // Empty line
        packetHandler.writeNoCache(
            com.hypixel.hytale.protocol.packets.interface_.ServerMessage(
                com.hypixel.hytale.protocol.packets.interface_.ChatType.Chat,
                Message.raw("").formattedMessage
            )
        )
        // Update available message
        packetHandler.writeNoCache(
            com.hypixel.hytale.protocol.packets.interface_.ServerMessage(
                com.hypixel.hytale.protocol.packets.interface_.ChatType.Chat,
                Message.empty()
                    .insert(
                        Message.translation("exampleplugin.ui.update.prefix")
                            .param("name", UpdateChecker.PLUGIN_NAME)
                            .color("#f39c12")
                    )
                    .insert(
                        Message.translation("exampleplugin.ui.update.available")
                            .color("#bdc3c7")
                    )
                    .insert(Message.raw(" v$latestVersion").color("#2ecc71").bold(true))
                    .formattedMessage
            )
        )
        // Download link message
        packetHandler.writeNoCache(
            com.hypixel.hytale.protocol.packets.interface_.ServerMessage(
                com.hypixel.hytale.protocol.packets.interface_.ChatType.Chat,
                Message.empty()
                    .insert(
                        Message.translation("exampleplugin.ui.update.prefix")
                            .param("name", UpdateChecker.PLUGIN_NAME)
                            .color("#f39c12")
                    )
                    .insert(
                        Message.translation("exampleplugin.ui.update.download")
                            .color("#bdc3c7")
                    )
                    .insert(
                        Message.raw(" " + UpdateChecker.GITHUB_RELEASES_URL)
                            .link(UpdateChecker.GITHUB_RELEASES_URL)
                            .color("#3498db")
                    )
                    .formattedMessage
            )
        )
    }
}
