package com.vulpeslab.exampleplugin.services

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

/**
 * Component that marks an entity as a hologram and stores its ID.
 * Used for identifying hologram entities and enabling billboard rotation.
 */
class HologramMarker(var hologramId: String = "") : Component<EntityStore> {

    companion object {
        private var componentType: ComponentType<EntityStore, HologramMarker>? = null

        fun getComponentType(): ComponentType<EntityStore, HologramMarker> {
            if (componentType == null) {
                componentType = EntityStore.REGISTRY.registerComponent(
                    HologramMarker::class.java
                ) { HologramMarker() }
            }
            return componentType!!
        }

        /**
         * Unregisters the component type during plugin shutdown.
         */
        fun unregister() {
            componentType = null
        }
    }

    override fun clone(): Component<EntityStore> {
        return HologramMarker(hologramId)
    }
}
