package com.vulpeslab.exampleplugin.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.math.shape.Box
import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox
import com.hypixel.hytale.server.core.modules.entity.component.Intangible
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import java.io.File
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Represents a hologram's persistent data.
 */
data class HologramData(
    val id: String,
    val text: String,
    val x: Double,
    val y: Double,
    val z: Double
)

/**
 * Manages hologram creation, deletion, and persistence.
 * Holograms are floating text entities that rotate to face players (billboard).
 */
object HologramManager {
    private const val HOLOGRAM_MODEL_ID = "Warp"
    private const val HOLOGRAM_MODEL_SCALE = 0.001f  // Very small scale to make model nearly invisible
    private val logger: Logger = Logger.getLogger("HologramManager")
    private lateinit var hologramsFile: File
    private val holograms = ConcurrentHashMap<String, HologramData>()
    private val entityRefs = ConcurrentHashMap<String, Ref<EntityStore>>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private var hologramModel: Model? = null

    /**
     * Initializes the HologramManager with the plugin's data directory.
     */
    fun initialize(dataDirectory: Path) {
        hologramsFile = dataDirectory.resolve("holograms.json").toFile()
        loadHolograms()
    }

    /**
     * Gets or loads a tiny-scale model for holograms (makes the model nearly invisible).
     * Uses a large bounding box override to ensure long render distance.
     */
    private fun getOrLoadModel(): Model? {
        if (hologramModel == null) {
            val modelAsset = ModelAsset.getAssetMap().getAsset(HOLOGRAM_MODEL_ID)
            if (modelAsset != null) {
                // Create a very small scale model so it's nearly invisible
                // Override bounding box to be very large for maximum render distance
                val largeBoundingBox = Box(
                    Vector3d(-50.0, -50.0, -50.0),
                    Vector3d(50.0, 50.0, 50.0)
                )
                hologramModel = Model.createScaledModel(modelAsset, HOLOGRAM_MODEL_SCALE, null, largeBoundingBox)
                logger.info("Hologram model loaded with scale $HOLOGRAM_MODEL_SCALE and large bounding box")
            } else {
                logger.warning("Failed to load hologram model: $HOLOGRAM_MODEL_ID not found")
            }
        }
        return hologramModel
    }

    /**
     * Spawns all saved holograms in the given world.
     */
    fun spawnAllHolograms(world: World) {
        holograms.values.forEach { data ->
            spawnHologramEntity(world, data)
        }
    }

    /**
     * Despawns all hologram entities.
     */
    fun despawnAllHolograms(world: World) {
        val store = world.entityStore.store
        entityRefs.values.forEach { ref ->
            if (ref.isValid) {
                store.removeEntity(ref, RemoveReason.REMOVE)
            }
        }
        entityRefs.clear()
    }

