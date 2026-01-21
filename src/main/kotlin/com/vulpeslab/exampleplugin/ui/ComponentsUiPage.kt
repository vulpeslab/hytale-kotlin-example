package com.vulpeslab.exampleplugin.ui

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef

/**
 * UI page showcasing all available UI components.
 * Demonstrates: Labels, Buttons, Text Fields, Password Fields, Number Fields,
 * Dropdowns, Checkboxes, Sliders, and Spinners.
 */
class ComponentsUiPage(playerRef: PlayerRef) :
    BasicCustomUIPage(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction) {

    override fun build(commandBuilder: UICommandBuilder) {
        commandBuilder.append("Pages/ExamplePluginComponentsPage.ui")

        // Note: Dropdown items must be defined in the UI file or appended individually
        // The UICommandBuilder.set() doesn't support List types for Items property
    }
}
