package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef

/**
 * Demonstrates CustomPageLifetime.CanDismiss - the page can be closed by pressing
 * escape, but clicking outside does not close it.
 */
class LifetimeCanDismissUiPage(playerRef: PlayerRef) :
    BasicCustomUIPage(playerRef, CustomPageLifetime.CanDismiss) {

    override fun build(commandBuilder: UICommandBuilder) {
        commandBuilder.append("Pages/ExamplePluginLifetimePage.ui")
        commandBuilder.set("#TitleLabel.TextSpans", Message.translation("exampleplugin.ui.lifetime.title"))
        commandBuilder.set("#LifetimeLabel.TextSpans", Message.raw("CanDismiss"))
        commandBuilder.set("#Description.TextSpans", Message.translation("exampleplugin.ui.lifetime.candismiss.description"))
        commandBuilder.set("#CloseButton.Visible", false)
    }
}
