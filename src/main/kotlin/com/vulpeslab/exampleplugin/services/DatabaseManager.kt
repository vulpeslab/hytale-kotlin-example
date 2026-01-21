package com.vulpeslab.exampleplugin.services

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages SQLite database connections and operations.
 * All query operations run asynchronously on a dedicated thread pool.
 */
object DatabaseManager {
    private var connection: Connection? = null
    private lateinit var executor: ExecutorService

    /**
     * Initializes the database connection and creates tables if needed.
     * @param dataPath The plugin data directory path
     */
    fun initialize(dataPath: Path) {
        // Create dedicated thread pool for database operations
        executor = Executors.newFixedThreadPool(2) { runnable ->
            Thread(runnable, "ExamplePlugin-DB").apply { isDaemon = true }
        }

        // Ensure the directory exists
        Files.createDirectories(dataPath)

        // Explicitly load the SQLite JDBC driver
        Class.forName("org.sqlite.JDBC")

        val dbPath = dataPath.resolve("database.sqlite3")
        val url = "jdbc:sqlite:$dbPath"

        connection = DriverManager.getConnection(url)
        createTables()
    }

    /**
     * Closes the database connection and shuts down the thread pool.
     */
    fun shutdown() {
        executor.shutdown()
        connection?.close()
        connection = null
    }

