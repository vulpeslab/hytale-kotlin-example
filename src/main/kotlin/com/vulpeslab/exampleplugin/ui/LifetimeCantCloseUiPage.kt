package com.vulpeslab.exampleplugin.ui

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

/**
 * Demonstrates CustomPageLifetime.CantClose - the page cannot be closed by pressing
 * escape or clicking outside. Only the explicit close button works.
 */
class LifetimeCantCloseUiPage(playerRef: PlayerRef) :
    InteractiveCustomUIPage<LifetimeCantCloseUiPage.PageData>(
        playerRef,
        CustomPageLifetime.CantClose,
        PageData.CODEC
    ) {

    override fun build(
        ref: Ref<EntityStore>,
        commandBuilder: UICommandBuilder,
        eventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        commandBuilder.append("Pages/ExamplePluginLifetimePage.ui")
        commandBuilder.set("#TitleLabel.TextSpans", Message.translation("exampleplugin.ui.lifetime.title"))
        commandBuilder.set("#LifetimeLabel.TextSpans", Message.raw("CantClose"))
        commandBuilder.set("#Description.TextSpans", Message.translation("exampleplugin.ui.lifetime.cantclose.description"))

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData().append("Action", Action.Close.name)
        )
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: PageData) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        when (data.action) {
            Action.Close -> {
                player.pageManager.setPage(ref, store, Page.None)
            }
        }
    }

    enum class Action {
        Close
    }

    class PageData {
        var action: Action = Action.Close

        companion object {
            val CODEC: BuilderCodec<PageData> = BuilderCodec.builder(PageData::class.java) { PageData() }
                .append(
                    KeyedCodec("Action", EnumCodec(Action::class.java, EnumCodec.EnumStyle.LEGACY)),
                    { obj, action -> obj.action = action },
                    { obj -> obj.action }
                ).add()
                .build()
        }
    }
}
