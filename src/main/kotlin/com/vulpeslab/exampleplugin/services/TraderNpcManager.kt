package com.vulpeslab.exampleplugin.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.server.npc.entities.NPCEntity
import java.io.File
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Represents a single trade offered by an NPC.
 */
data class TradeData(
    val inputItemId: String,
    val inputQuantity: Int,
    val outputItemId: String,
    val outputQuantity: Int
)

/**
 * Represents a trader NPC's persistent data.
 */
data class TraderNpcData(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val trades: MutableList<TradeData> = mutableListOf()
)

/**
 * Manages trader NPC creation, deletion, persistence, and entity spawning.
 */
object TraderNpcManager {
    // Custom NPC role that has interaction but doesn't open barter shop
    private const val NPC_ROLE = "ExamplePlugin_Trader"
    private val logger: Logger = Logger.getLogger("TraderNpcManager")
    private lateinit var npcsFile: File
    private val npcs = ConcurrentHashMap<String, TraderNpcData>()
    private val entityRefs = ConcurrentHashMap<String, Ref<EntityStore>>()
    // Track NPCs we've already attempted to spawn this session to prevent duplicates
    private val spawnedThisSession = ConcurrentHashMap.newKeySet<String>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Initializes the TraderNpcManager with the plugin's data directory.
     */
    fun initialize(dataDirectory: Path) {
        npcsFile = dataDirectory.resolve("trader_npcs.json").toFile()
        loadNpcs()
    }

    /**
     * Recovers refs for NPCs that already exist in the world (from game's persistence).
     * Only spawns NPCs that are missing from the world.
     */
    fun recoverOrSpawnNpcs(world: World) {
        val store = world.entityStore.store
        val markerType = TraderNpcMarker.getComponentType()

        // First, scan the world and recover refs for existing entities
        store.forEachChunk(
            markerType,
            java.util.function.BiConsumer { chunk: com.hypixel.hytale.component.ArchetypeChunk<EntityStore>, _: com.hypixel.hytale.component.CommandBuffer<EntityStore> ->
                for (i in 0 until chunk.size()) {
                    val marker = chunk.getComponent(i, markerType) ?: continue
                    val ref = chunk.getReferenceTo(i)
                    if (npcs.containsKey(marker.npcId)) {
                        logger.info("Recovered existing trader NPC entity: ${marker.npcId}")
                        entityRefs[marker.npcId] = ref
                        spawnedThisSession.add(marker.npcId)
                    } else {
                        // Entity exists but not in our data - remove it (orphaned)
                        logger.info("Removing orphaned trader NPC entity: ${marker.npcId}")
                        store.removeEntity(ref, RemoveReason.REMOVE)
                    }
                }
            }
        )

        // Now spawn any NPCs that weren't found in the world
        npcs.values.forEach { data ->
            if (!entityRefs.containsKey(data.id)) {
                logger.info("Spawning missing trader NPC: ${data.id} '${data.name}'")
                spawnNpcEntity(world, data)
            }
        }
    }

    /**
     * Restores trader NPCs that lost their marker component after persistence.
     * Custom components don't persist with the game's entity save, so we need to
     * find NPCs by their role and position, delete the orphaned entity, and spawn fresh.
     */
    fun restoreMarkersForPersistedEntities(world: World) {
        val store = world.entityStore.store
        val npcType = NPCEntity.getComponentType() ?: return
        val markerType = TraderNpcMarker.getComponentType()
        val transformType = com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType()

        // Collect entities to replace (can't modify while iterating)
        val toReplace = mutableListOf<Pair<Ref<EntityStore>, TraderNpcData>>()

        store.forEachChunk(
            npcType,
            java.util.function.BiConsumer { chunk: com.hypixel.hytale.component.ArchetypeChunk<EntityStore>, _: com.hypixel.hytale.component.CommandBuffer<EntityStore> ->
                for (i in 0 until chunk.size()) {
                    val npcEntity = chunk.getComponent(i, npcType) ?: continue

                    // Only process our trader NPCs
                    if (npcEntity.roleName != NPC_ROLE) continue

                    val ref = chunk.getReferenceTo(i)

                    // Check if it already has a marker (properly spawned)
                    val existingMarker = store.getComponent(ref, markerType)
                    if (existingMarker != null) {
                        // Has marker - just recover the ref
                        if (!entityRefs.containsKey(existingMarker.npcId)) {
                            logger.info("Recovering ref for properly marked NPC: ${existingMarker.npcId}")
                            entityRefs[existingMarker.npcId] = ref
                            spawnedThisSession.add(existingMarker.npcId)
                        }
                        continue
                    }

                    // No marker - try to match by position
                    val transform = store.getComponent(ref, transformType) ?: continue
                    val pos = transform.position

                    val npcData = findNpcByPosition(pos.x, pos.y, pos.z, 1.0)
                    if (npcData != null && !entityRefs.containsKey(npcData.id)) {
                        toReplace.add(Pair(ref, npcData))
                    }
                }
            }
        )

        // Replace orphaned entities with fresh ones
        toReplace.forEach { (oldRef, npcData) ->
            logger.info("Replacing orphaned persisted NPC with fresh spawn: ${npcData.id} '${npcData.name}'")
            // Remove the old entity
            store.removeEntity(oldRef, RemoveReason.REMOVE)
            // Spawn a fresh one with proper marker
            spawnNpcEntity(world, npcData)
        }
    }