    /**
     * Loads holograms from the JSON file.
     */
    private fun loadHolograms() {
        if (!hologramsFile.exists()) {
            return
        }
        try {
            val type = object : TypeToken<Map<String, HologramData>>() {}.type
            val loaded: Map<String, HologramData>? = gson.fromJson(hologramsFile.readText(), type)
            loaded?.let {
                holograms.clear()
                holograms.putAll(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves holograms to the JSON file.
     */
    private fun saveHolograms() {
        try {
            hologramsFile.parentFile?.mkdirs()
            hologramsFile.writeText(gson.toJson(holograms))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gets all holograms.
     */
    fun getAllHolograms(): List<HologramData> = holograms.values.toList()

    /**
     * Gets all holograms asynchronously.
     */
    fun getAllHologramsAsync(): CompletableFuture<List<HologramData>> {
        return CompletableFuture.supplyAsync { getAllHolograms() }
    }

    /**
     * Gets a hologram by ID.
     */
    fun getHologram(id: String): HologramData? = holograms[id]

    /**
     * Checks if a hologram entity is already spawned.
     */
    fun isSpawned(id: String): Boolean = entityRefs.containsKey(id) && entityRefs[id]?.isValid == true

    /**
     * Spawns a single hologram by ID.
     */
    fun spawnHologram(id: String, world: World) {
        val data = holograms[id] ?: return
        if (isSpawned(id)) return
        spawnHologramEntity(world, data)
    }

    /**
     * Gets a hologram by ID asynchronously.
     */
    fun getHologramAsync(id: String): CompletableFuture<HologramData?> {
        return CompletableFuture.supplyAsync { getHologram(id) }
    }

    /**
     * Creates a new hologram at the given position.
     */
    fun createHologram(text: String, position: Vector3d, world: World): HologramData {
        val id = UUID.randomUUID().toString().substring(0, 8)
        val data = HologramData(id, text, position.x, position.y, position.z)
        holograms[id] = data
        saveHolograms()
        spawnHologramEntity(world, data)
        return data
    }

    /**
     * Creates a new hologram asynchronously.
     */
    fun createHologramAsync(text: String, position: Vector3d, world: World): CompletableFuture<HologramData> {
        return CompletableFuture.supplyAsync { createHologram(text, position, world) }
    }

    /**
     * Deletes a hologram by ID.
     */
    fun deleteHologram(id: String, world: World): Boolean {
        val removed = holograms.remove(id)
        if (removed != null) {
            saveHolograms()
            despawnHologramEntity(id, world)
            return true
        }
        return false
    }

    /**
     * Deletes a hologram asynchronously.
     */
    fun deleteHologramAsync(id: String, world: World): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { deleteHologram(id, world) }
    }

    /**
     * Updates a hologram's text.
     */
    fun updateHologramText(id: String, newText: String, world: World): Boolean {
        val existing = holograms[id] ?: return false
        val updated = existing.copy(text = newText)
        holograms[id] = updated
        saveHolograms()

        val store = world.entityStore.store

        // Try fast path: use cached ref if still valid
        val cachedRef = entityRefs[id]
        if (cachedRef != null && cachedRef.isValid) {
            logger.info("Hologram $id update: using fast path (cached ref valid)")
            entityRefs.remove(id)
            store.removeEntity(cachedRef, RemoveReason.REMOVE)
        } else {
            logger.info("Hologram $id update: using fallback (cached ref invalid or missing)")
            // Fallback: search by HologramMarker (handles invalid/stale refs)
            removeHologramEntityByMarker(id, store)
        }

        // Spawn new entity with updated text
        spawnHologramEntity(world, updated)
        return true
    }

    /**
     * Finds and removes a hologram entity by its HologramMarker ID.
     * Fallback for when cached refs become invalid.
     */
    private fun removeHologramEntityByMarker(hologramId: String, store: Store<EntityStore>) {
        entityRefs.remove(hologramId)

        val markerType = HologramMarker.getComponentType()
        val refsToRemove = mutableListOf<Ref<EntityStore>>()

        store.forEachChunk(
            markerType,
            java.util.function.BiConsumer { chunk: com.hypixel.hytale.component.ArchetypeChunk<EntityStore>, _: com.hypixel.hytale.component.CommandBuffer<EntityStore> ->
                for (i in 0 until chunk.size()) {
                    val marker = chunk.getComponent(i, markerType)
                    if (marker != null && marker.hologramId == hologramId) {
                        refsToRemove.add(chunk.getReferenceTo(i))
                    }
                }
            }
        )

        refsToRemove.forEach { ref ->
            store.removeEntity(ref, RemoveReason.REMOVE)
        }
    }

    /**
     * Spawns a hologram entity in the world.
     */
    private fun spawnHologramEntity(world: World, data: HologramData) {
        logger.info("Spawning hologram entity: ${data.id} at (${data.x}, ${data.y}, ${data.z})")
        val store = world.entityStore.store
        val holder: Holder<EntityStore> = EntityStore.REGISTRY.newHolder()

        // Add components
        val position = Vector3d(data.x, data.y, data.z)
        val rotation = Vector3f()

        holder.addComponent(TransformComponent.getComponentType(), TransformComponent(position, rotation))
        holder.addComponent(Nameplate.getComponentType(), Nameplate(data.text))
        holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID())
        holder.addComponent(HologramMarker.getComponentType(), HologramMarker(data.id))

        // NetworkId is required for the entity to be visible to clients
        val networkId = store.externalData.takeNextNetworkId()
        holder.addComponent(NetworkId.getComponentType(), NetworkId(networkId))
        logger.info("Hologram ${data.id} assigned NetworkId: $networkId")

        // Add a tiny-scale model so the entity is tracked (required for nameplate to render)
        // Model has a large bounding box override for long render distance
        val model = getOrLoadModel()
        if (model != null) {
            holder.addComponent(ModelComponent.getComponentType(), ModelComponent(model))
            // Add BoundingBox component matching the model's bounding box
            model.boundingBox?.let { box ->
                holder.addComponent(BoundingBox.getComponentType(), BoundingBox(box))
            }
        }

        // Intangible makes the entity non-collidable
        holder.ensureComponent(Intangible.getComponentType())

        // Add to world
        val ref = store.addEntity(holder, AddReason.SPAWN)
        if (ref != null) {
            entityRefs[data.id] = ref
            logger.info("Hologram ${data.id} entity created successfully with ref: $ref")
        } else {
            logger.warning("Hologram ${data.id} entity creation FAILED - addEntity returned null")
        }
    }

    /**
     * Despawns a hologram entity from the world.
     */
    private fun despawnHologramEntity(id: String, world: World) {
        val ref = entityRefs.remove(id) ?: return
        if (ref.isValid) {
            world.entityStore.store.removeEntity(ref, RemoveReason.REMOVE)
        }
    }

    /**
     * Gets all active hologram entity refs.
     */
    fun getEntityRefs(): Collection<Ref<EntityStore>> = entityRefs.values

    /**
     * Gets the entity ref for a specific hologram.
     */
    fun getEntityRef(id: String): Ref<EntityStore>? = entityRefs[id]

    /**
     * Checks if a hologram exists.
     */
    fun hologramExists(id: String): Boolean = holograms.containsKey(id)
}
