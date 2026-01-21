package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef

/**
 * Basic UI page example using raw (non-translated) text.
 * Demonstrates simple UI rendering with hardcoded strings.
 */
class BasicUiPage(playerRef: PlayerRef) :
    BasicCustomUIPage(playerRef, CustomPageLifetime.CanDismiss) {

    override fun build(commandBuilder: UICommandBuilder) {
        commandBuilder.append("Pages/ExamplePluginUiExample.ui")
        commandBuilder.set("#TitleLabel.TextSpans", Message.raw("Basic UI Example"))
        commandBuilder.set("#Description.TextSpans", Message.raw("This text is hardcoded and not translated."))
    }
}
