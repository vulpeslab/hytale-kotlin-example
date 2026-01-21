package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.EnumCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.protocol.packets.interface_.Page
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.DatabaseManager

/**
 * Interactive registration form UI page demonstrating text inputs and button events.
 */
class RegisterUiPage(playerRef: PlayerRef) :
    InteractiveCustomUIPage<RegisterUiPage.PageData>(
        playerRef,
        CustomPageLifetime.CanDismiss,
        PageData.CODEC
    ) {

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/ExamplePluginRegisterPage.ui")

        // Event data for send action
        val sendEventData = EventData()
            .append("Action", Action.Send.name)
            .append("@Username", "#UsernameInput.Value")
            .append("@Email", "#EmailInput.Value")
            .append("@Password", "#PasswordInput.Value")

        // Bind send button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SendButton",
            sendEventData
        )

        // Bind Enter key on all inputs
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#UsernameInput",
            sendEventData
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#EmailInput",
            sendEventData
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#PasswordInput",
            sendEventData
        )

        // Bind clear button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ClearButton",
            EventData().append("Action", Action.Clear.name)
        )
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType())

        when (data.action) {
            Action.Send -> {
                val username = data.username ?: ""
                val email = data.email ?: ""
                val password = data.password ?: ""

                // Get player UUID
                @Suppress("DEPRECATION")
                val playerUuid = player?.getUuid()
                if (playerUuid == null) {
                    showError(Message.translation("exampleplugin.ui.register.error.generic"))
                    return
                }

                // Check if this UUID already has an account (async)
                DatabaseManager.hasAccountForUuidAsync(playerUuid).thenAccept { hasAccount ->
                    if (hasAccount) {
                        showError(Message.translation("exampleplugin.ui.register.error.alreadyregistered"))
                        return@thenAccept
                    }

                    // Save to database with UUID (async)
                    DatabaseManager.saveRegistrationAsync(playerUuid, username, email, password).thenAccept { id ->
                        if (id > 0) {
                            // Close the page
                            player.pageManager.setPage(ref, store, Page.None)
                            // Send success message
                            playerRef.sendMessage(Message.translation("exampleplugin.ui.register.success"))
                        } else {
                            showError(Message.translation("exampleplugin.ui.register.error.generic"))
                        }
                    }
                }
            }
            Action.Clear -> {
                // Clear all input fields by sending an update
                val commandBuilder = UICommandBuilder()
                commandBuilder.set("#UsernameInput.Value", "")
                commandBuilder.set("#EmailInput.Value", "")
                commandBuilder.set("#PasswordInput.Value", "")
                commandBuilder.set("#ErrorLabel.Visible", false)
                sendUpdate(commandBuilder, null, false)
            }
        }
    }

    private fun showError(message: Message) {
        val commandBuilder = UICommandBuilder()
        commandBuilder.set("#ErrorLabel.TextSpans", message)
        commandBuilder.set("#ErrorLabel.Visible", true)
        sendUpdate(commandBuilder, null, false)
    }

    enum class Action {
        Send,
        Clear
    }

    class PageData {
        var action: Action = Action.Send
        var username: String? = null
        var email: String? = null
        var password: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .append(
                    KeyedCodec("@Username", Codec.STRING),
                    { obj, username -> obj.username = username },
                    { obj -> obj.username }
                ).add()
                .append(
                    KeyedCodec("@Email", Codec.STRING),
                    { obj, email -> obj.email = email },
                    { obj -> obj.email }
                ).add()
                .append(
                    KeyedCodec("@Password", Codec.STRING),
                    { obj, password -> obj.password = password },
                    { obj -> obj.password }
                ).add()
                .build()
        }
    }
}
