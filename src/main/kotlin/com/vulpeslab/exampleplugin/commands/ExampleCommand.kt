package com.vulpeslab.exampleplugin.commands

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.UpdateChecker
import java.util.concurrent.CompletableFuture

/**
 * Main example plugin command collection.
 * Usage: /example <subcommand>
 * Subcommands: info
 */
class ExampleCommand : AbstractCommandCollection("example", "exampleplugin.commands.example.description") {

    init {
        addSubCommand(InfoCommand())
        addSubCommand(DurabilityCommand())
    }

    /**
     * Shows plugin information with a clickable link.
     * Usage: /example info
     * No permission required for basic info, but update check requires updatenotify permission.
     */
    private class InfoCommand : AbstractAsyncCommand("info", "exampleplugin.commands.example.info.description") {
        // Allow all players to use this command (no permission required)
        override fun canGeneratePermission(): Boolean = false

        override fun executeAsync(context: CommandContext): CompletableFuture<Void> {
            // Plugin name and version
            context.sendMessage(
                Message.empty()
                    .insert(
                        Message.translation("exampleplugin.commands.example.info.name")
                            .param("name", UpdateChecker.PLUGIN_NAME)
                            .color("#f1c40f")
                            .bold(true)
                    )
                    .insert(
                        Message.translation("exampleplugin.commands.example.info.version")
                            .param("version", UpdateChecker.PLUGIN_VERSION)
                            .color("#95a5a6")
                    )
            )

            // Author
            context.sendMessage(
                Message.translation("exampleplugin.commands.example.info.author")
                    .param("author", Message.raw(UpdateChecker.PLUGIN_AUTHOR).color("#3498db").bold(true))
                    .color("#bdc3c7")
            )

            // GitHub repository
            context.sendMessage(
                Message.empty()
                    .insert(Message.translation("exampleplugin.commands.example.info.github").color("#bdc3c7"))
                    .insert(
                        Message.raw(" " + UpdateChecker.GITHUB_URL)
                            .link(UpdateChecker.GITHUB_URL)
                            .color("#3498db")
                    )
            )

            // Only check for updates if sender has permission
            if (!context.sender().hasPermission(UpdateChecker.UPDATE_NOTIFY_PERMISSION)) {
                return CompletableFuture.completedFuture(null)
            }

            // Check for updates asynchronously
            return UpdateChecker.checkForUpdateAsync().thenAccept { updateInfo ->
                context.sendMessage(Message.raw(""))  // Empty line

                if (updateInfo.error != null) {
                    context.sendMessage(
                        Message.translation("exampleplugin.commands.example.info.update.failed")
                            .color("#e74c3c")
                    )
                } else if (updateInfo.isUpdateAvailable) {
                    context.sendMessage(
                        Message.empty()
                            .insert(
                                Message.translation("exampleplugin.commands.example.info.update.available")
                                    .color("#f39c12")
                            )
                            .insert(Message.raw(" v${updateInfo.latestVersion}").color("#2ecc71").bold(true))
                    )
                    context.sendMessage(
                        Message.empty()
                            .insert(
                                Message.translation("exampleplugin.commands.example.info.update.download")
                                    .color("#bdc3c7")
                            )
                            .insert(
                                Message.raw(" " + UpdateChecker.GITHUB_RELEASES_URL)
                                    .link(UpdateChecker.GITHUB_RELEASES_URL)
                                    .color("#3498db")
                            )
                    )
                } else {
                    context.sendMessage(
                        Message.translation("exampleplugin.commands.example.info.update.latest")
                            .color("#2ecc71")
                    )
                }
            }
        }
    }

    /**
     * Sets the durability of the held item.
     * Usage: /example durability <amount>
     */
    private class DurabilityCommand : AbstractPlayerCommand(
        "durability",
        "exampleplugin.commands.example.durability.description"
    ) {
        private val amountArg: RequiredArg<Double> = withRequiredArg(
            "amount",
            "exampleplugin.commands.example.durability.amount.desc",
            ArgTypes.DOUBLE
        )

        override fun execute(
            context: CommandContext,
            store: com.hypixel.hytale.component.Store<EntityStore>,
            ref: com.hypixel.hytale.component.Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)

            val inventory = player.inventory
            val inHand = inventory.activeHotbarItem

            if (inHand == null || ItemStack.isEmpty(inHand)) {
                playerRef.sendMessage(Message.translation("exampleplugin.commands.example.durability.notHoldingItem"))
                return
            }

            if (inHand.isUnbreakable() || inHand.maxDurability <= 0.0) {
                playerRef.sendMessage(Message.translation("exampleplugin.commands.example.durability.notDurable"))
                return
            }

            val amount = amountArg.get(context)
            val clamped = kotlin.math.max(0.0, kotlin.math.min(amount, inHand.maxDurability))
            val updated = inHand.withDurability(clamped)
            inventory.hotbar.replaceItemStackInSlot(inventory.activeHotbarSlot.toInt().toShort(), inHand, updated)

            playerRef.sendMessage(
                Message.translation("exampleplugin.commands.example.durability.set")
                    .param("value", clamped)
            )
        }
    }
}
