package com.vulpeslab.exampleplugin.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a single item in a kit.
 */
data class KitItem(
    val itemId: String,
    val quantity: Int = 1
)

/**
 * Represents a kit containing items.
 */
data class Kit(
    val name: String,
    val items: MutableList<KitItem> = mutableListOf()
)

/**
 * Manages kit data storage and retrieval.
 * Kits are stored in a JSON file in the plugin data directory.
 */
object KitManager {
    private lateinit var kitsFile: File
    private val kits = ConcurrentHashMap<String, Kit>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Initializes the KitManager with the plugin's data directory.
     */
    fun initialize(dataDirectory: Path) {
        kitsFile = dataDirectory.resolve("kits.json").toFile()
        loadKits()
    }

    /**
     * Loads kits from the JSON file.
     */
    private fun loadKits() {
        if (!kitsFile.exists()) {
            return
        }
        try {
            val type = object : TypeToken<Map<String, Kit>>() {}.type
            val loadedKits: Map<String, Kit>? = gson.fromJson(kitsFile.readText(), type)
            loadedKits?.let {
                kits.clear()
                kits.putAll(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves kits to the JSON file.
     */
    private fun saveKits() {
        try {
            kitsFile.parentFile?.mkdirs()
            kitsFile.writeText(gson.toJson(kits))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gets all kits.
     */
    fun getAllKits(): List<Kit> = kits.values.toList()

    /**
     * Gets all kits asynchronously.
     */
    fun getAllKitsAsync(): CompletableFuture<List<Kit>> {
        return CompletableFuture.supplyAsync { getAllKits() }
    }

    /**
     * Gets a kit by name.
     */
    fun getKit(name: String): Kit? = kits[name.lowercase()]

    /**
     * Gets a kit by name asynchronously.
     */
    fun getKitAsync(name: String): CompletableFuture<Kit?> {
        return CompletableFuture.supplyAsync { getKit(name) }
    }

    /**
     * Creates or updates a kit.
     */
    fun saveKit(kit: Kit): Boolean {
        kits[kit.name.lowercase()] = kit
        saveKits()
        return true
    }

    /**
     * Creates or updates a kit asynchronously.
     */
    fun saveKitAsync(kit: Kit): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { saveKit(kit) }
    }

    /**
     * Creates a new kit with the given name.
     */
    fun createKit(name: String): Kit? {
        val key = name.lowercase()
        if (kits.containsKey(key)) {
            return null // Kit already exists
        }
        val kit = Kit(name)
        kits[key] = kit
        saveKits()
        return kit
    }

    /**
     * Creates a new kit asynchronously.
     */
    fun createKitAsync(name: String): CompletableFuture<Kit?> {
        return CompletableFuture.supplyAsync { createKit(name) }
    }

    /**
     * Deletes a kit by name.
     */
    fun deleteKit(name: String): Boolean {
        val removed = kits.remove(name.lowercase()) != null
        if (removed) {
            saveKits()
        }
        return removed
    }

    /**
     * Deletes a kit asynchronously.
     */
    fun deleteKitAsync(name: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { deleteKit(name) }
    }

    /**
     * Adds an item to a kit.
     */
    fun addItemToKit(kitName: String, itemId: String, quantity: Int = 1): Boolean {
        val kit = kits[kitName.lowercase()] ?: return false
        kit.items.add(KitItem(itemId, quantity))
        saveKits()
        return true
    }

    /**
     * Adds an item to a kit asynchronously.
     */
    fun addItemToKitAsync(kitName: String, itemId: String, quantity: Int = 1): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { addItemToKit(kitName, itemId, quantity) }
    }

    /**
     * Removes an item from a kit by index.
     */
    fun removeItemFromKit(kitName: String, index: Int): Boolean {
        val kit = kits[kitName.lowercase()] ?: return false
        if (index < 0 || index >= kit.items.size) {
            return false
        }
        kit.items.removeAt(index)
        saveKits()
        return true
    }

    /**
     * Removes an item from a kit by index asynchronously.
     */
    fun removeItemFromKitAsync(kitName: String, index: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { removeItemFromKit(kitName, index) }
    }

    /**
     * Removes an item from a kit by item ID.
     */
    fun removeItemFromKitByItemId(kitName: String, itemId: String): Boolean {
        val kit = kits[kitName.lowercase()] ?: return false
        val removed = kit.items.removeIf { it.itemId == itemId }
        if (removed) {
            saveKits()
        }
        return removed
    }

    /**
     * Removes an item from a kit by item ID asynchronously.
     */
    fun removeItemFromKitByItemIdAsync(kitName: String, itemId: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { removeItemFromKitByItemId(kitName, itemId) }
    }

    /**
     * Sets the quantity of an item in a kit, or adds it if not present.
     */
    fun setItemQuantity(kitName: String, itemId: String, quantity: Int): Boolean {
        val kit = kits[kitName.lowercase()] ?: return false
        val existingItem = kit.items.find { it.itemId == itemId }
        if (existingItem != null) {
            kit.items.remove(existingItem)
        }
        if (quantity > 0) {
            kit.items.add(KitItem(itemId, quantity))
        }
        saveKits()
        return true
    }

    /**
     * Sets the quantity of an item asynchronously.
     */
    fun setItemQuantityAsync(kitName: String, itemId: String, quantity: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { setItemQuantity(kitName, itemId, quantity) }
    }

    /**
     * Checks if a kit exists.
     */
    fun kitExists(name: String): Boolean = kits.containsKey(name.lowercase())

    /**
     * Renames a kit.
     */
    fun renameKit(oldName: String, newName: String): Boolean {
        val oldKey = oldName.lowercase()
        val newKey = newName.lowercase()

        if (!kits.containsKey(oldKey)) return false
        if (kits.containsKey(newKey)) return false

        val kit = kits.remove(oldKey) ?: return false
        val renamedKit = Kit(newName, kit.items)
        kits[newKey] = renamedKit
        saveKits()
        return true
    }

    /**
     * Renames a kit asynchronously.
     */
    fun renameKitAsync(oldName: String, newName: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { renameKit(oldName, newName) }
    }

    /**
     * Moves an item in a kit from one position to another.
     */
    fun moveItemInKit(kitName: String, fromIndex: Int, toIndex: Int): Boolean {
        val kit = kits[kitName.lowercase()] ?: return false
        if (fromIndex < 0 || fromIndex >= kit.items.size) return false
        if (toIndex < 0 || toIndex >= kit.items.size) return false
        if (fromIndex == toIndex) return true

        val item = kit.items.removeAt(fromIndex)
        kit.items.add(toIndex, item)
        saveKits()
        return true
    }

    /**
     * Moves an item in a kit asynchronously.
     */
    fun moveItemInKitAsync(kitName: String, fromIndex: Int, toIndex: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { moveItemInKit(kitName, fromIndex, toIndex) }
    }
}
