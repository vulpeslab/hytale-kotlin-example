package com.vulpeslab.exampleplugin.services

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

/**
 * Component that marks an entity as a trader NPC and stores its ID.
 * Used for identifying trader NPC entities and linking them to their data.
 */
class TraderNpcMarker(var npcId: String = "") : Component<EntityStore> {

    companion object {
        private var componentType: ComponentType<EntityStore, TraderNpcMarker>? = null

        fun getComponentType(): ComponentType<EntityStore, TraderNpcMarker> {
            if (componentType == null) {
                componentType = EntityStore.REGISTRY.registerComponent(
                    TraderNpcMarker::class.java
                ) { TraderNpcMarker() }
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
        return TraderNpcMarker(npcId)
    }
}
