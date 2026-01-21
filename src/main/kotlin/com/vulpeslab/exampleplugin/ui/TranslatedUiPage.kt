package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef

/**
 * Translated UI page example using i18n translation keys.
 * Demonstrates localized UI rendering with support for multiple languages.
 * Translations are defined in resources/Server/Languages/{locale}/exampleplugin/ui.lang
 * Keys are auto-prefixed with path: "translated.title" becomes "exampleplugin.ui.translated.title"
 */
class TranslatedUiPage(playerRef: PlayerRef) :
    BasicCustomUIPage(playerRef, CustomPageLifetime.CanDismiss) {

    override fun build(commandBuilder: UICommandBuilder) {
        commandBuilder.append("Pages/ExamplePluginUiExample.ui")
        commandBuilder.set("#TitleLabel.TextSpans", Message.translation("exampleplugin.ui.translated.title"))
        commandBuilder.set("#Description.TextSpans", Message.translation("exampleplugin.ui.translated.description"))
    }
}
