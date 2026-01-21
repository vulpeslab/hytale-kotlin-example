package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef

/**
 * Demonstrates CustomPageLifetime.CanDismissOrCloseThroughInteraction - the page can be
 * closed by pressing escape OR by clicking outside the UI container.
 */
class LifetimeCanDismissOrCloseUiPage(playerRef: PlayerRef) :
    BasicCustomUIPage(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction) {

    override fun build(commandBuilder: UICommandBuilder) {
        // Uses no-overlay version so clicks pass through to game world
        commandBuilder.append("Pages/ExamplePluginLifetimePageNoOverlay.ui")
        commandBuilder.set("#TitleLabel.TextSpans", Message.translation("exampleplugin.ui.lifetime.title"))
        commandBuilder.set("#LifetimeLabel.TextSpans", Message.raw("CanDismissOrCloseThroughInteraction"))
        commandBuilder.set("#Description.TextSpans", Message.translation("exampleplugin.ui.lifetime.candismissorclose.description"))
        commandBuilder.set("#CloseButton.Visible", false)
    }
}
