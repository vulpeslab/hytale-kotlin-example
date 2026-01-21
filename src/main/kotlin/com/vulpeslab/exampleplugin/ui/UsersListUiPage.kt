package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.vulpeslab.exampleplugin.services.DatabaseManager

/**
 * UI page showing a list of all registered users.
 */
class UsersListUiPage(playerRef: PlayerRef) :
    InteractiveCustomUIPage<UsersListUiPage.PageData>(
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
        commandBuilder.append("Pages/ExamplePluginUsersListPage.ui")

        // Load users asynchronously and update the UI when ready
        DatabaseManager.getAllUsersAsync().thenAccept { users ->
            val updateBuilder = UICommandBuilder()
            val updateEventBuilder = UIEventBuilder()

            updateBuilder.clear("#UserList")
            users.forEachIndexed { index, user ->
                val selector = "#UserList[$index]"
                updateBuilder.append("#UserList", "Pages/ExamplePluginUserListItem.ui")
                updateBuilder.set("$selector #UsernameLabel.Text", user.username)
                updateBuilder.set("$selector #EmailLabel.Text", user.email)
                updateBuilder.set("$selector #UuidLabel.Text", user.hytaleUuid?.toString() ?: "N/A")

                updateEventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #UserButton",
                    EventData().append("UserId", user.id.toString())
                )
            }

            sendUpdate(updateBuilder, updateEventBuilder, false)
        }
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val userId = data.userId?.toLongOrNull() ?: return

        // Open edit page for this user
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        player.pageManager.openCustomPage(ref, store, UserEditUiPage(playerRef, userId))
    }

    class PageData {
        var userId: String? = null

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("UserId", Codec.STRING),
                    { obj, userId -> obj.userId = userId },
                    { obj -> obj.userId }
                ).add()
                .build()
        }
    }
}
