# ExamplePlugin

> **Note:** This is a **learning resource for developers**, not a production plugin. It demonstrates how to mod Hytale servers. Use it as a reference to build your own plugins.

An example Hytale plugin demonstrating various plugin/modding capabilities. See the [`docs/`](./docs/) folder for detailed documentation on various Hytale modding topics.

## Features

- **Flight System** - Toggle flight with automatic fall damage prevention
- **Kit System** - Create, edit, and distribute item kits
- **Waypoint System** - Server-wide teleport locations with map markers and permissions
- **Inventory Tools** - View other players' inventories, clear your own
- **Death Tracking** - Return to your last death location
- **Map Teleport** - Allow clicking map markers to teleport (even in Adventure mode)
- **UI Examples** - Various UI pages demonstrating forms, translations, and components
- **Armor HUD** - Displays equipped armor on screen (shown automatically on join)

## Commands

### `/back`
Teleports you to your last death location (same world only).

### `/clearinventory`
Clears your inventory.
- **Aliases:** `ci`

### `/editkit <name>`
Opens the kit editor for a specific kit.

### `/example info`
Shows plugin information with a clickable website link.

### `/fly`
Toggles flight mode. Fall damage is automatically prevented while flying.

### `/invsee <player>`
Opens another player's inventory.
- Without modify permission: Read-only view
- With modify permission: Can move items

### `/kit <name>`
Gives you a kit. Requires permission for the specific kit.

### `/waypoint <name>`
Teleports you to a waypoint. Requires permission for the specific waypoint.

### `/waypoints`
Opens the waypoint management UI (admin only). Allows creating, updating, deleting, and teleporting to waypoints.

### `/editkits`
Opens the kit list management UI.

### `/ui <subcommand>`
Opens various UI example pages.
- `basic` - Basic UI with raw text
- `translated` - UI with i18n support
- `register` - Registration form with inputs
- `login` - Login form
- `users` - User management UI
- `components` - All UI components showcase
- `lifetime-cantclose` - UI only closable via close button
- `lifetime-candismiss` - UI closable with escape
- `lifetime-candissmissorclose` - UI closable with escape or click outside

### `/gmc`
Shortcut for `gamemode creative`.

### `/gma`
Shortcut for `gamemode adventure`.

## Permissions

### Command Permissions

| Permission | Description |
|------------|-------------|
| `vulpeslab.exampleplugin.command.back` | Use `/back` |
| `vulpeslab.exampleplugin.command.clearinventory` | Use `/clearinventory` |
| `vulpeslab.exampleplugin.command.editkit` | Use `/editkit` |
| `vulpeslab.exampleplugin.command.editkits` | Use `/editkits` |
| `vulpeslab.exampleplugin.command.example` | Use `/example` |
| `vulpeslab.exampleplugin.command.fly` | Use `/fly` |
| `vulpeslab.exampleplugin.command.invsee` | Use `/invsee` (view only) |
| `vulpeslab.exampleplugin.command.invsee.modify` | Move items in `/invsee` |
| `vulpeslab.exampleplugin.command.kit` | Use `/kit` (base) |
| `vulpeslab.exampleplugin.command.ui` | Use `/ui` |
| `vulpeslab.exampleplugin.command.waypoint` | Use `/waypoint` (base) |
| `vulpeslab.exampleplugin.command.waypoints` | Use `/waypoints` (admin UI) |

### Kit Permissions

| Permission | Description |
|------------|-------------|
| `vulpeslab.exampleplugin.command.kit.<name>` | Use specific kit |

### Waypoint Permissions

| Permission | Description |
|------------|-------------|
| `vulpeslab.exampleplugin.waypoint.<name>` | Use specific waypoint + see on map |

### Feature Permissions

| Permission | Description |
|------------|-------------|
| `vulpeslab.exampleplugin.mapteleport` | Click any map marker to teleport (works in Adventure mode) |

## Building

```bash
./gradlew :plugin-example:build
```

The plugin JAR outputs to `../mods/` where the Hytale Server loads plugins from.