    /**
     * Despawns all NPC entities.
     */
    fun despawnAllNpcs(world: World) {
        val store = world.entityStore.store
        entityRefs.values.forEach { ref ->
            if (ref.isValid) {
                store.removeEntity(ref, RemoveReason.REMOVE)
            }
        }
        entityRefs.clear()
        spawnedThisSession.clear()
    }

    /**
     * Removes all existing entities with TraderNpcMarker OR with our NPC role from the world.
     * This catches both properly marked NPCs and orphaned ones from earlier testing.
     * Returns the number of entities removed.
     */
    fun cleanupExistingEntities(world: World): Int {
        val store = world.entityStore.store
        val markerType = TraderNpcMarker.getComponentType()
        val npcType = NPCEntity.getComponentType()
        val refsToRemove = mutableSetOf<Ref<EntityStore>>()

        // First pass: find entities with our marker
        store.forEachChunk(
            markerType,
            java.util.function.BiConsumer { chunk: com.hypixel.hytale.component.ArchetypeChunk<EntityStore>, _: com.hypixel.hytale.component.CommandBuffer<EntityStore> ->
                for (i in 0 until chunk.size()) {
                    val marker = chunk.getComponent(i, markerType)
                    if (marker != null) {
                        refsToRemove.add(chunk.getReferenceTo(i))
                    }
                }
            }
        )

        // Second pass: find NPCs with our role name (catches orphaned entities without marker)
        if (npcType != null) {
            store.forEachChunk(
                npcType,
                java.util.function.BiConsumer { chunk: com.hypixel.hytale.component.ArchetypeChunk<EntityStore>, _: com.hypixel.hytale.component.CommandBuffer<EntityStore> ->
                    for (i in 0 until chunk.size()) {
                        val npcEntity = chunk.getComponent(i, npcType)
                        if (npcEntity != null && npcEntity.roleName == NPC_ROLE) {
                            refsToRemove.add(chunk.getReferenceTo(i))
                        }
                    }
                }
            )
        }

        refsToRemove.forEach { ref ->
            logger.info("Removing trader NPC entity: $ref")
            store.removeEntity(ref, RemoveReason.REMOVE)
        }

        if (refsToRemove.isNotEmpty()) {
            logger.info("Cleaned up ${refsToRemove.size} trader NPC entities")
        }

        return refsToRemove.size
    }

