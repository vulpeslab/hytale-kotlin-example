package com.vulpeslab.exampleplugin.commands

import com.vulpeslab.exampleplugin.ui.BasicUiPage
import com.vulpeslab.exampleplugin.ui.ComponentsUiPage
import com.vulpeslab.exampleplugin.ui.LifetimeCanDismissOrCloseUiPage
import com.vulpeslab.exampleplugin.ui.LifetimeCanDismissUiPage
import com.vulpeslab.exampleplugin.ui.LifetimeCantCloseUiPage
import com.vulpeslab.exampleplugin.ui.LoginUiPage
import com.vulpeslab.exampleplugin.ui.RegisterUiPage
import com.vulpeslab.exampleplugin.ui.TranslatedUiPage
import com.vulpeslab.exampleplugin.ui.UsersListUiPage
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

/**
 * Command collection for UI examples.
 * Usage: /ui <subcommand>
 * Subcommands: basic, translated, register, login, users
 */
class UiCommand : AbstractCommandCollection("ui", "Show UI examples") {

    init {
        addSubCommand(BasicUiCommand())
        addSubCommand(TranslatedUiCommand())
        addSubCommand(RegisterUiCommand())
        addSubCommand(LoginUiCommand())
        addSubCommand(UsersUiCommand())
        addSubCommand(ComponentsUiCommand())
        addSubCommand(LifetimeCantCloseUiCommand())
        addSubCommand(LifetimeCanDismissUiCommand())
        addSubCommand(LifetimeCanDismissOrCloseUiCommand())
    }

    /**
     * Opens the basic UI with raw (non-translated) text.
     */
    private class BasicUiCommand : AbstractPlayerCommand("basic", "Show basic UI with raw text") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, BasicUiPage(playerRef))
        }
    }

    /**
     * Opens the translated UI with i18n support.
     */
    private class TranslatedUiCommand : AbstractPlayerCommand("translated", "Show translated UI with i18n") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, TranslatedUiPage(playerRef))
        }
    }

    /**
     * Opens the registration form UI with interactive inputs.
     */
    private class RegisterUiCommand : AbstractPlayerCommand("register", "Show registration form with inputs") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, RegisterUiPage(playerRef))
        }
    }

    /**
     * Opens the login form UI.
     */
    private class LoginUiCommand : AbstractPlayerCommand("login", "Show login form") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, LoginUiPage(playerRef))
        }
    }

    /**
     * Opens the user management UI.
     */
    private class UsersUiCommand : AbstractPlayerCommand("users", "Manage registered users") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, UsersListUiPage(playerRef))
        }
    }

    /**
     * Opens the UI components showcase.
     */
    private class ComponentsUiCommand : AbstractPlayerCommand("components", "Show all UI components") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, ComponentsUiPage(playerRef))
        }
    }

    /**
     * Opens a UI with CantClose lifetime - can only be closed via the close button.
     */
    private class LifetimeCantCloseUiCommand : AbstractPlayerCommand("lifetime-cantclose", "UI that can't be dismissed") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, LifetimeCantCloseUiPage(playerRef))
        }
    }

    /**
     * Opens a UI with CanDismiss lifetime - can be closed by pressing escape.
     */
    private class LifetimeCanDismissUiCommand : AbstractPlayerCommand("lifetime-candismiss", "UI closable with escape") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, LifetimeCanDismissUiPage(playerRef))
        }
    }

    /**
     * Opens a UI with CanDismissOrCloseThroughInteraction lifetime - can be closed
     * by pressing escape or clicking outside the UI.
     */
    private class LifetimeCanDismissOrCloseUiCommand : AbstractPlayerCommand("lifetime-candissmissorclose", "UI closable with escape or click outside") {
        override fun execute(
            context: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            requireNotNull(player)
            player.pageManager.openCustomPage(ref, store, LifetimeCanDismissOrCloseUiPage(playerRef))
        }
    }
}
