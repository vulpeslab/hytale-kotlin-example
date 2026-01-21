package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.util.NotificationUtil
import com.vulpeslab.exampleplugin.services.DatabaseManager

/**
 * Listener that restores god mode state when players are ready in the world.
 */
object GodModeListener {

    /**
     * Called when a player is ready in the world (after all initialization is complete).
     * Checks the database for god mode status and applies Invulnerable component if enabled.
     */
    fun onPlayerReady(event: PlayerReadyEvent) {
        val player = event.player
        val ref = event.playerRef
        val store = ref.store

        // Get PlayerRef component to access UUID and packet handler
        val playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType())
            ?: return

        val uuid = playerRefComponent.uuid

        // Check database for god mode status (synchronous to apply immediately)
        val hasGodMode = DatabaseManager.hasGodModeAsync(uuid).join()

        if (hasGodMode) {
            // Apply invulnerable component
            store.ensureComponent(ref, Invulnerable.getComponentType())

            // Send reminder notification
            NotificationUtil.sendNotification(
                playerRefComponent.packetHandler,
                Message.translation("exampleplugin.ui.god.reminder")
            )
        }
    }
}