    private fun createTables() {
        connection?.createStatement()?.use { statement ->
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS registrations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    hytale_uuid TEXT NOT NULL UNIQUE,
                    username TEXT NOT NULL,
                    email TEXT NOT NULL,
                    password TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.trimIndent()
            )

            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS player_settings (
                    hytale_uuid TEXT PRIMARY KEY,
                    god_mode INTEGER NOT NULL DEFAULT 0,
                    fly_mode INTEGER NOT NULL DEFAULT 0,
                    was_flying INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
        // Migration: Add hytale_uuid column if it doesn't exist (for existing databases)
        migrateAddUuidColumn()
        // Migration: Add fly_mode column if it doesn't exist
        migrateAddFlyModeColumn()
        // Migration: Add was_flying column if it doesn't exist
        migrateAddWasFlyingColumn()
    }

    private fun migrateAddUuidColumn() {
        connection?.createStatement()?.use { statement ->
            val resultSet = statement.executeQuery("PRAGMA table_info(registrations)")
            var hasUuidColumn = false
            while (resultSet.next()) {
                if (resultSet.getString("name") == "hytale_uuid") {
                    hasUuidColumn = true
                    break
                }
            }
            if (!hasUuidColumn) {
                statement.executeUpdate("ALTER TABLE registrations ADD COLUMN hytale_uuid TEXT")
            }
        }
    }

    private fun migrateAddFlyModeColumn() {
        connection?.createStatement()?.use { statement ->
            val resultSet = statement.executeQuery("PRAGMA table_info(player_settings)")
            var hasFlyModeColumn = false
            while (resultSet.next()) {
                if (resultSet.getString("name") == "fly_mode") {
                    hasFlyModeColumn = true
                    break
                }
            }
            if (!hasFlyModeColumn) {
                statement.executeUpdate("ALTER TABLE player_settings ADD COLUMN fly_mode INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    private fun migrateAddWasFlyingColumn() {
        connection?.createStatement()?.use { statement ->
            val resultSet = statement.executeQuery("PRAGMA table_info(player_settings)")
            var hasWasFlyingColumn = false
            while (resultSet.next()) {
                if (resultSet.getString("name") == "was_flying") {
                    hasWasFlyingColumn = true
                    break
                }
            }
            if (!hasWasFlyingColumn) {
                statement.executeUpdate("ALTER TABLE player_settings ADD COLUMN was_flying INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    /**
     * Checks if a Hytale UUID already has a registered account (async).
     */
    fun hasAccountForUuidAsync(hytaleUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            hasAccountForUuidSync(hytaleUuid)
        }, executor)
    }

    private fun hasAccountForUuidSync(hytaleUuid: UUID): Boolean {
        val sql = "SELECT id FROM registrations WHERE hytale_uuid = ? LIMIT 1"

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setString(1, hytaleUuid.toString())
            statement.executeQuery().use { resultSet ->
                resultSet.next()
            }
        } ?: false
    }

    /**
     * Saves a registration to the database linked to a Hytale UUID (async).
     * @return CompletableFuture with the ID of the inserted row, or -1 on failure
     */
    fun saveRegistrationAsync(hytaleUuid: UUID, username: String, email: String, password: String): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync({
            saveRegistrationSync(hytaleUuid, username, email, password)
        }, executor)
    }

    private fun saveRegistrationSync(hytaleUuid: UUID, username: String, email: String, password: String): Long {
        val sql = "INSERT INTO registrations (hytale_uuid, username, email, password) VALUES (?, ?, ?, ?)"

        return connection?.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)?.use { statement ->
            statement.setString(1, hytaleUuid.toString())
            statement.setString(2, username)
            statement.setString(3, email)
            statement.setString(4, password)
            statement.executeUpdate()

            statement.generatedKeys.use { keys ->
                if (keys.next()) keys.getLong(1) else -1L
            }
        } ?: -1L
    }

    /**
     * Verifies login credentials and checks that the Hytale UUID matches the account (async).
     * @return CompletableFuture with true if credentials are valid and UUID matches, false otherwise
     */
    fun verifyCredentialsAsync(hytaleUuid: UUID, username: String, password: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            verifyCredentialsSync(hytaleUuid, username, password)
        }, executor)
    }

    private fun verifyCredentialsSync(hytaleUuid: UUID, username: String, password: String): Boolean {
        val sql = "SELECT id FROM registrations WHERE hytale_uuid = ? AND username = ? AND password = ? LIMIT 1"

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setString(1, hytaleUuid.toString())
            statement.setString(2, username)
            statement.setString(3, password)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
            }
        } ?: false
    }

    /**
     * Gets all registered users (async).
     */
    fun getAllUsersAsync(): CompletableFuture<List<User>> {
        return CompletableFuture.supplyAsync({
            getAllUsersSync()
        }, executor)
    }

    private fun getAllUsersSync(): List<User> {
        val sql = "SELECT id, hytale_uuid, username, email FROM registrations ORDER BY username"
        val users = mutableListOf<User>()

        connection?.createStatement()?.use { statement ->
            statement.executeQuery(sql).use { resultSet ->
                while (resultSet.next()) {
                    val uuidStr = resultSet.getString("hytale_uuid")
                    users.add(
                        User(
                            id = resultSet.getLong("id"),
                            hytaleUuid = uuidStr?.let { UUID.fromString(it) },
                            username = resultSet.getString("username"),
                            email = resultSet.getString("email")
                        )
                    )
                }
            }
        }

        return users
    }

    /**
     * Gets a user by ID (async).
     */
    fun getUserByIdAsync(id: Long): CompletableFuture<User?> {
        return CompletableFuture.supplyAsync({
            getUserByIdSync(id)
        }, executor)
    }

    private fun getUserByIdSync(id: Long): User? {
        val sql = "SELECT id, hytale_uuid, username, email FROM registrations WHERE id = ?"

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setLong(1, id)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    val uuidStr = resultSet.getString("hytale_uuid")
                    User(
                        id = resultSet.getLong("id"),
                        hytaleUuid = uuidStr?.let { UUID.fromString(it) },
                        username = resultSet.getString("username"),
                        email = resultSet.getString("email")
                    )
                } else null
            }
        }
    }

    /**
     * Updates a user's data. Only updates fields that are not null (async).
     */
    fun updateUserAsync(id: Long, username: String?, email: String?, password: String?): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            updateUserSync(id, username, email, password)
        }, executor)
    }

    private fun updateUserSync(id: Long, username: String?, email: String?, password: String?): Boolean {
        val updates = mutableListOf<String>()
        if (username != null) updates.add("username = ?")
        if (email != null) updates.add("email = ?")
        if (password != null) updates.add("password = ?")

        if (updates.isEmpty()) return false

        val sql = "UPDATE registrations SET ${updates.joinToString(", ")} WHERE id = ?"

        return connection?.prepareStatement(sql)?.use { statement ->
            var paramIndex = 1
            if (username != null) statement.setString(paramIndex++, username)
            if (email != null) statement.setString(paramIndex++, email)
            if (password != null) statement.setString(paramIndex++, password)
            statement.setLong(paramIndex, id)
            statement.executeUpdate() > 0
        } ?: false
    }

    data class User(
        val id: Long,
        val hytaleUuid: UUID?,
        val username: String,
        val email: String
    )

    // ===================
    // God Mode Methods
    // ===================

    /**
     * Checks if a player has god mode enabled (async).
     */
    fun hasGodModeAsync(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            hasGodModeSync(uuid)
        }, executor)
    }

    private fun hasGodModeSync(uuid: UUID): Boolean {
        val sql = "SELECT god_mode FROM player_settings WHERE hytale_uuid = ?"

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setString(1, uuid.toString())
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    resultSet.getInt("god_mode") == 1
                } else {
                    false
                }
            }
        } ?: false
    }

    /**
     * Sets god mode status for a player (async).
     * Creates the player_settings row if it doesn't exist.
     * @return CompletableFuture with true if successful, false otherwise
     */
    fun setGodModeAsync(uuid: UUID, enabled: Boolean): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            setGodModeSync(uuid, enabled)
        }, executor)
    }

    private fun setGodModeSync(uuid: UUID, enabled: Boolean): Boolean {
        val sql = """
            INSERT INTO player_settings (hytale_uuid, god_mode) VALUES (?, ?)
            ON CONFLICT(hytale_uuid) DO UPDATE SET god_mode = excluded.god_mode
        """.trimIndent()

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setString(1, uuid.toString())
            statement.setInt(2, if (enabled) 1 else 0)
            statement.executeUpdate() > 0
        } ?: false
    }

    // ===================
    // Fly Mode Methods
    // ===================

    /**
     * Checks if a player has fly mode enabled (async).
     */
    fun hasFlyModeAsync(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            hasFlyModeSync(uuid)
        }, executor)
    }

    private fun hasFlyModeSync(uuid: UUID): Boolean {
        val sql = "SELECT fly_mode FROM player_settings WHERE hytale_uuid = ?"

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setString(1, uuid.toString())
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    resultSet.getInt("fly_mode") == 1
                } else {
                    false
                }
            }
        } ?: false
    }

    /**
     * Sets fly mode status for a player (async).
     * Creates the player_settings row if it doesn't exist.
     * @return CompletableFuture with true if successful, false otherwise
     */
    fun setFlyModeAsync(uuid: UUID, enabled: Boolean): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            setFlyModeSync(uuid, enabled)
        }, executor)
    }

    private fun setFlyModeSync(uuid: UUID, enabled: Boolean): Boolean {
        val sql = """
            INSERT INTO player_settings (hytale_uuid, fly_mode) VALUES (?, ?)
            ON CONFLICT(hytale_uuid) DO UPDATE SET fly_mode = excluded.fly_mode
        """.trimIndent()

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setString(1, uuid.toString())
            statement.setInt(2, if (enabled) 1 else 0)
            statement.executeUpdate() > 0
        } ?: false
    }

    /**
     * Checks if a player was flying when they disconnected (async).
     */
    fun wasFlyingAsync(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            wasFlyingSync(uuid)
        }, executor)
    }

    private fun wasFlyingSync(uuid: UUID): Boolean {
        val sql = "SELECT was_flying FROM player_settings WHERE hytale_uuid = ?"

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setString(1, uuid.toString())
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    resultSet.getInt("was_flying") == 1
                } else {
                    false
                }
            }
        } ?: false
    }

    /**
     * Sets whether the player was flying when they disconnected (async).
     */
    fun setWasFlyingAsync(uuid: UUID, wasFlying: Boolean): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            setWasFlyingSync(uuid, wasFlying)
        }, executor)
    }

    private fun setWasFlyingSync(uuid: UUID, wasFlying: Boolean): Boolean {
        val sql = """
            INSERT INTO player_settings (hytale_uuid, was_flying) VALUES (?, ?)
            ON CONFLICT(hytale_uuid) DO UPDATE SET was_flying = excluded.was_flying
        """.trimIndent()

        return connection?.prepareStatement(sql)?.use { statement ->
            statement.setString(1, uuid.toString())
            statement.setInt(2, if (wasFlying) 1 else 0)
            statement.executeUpdate() > 0
        } ?: false
    }
}