    /**
     * Loads NPCs from the JSON file.
     */
    private fun loadNpcs() {
        if (!npcsFile.exists()) {
            return
        }
        try {
            val type = object : TypeToken<Map<String, TraderNpcData>>() {}.type
            val loaded: Map<String, TraderNpcData>? = gson.fromJson(npcsFile.readText(), type)
            loaded?.let {
                npcs.clear()
                npcs.putAll(it)
            }
        } catch (e: Exception) {
            logger.warning("Failed to load trader NPCs: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Saves NPCs to the JSON file.
     */
    private fun saveNpcs() {
        try {
            npcsFile.parentFile?.mkdirs()
            npcsFile.writeText(gson.toJson(npcs))
        } catch (e: Exception) {
            logger.warning("Failed to save trader NPCs: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Gets all NPCs.
     */
    fun getAllNpcs(): List<TraderNpcData> = npcs.values.toList()

    /**
     * Gets an NPC by ID.
     */
    fun getNpc(id: String): TraderNpcData? = npcs[id]

    /**
     * Finds an NPC by position within a tolerance.
     * Used to restore marker for persisted entities after restart.
     */
    fun findNpcByPosition(x: Double, y: Double, z: Double, tolerance: Double): TraderNpcData? {
        return npcs.values.find { npc ->
            kotlin.math.abs(npc.x - x) <= tolerance &&
            kotlin.math.abs(npc.y - y) <= tolerance &&
            kotlin.math.abs(npc.z - z) <= tolerance
        }
    }

    /**
     * Gets an NPC by its entity ref.
     */
    fun getNpcByEntityRef(ref: Ref<EntityStore>, store: Store<EntityStore>): TraderNpcData? {
        val marker = store.getComponent(ref, TraderNpcMarker.getComponentType()) ?: return null
        return npcs[marker.npcId]
    }

    /**
     * Checks if an NPC entity is already spawned.
     */
    fun isSpawned(id: String): Boolean = entityRefs.containsKey(id) && entityRefs[id]?.isValid == true

    /**
     * Recovers an entity ref for an NPC that was persisted by the game.
     */
    fun recoverEntityRef(id: String, ref: Ref<EntityStore>) {
        if (!entityRefs.containsKey(id)) {
            logger.info("Recovered persisted trader NPC entity: $id")
            entityRefs[id] = ref
            spawnedThisSession.add(id)
        }
    }

    /**
     * Finds an existing entity with the given NPC ID marker in the world.
     * Returns the ref if found, null otherwise.
     */
    private fun findExistingEntity(id: String, store: Store<EntityStore>): Ref<EntityStore>? {
        val markerType = TraderNpcMarker.getComponentType()
        var foundRef: Ref<EntityStore>? = null

        store.forEachChunk(
            markerType,
            java.util.function.BiConsumer { chunk: com.hypixel.hytale.component.ArchetypeChunk<EntityStore>, _: com.hypixel.hytale.component.CommandBuffer<EntityStore> ->
                if (foundRef != null) return@BiConsumer  // Already found
                for (i in 0 until chunk.size()) {
                    val marker = chunk.getComponent(i, markerType)
                    if (marker != null && marker.npcId == id) {
                        foundRef = chunk.getReferenceTo(i)
                        return@BiConsumer
                    }
                }
            }
        )

        return foundRef
    }

    /**
     * Spawns a single NPC by ID (only used for respawning missing NPCs).
     */
    fun spawnNpc(id: String, world: World) {
        val data = npcs[id] ?: return
        if (isSpawned(id)) return
        spawnNpcEntity(world, data)
    }

    /**
     * Creates a new trader NPC at the given position.
     */
    fun createNpc(name: String, position: Vector3d, world: World): TraderNpcData {
        val id = UUID.randomUUID().toString().substring(0, 8)
        val data = TraderNpcData(id, name, position.x, position.y, position.z)
        npcs[id] = data
        saveNpcs()
        spawnNpcEntity(world, data)
        return data
    }

    /**
     * Deletes an NPC by ID.
     */
    fun deleteNpc(id: String, world: World): Boolean {
        val removed = npcs.remove(id)
        if (removed != null) {
            saveNpcs()
            despawnNpcEntity(id, world)
            return true
        }
        return false
    }

    /**
     * Clears all NPCs - removes all entities with TraderNpcMarker or our role from the world,
     * clears all NPC data, and saves the empty state.
     * Returns the number of entities removed.
     */
    fun clearAllNpcs(world: World): Int {
        // Remove all entities with TraderNpcMarker or our role from the world
        val count = cleanupExistingEntities(world)

        // Clear all tracking maps
        entityRefs.clear()
        spawnedThisSession.clear()
        npcs.clear()

        // Save empty state
        saveNpcs()

        logger.info("Cleared all trader NPCs (removed $count entities)")
        return count
    }

    /**
     * Adds a trade to an NPC.
     */
    fun addTrade(npcId: String, trade: TradeData): Boolean {
        val npc = npcs[npcId] ?: return false
        npc.trades.add(trade)
        saveNpcs()
        return true
    }

    /**
     * Removes a trade from an NPC by index.
     */
    fun removeTrade(npcId: String, index: Int): Boolean {
        val npc = npcs[npcId] ?: return false
        if (index < 0 || index >= npc.trades.size) return false
        npc.trades.removeAt(index)
        saveNpcs()
        return true
    }

    /**
     * Updates a trade at the specified index.
     */
    fun updateTrade(npcId: String, index: Int, trade: TradeData): Boolean {
        val npc = npcs[npcId] ?: return false
        if (index < 0 || index >= npc.trades.size) return false
        npc.trades[index] = trade
        saveNpcs()
        return true
    }

    /**
     * Spawns an NPC entity in the world using NPCPlugin for proper interaction support.
     */
    private fun spawnNpcEntity(world: World, data: TraderNpcData) {
        logger.info("Spawning trader NPC entity: ${data.id} '${data.name}' at (${data.x}, ${data.y}, ${data.z})")
        val store = world.entityStore.store

        val position = Vector3d(data.x, data.y, data.z)
        val rotation = Vector3f()

        // Use NPCPlugin to spawn a proper NPC with full interaction support
        val npcPair = NPCPlugin.get().spawnNPC(store, NPC_ROLE, null, position, rotation)
        if (npcPair == null) {
            logger.warning("Trader NPC ${data.id} entity creation FAILED - NPCPlugin.spawnNPC returned null")
            return
        }

        val ref = npcPair.first()

        // Add our marker component to identify this as our trader NPC
        store.putComponent(ref, TraderNpcMarker.getComponentType(), TraderNpcMarker(data.id))

        // Set custom nameplate
        store.putComponent(ref, Nameplate.getComponentType(), Nameplate(data.name))

        entityRefs[data.id] = ref
        logger.info("Trader NPC ${data.id} entity created successfully with ref: $ref")
    }

    /**
     * Despawns an NPC entity from the world.
     */
    private fun despawnNpcEntity(id: String, world: World) {
        val ref = entityRefs.remove(id) ?: return
        if (ref.isValid) {
            world.entityStore.store.removeEntity(ref, RemoveReason.REMOVE)
        }
    }

    /**
     * Gets the entity ref for a specific NPC.
     */
    fun getEntityRef(id: String): Ref<EntityStore>? = entityRefs[id]

    /**
     * Checks if an NPC exists.
     */
    fun npcExists(id: String): Boolean = npcs.containsKey(id)
}
