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
import com.vulpeslab.exampleplugin.services.SessionManager

/**
 * Login form UI page.
 */
class LoginUiPage(playerRef: PlayerRef) :
    InteractiveCustomUIPage<LoginUiPage.PageData>(
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
        commandBuilder.append("Pages/ExamplePluginLoginPage.ui")

        // Event data for login action
        val loginEventData = EventData()
            .append("Action", Action.Login.name)
            .append("@Username", "#UsernameInput.Value")
            .append("@Password", "#PasswordInput.Value")

        // Bind login button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#LoginButton",
            loginEventData
        )

        // Bind Enter key on username input
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#UsernameInput",
            loginEventData
        )

        // Bind Enter key on password input
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#PasswordInput",
            loginEventData
        )
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType())

        when (data.action) {
            Action.Login -> {
                val username = data.username ?: ""
                val password = data.password ?: ""

                // Get player UUID
                @Suppress("DEPRECATION")
                val playerUuid = player?.getUuid()
                if (playerUuid == null) {
                    showError(Message.translation("exampleplugin.ui.login.failed"))
                    return
                }

                // Verify credentials and UUID match (async)
                DatabaseManager.verifyCredentialsAsync(playerUuid, username, password).thenAccept { isValid ->
                    if (isValid) {
                        // Store session
                        SessionManager.login(playerUuid, username)

                        // Close the page
                        player.pageManager.setPage(ref, store, Page.None)

                        // Send success message
                        playerRef.sendMessage(Message.translation("exampleplugin.ui.login.success"))
                    } else {
                        // Show error in UI
                        showError(Message.translation("exampleplugin.ui.login.failed"))
                    }
                }
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
        Login
    }

    class PageData {
        var action: Action = Action.Login
        var username: String? = null
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
                    KeyedCodec("@Password", Codec.STRING),
                    { obj, password -> obj.password = password },
                    { obj -> obj.password }
                ).add()
                .build()
        }
    }
}
