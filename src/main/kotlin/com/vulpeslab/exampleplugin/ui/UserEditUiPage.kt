package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.EnumCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
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
 * UI page for editing a user's details.
 */
class UserEditUiPage(playerRef: PlayerRef, private val userId: Long) :
    InteractiveCustomUIPage<UserEditUiPage.PageData>(
        playerRef,
        CustomPageLifetime.CanDismissOrCloseThroughInteraction,
        PageData.CODEC
    ) {

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/ExamplePluginUserEditPage.ui")

        // Event data for save action
        val saveEventData = EventData()
            .append("Action", Action.Save.name)
            .append("@Username", "#UsernameInput.Value")
            .append("@Email", "#EmailInput.Value")
            .append("@Password", "#PasswordInput.Value")

        // Bind save button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SaveButton",
            saveEventData
        )

        // Bind cancel button
        val cancelEventData = EventData()
            .append("Action", Action.Cancel.name)
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelButton",
            cancelEventData
        )

        // Bind Enter key on inputs
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#UsernameInput",
            saveEventData
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#EmailInput",
            saveEventData
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Validating,
            "#PasswordInput",
            saveEventData
        )

        // Load current user data asynchronously
        DatabaseManager.getUserByIdAsync(userId).thenAccept { user ->
            if (user != null) {
                val updateBuilder = UICommandBuilder()
                updateBuilder.set("#UsernameInput.Value", user.username)
                updateBuilder.set("#EmailInput.Value", user.email)
                sendUpdate(updateBuilder, null, false)
            }
        }
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.Save -> {
                val username = data.username?.takeIf { it.isNotBlank() }
                val email = data.email?.takeIf { it.isNotBlank() }
                val password = data.password?.takeIf { it.isNotBlank() }

                // Update user in database (async)
                DatabaseManager.updateUserAsync(userId, username, email, password).thenAccept {
                    // Go back to users list
                    player.pageManager.openCustomPage(ref, store, UsersListUiPage(playerRef))

                    // Send success message
                    playerRef.sendMessage(Message.translation("exampleplugin.ui.users.edit.success"))
                }
            }
            Action.Cancel -> {
                // Go back to users list
                player.pageManager.openCustomPage(ref, store, UsersListUiPage(playerRef))
            }
        }
    }

    enum class Action {
        Save,
        Cancel
    }

    class PageData {
        var action: Action = Action.Save
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
