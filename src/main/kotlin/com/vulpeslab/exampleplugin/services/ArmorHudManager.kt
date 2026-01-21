package com.vulpeslab.exampleplugin.services

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.ui.ArmorHud
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the armor HUD for players.
 */
object ArmorHudManager {

    private data class HudState(
        val hud: ArmorHud,
        val ref: Ref<EntityStore>,
        val enabled: Boolean
    )

    private val playerHuds = ConcurrentHashMap<UUID, HudState>()

    fun enableHud(uuid: UUID, playerRef: PlayerRef, ref: Ref<EntityStore>, store: Store<EntityStore>) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        val hud = ArmorHud(playerRef, ref, store)
        playerHuds[uuid] = HudState(hud, ref, true)
        player.hudManager.setCustomHud(playerRef, hud)
    }

    fun disableHud(uuid: UUID, playerRef: PlayerRef, ref: Ref<EntityStore>, store: Store<EntityStore>) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        playerHuds.remove(uuid)
        player.hudManager.setCustomHud(playerRef, null)
    }

    fun toggleHud(uuid: UUID, playerRef: PlayerRef, ref: Ref<EntityStore>, store: Store<EntityStore>): Boolean {
        return if (isHudEnabled(uuid)) {
            disableHud(uuid, playerRef, ref, store)
            false
        } else {
            enableHud(uuid, playerRef, ref, store)
            true
        }
    }

    fun isHudEnabled(uuid: UUID): Boolean {
        return playerHuds[uuid]?.enabled == true
    }

    fun refreshHud(uuid: UUID) {
        val state = playerHuds[uuid] ?: return
        if (!state.enabled) return
        // Check if the entity ref is still valid before refreshing
        if (!state.ref.isValid) {
            playerHuds.remove(uuid)
            return
        }
        state.hud.refresh()
    }

    fun clear() {
        playerHuds.clear()
    }

    fun removePlayer(uuid: UUID) {
        playerHuds.remove(uuid)
    }
}
