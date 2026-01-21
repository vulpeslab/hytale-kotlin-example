package com.vulpeslab.exampleplugin.services

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages player login sessions in memory.
 */
object SessionManager {
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Logs in a player with the given account.
     */
    fun login(playerUuid: UUID, username: String) {
        sessions[playerUuid] = Session(username)
    }

    /**
     * Logs out a player.
     */
    fun logout(playerUuid: UUID) {
        sessions.remove(playerUuid)
    }

    /**
     * Checks if a player is logged in.
     */
    fun isLoggedIn(playerUuid: UUID): Boolean {
        return sessions.containsKey(playerUuid)
    }

    /**
     * Gets the session for a player, or null if not logged in.
     */
    fun getSession(playerUuid: UUID): Session? {
        return sessions[playerUuid]
    }

    /**
     * Clears all sessions (used on plugin shutdown).
     */
    fun clear() {
        sessions.clear()
    }

    data class Session(
        val username: String
    )
}
