package com.vulpeslab.exampleplugin.listeners

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.vulpeslab.exampleplugin.services.NpcEditModeManager
import com.vulpeslab.exampleplugin.services.TraderNpcManager
import com.vulpeslab.exampleplugin.services.TraderNpcMarker
import com.vulpeslab.exampleplugin.ui.NpcTradeConfigUiPage
import com.vulpeslab.exampleplugin.ui.NpcTradeExecuteUiPage
import java.util.function.BiConsumer
import java.util.logging.Logger

/**
 * ECS System that monitors trader NPCs for pending player interactions.
 * When a player presses F on a trader NPC, the NPC system adds them to interactedPlayers.
 * This system polls those pending interactions and opens the appropriate trade UI.
 *
 * Note: TraderNpcMarker restoration for persisted entities is handled by TraderNpcManager
 * during chunk load events, not in this system.
 */
class TraderNpcInteractionSystem : EntityTickingSystem<EntityStore>() {

    companion object {
        private val logger: Logger = Logger.getLogger("TraderNpcInteractionSystem")
        private var tickCount = 0

        private val NPC_QUERY: Query<EntityStore> by lazy {
            Query.and(
                TraderNpcMarker.getComponentType(),
                NPCEntity.getComponentType()
            )
        }

        private val PLAYER_QUERY: Query<EntityStore> by lazy {
            Query.and(
                Player.getComponentType(),
                PlayerRef.getComponentType()
            )
        }
    }

    override fun getGroup(): SystemGroup<EntityStore>? {
        return EntityTrackerSystems.QUEUE_UPDATE_GROUP
    }

    override fun getQuery(): Query<EntityStore> {
        return NPC_QUERY
    }

    override fun tick(
        dt: Float,
        index: Int,
        archetypeChunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val markerType = TraderNpcMarker.getComponentType()
        val npcType = NPCEntity.getComponentType() ?: return

        val marker = archetypeChunk.getComponent(index, markerType) ?: return
        val npcEntity: NPCEntity = archetypeChunk.getComponent(index, npcType) ?: return
        val npcRef = archetypeChunk.getReferenceTo(index)

        // Debug logging (only every 100 ticks to avoid spam)
        tickCount++
        if (tickCount % 100 == 0) {
            logger.info("Tick #$tickCount - Processing NPC: ${marker.npcId}")
        }

        // Recover ref if we don't have it (entity persisted from previous session)
        if (!TraderNpcManager.isSpawned(marker.npcId)) {
            logger.info("Recovering ref for NPC: ${marker.npcId}")
            TraderNpcManager.recoverEntityRef(marker.npcId, npcRef)
        }

        val stateSupport = npcEntity.getRole()?.stateSupport ?: return
        val npcData = TraderNpcManager.getNpc(marker.npcId) ?: return

        // Check all players to see if they've interacted with this NPC
        val playerType = Player.getComponentType()
        val playerRefType = PlayerRef.getComponentType()

        store.forEachChunk(
            PLAYER_QUERY,
            BiConsumer { playerChunk: ArchetypeChunk<EntityStore>, _: CommandBuffer<EntityStore> ->
                for (i in 0 until playerChunk.size()) {
                    val playerRef: Ref<EntityStore> = playerChunk.getReferenceTo(i)
                    val player: Player = playerChunk.getComponent(i, playerType) ?: continue

                    // Try to consume an interaction for this player
                    if (stateSupport.consumeInteraction(playerRef)) {
                        logger.info("Consumed interaction for player on NPC: ${npcData.id}")
                        val playerRefComponent: PlayerRef = playerChunk.getComponent(i, playerRefType) ?: continue
                        val playerUuid = player.uuid ?: continue

                        // Open appropriate UI based on edit mode
                        if (NpcEditModeManager.isInEditMode(playerUuid)) {
                            logger.info("Opening config UI for player in edit mode")
                            player.pageManager?.openCustomPage(playerRef, store,
                                NpcTradeConfigUiPage(playerRefComponent, npcData.id))
                        } else {
                            logger.info("Opening trade UI for player")
                            player.pageManager?.openCustomPage(playerRef, store,
                                NpcTradeExecuteUiPage(playerRefComponent, npcData.id))
                        }
                    }
                }
            }
        )
    }
}
