package com.vulpeslab.exampleplugin.services

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages which players are in NPC edit mode.
 * When in edit mode, interacting with a trader NPC opens the config UI instead of the trade UI.
 */
object NpcEditModeManager {
    private val playersInEditMode = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Checks if a player is in edit mode.
     */
    fun isInEditMode(playerUuid: UUID): Boolean {
        return playersInEditMode.contains(playerUuid)
    }

    /**
     * Enables edit mode for a player.
     */
    fun enableEditMode(playerUuid: UUID) {
        playersInEditMode.add(playerUuid)
    }

    /**
     * Disables edit mode for a player.
     */
    fun disableEditMode(playerUuid: UUID) {
        playersInEditMode.remove(playerUuid)
    }

    /**
     * Toggles edit mode for a player.
     * @return true if edit mode is now enabled, false if disabled
     */
    fun toggleEditMode(playerUuid: UUID): Boolean {
        return if (playersInEditMode.contains(playerUuid)) {
            playersInEditMode.remove(playerUuid)
            false
        } else {
            playersInEditMode.add(playerUuid)
            true
        }
    }

    /**
     * Clears all edit mode states (used on plugin shutdown).
     */
    fun clear() {
        playersInEditMode.clear()
    }
}
