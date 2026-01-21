# ExamplePlugin

> **Note:** This is a **learning resource for developers**, not a production plugin. It demonstrates how to mod Hytale servers. Use it as a reference to build your own plugins.

An example Hytale plugin demonstrating various plugin/modding capabilities. See the [`docs/`](./docs/) folder for detailed documentation on various Hytale modding topics.

## Features

### Player Systems
- **Flight System** - Toggle flight with automatic fall damage prevention
- **God Mode** - Toggle invulnerability, persisted across sessions
- **Death Tracking** - Return to your last death location with `/back`
- **Armor HUD** - Displays equipped armor and defense value on screen

### Item Management
- **Kit System** - Create, edit, and distribute item kits with UI
- **Inventory Tools** - View other players' inventories, clear your own
- **Repair Command** - Repair held item to full durability
- **Durability Command** - Set specific durability value on held item

### World Features
- **Waypoint System** - Server-wide teleport locations with map markers and permissions
- **Map Teleport** - Click map markers to teleport (works in Adventure mode)
- **Holograms** - Create floating text displays that always face the player
- **NPC Trading** - Create trader NPCs with configurable item trades

### UI Examples
- Various UI pages demonstrating forms, translations, and components
- User registration and login system with SQLite database
- Interactive kit browser with item selection

## Commands

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/fly` | Toggle flight mode (fall damage auto-prevented) | `vulpeslab.exampleplugin.command.fly` |
| `/god` | Toggle god mode (invulnerability) | `vulpeslab.exampleplugin.command.god` |
| `/back` | Teleport to last death location | `vulpeslab.exampleplugin.command.back` |
| `/suicide` | Kill yourself | `vulpeslab.exampleplugin.command.suicide` |
| `/clearinventory` | Clear your inventory (alias: `ci`) | `vulpeslab.exampleplugin.command.clearinventory` |
| `/repair` | Repair held item to full durability | `vulpeslab.exampleplugin.command.repair` |

### Kit Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/kit <name>` | Receive a kit | `vulpeslab.exampleplugin.command.kit` + `vulpeslab.exampleplugin.command.kit.<name>` |
| `/editkit <name>` | Open kit editor for a specific kit | `vulpeslab.exampleplugin.command.editkit` |
| `/editkits` | Open kit list management UI | `vulpeslab.exampleplugin.command.editkits` |

### Waypoint Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/waypoint <name>` | Teleport to a waypoint | `vulpeslab.exampleplugin.command.waypoint` + `vulpeslab.exampleplugin.waypoint.<name>` |
| `/waypoints` | Open waypoint management UI | `vulpeslab.exampleplugin.command.waypoints` |

### Hologram Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/holograms` | Open hologram management UI | `vulpeslab.exampleplugin.command.holograms` |

### NPC Trading Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/npc create <name>` | Create a trader NPC at your location | `vulpeslab.exampleplugin.command.npc.create` |
| `/npc edit` | Toggle NPC edit mode | `vulpeslab.exampleplugin.command.npc.edit` |
| `/npc clear` | Remove all trader NPCs from the world | `vulpeslab.exampleplugin.command.npc.clear` |

### Utility Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/example info` | Show plugin information and update status | None (public) |
| `/example durability <amount>` | Set durability of held item | `vulpeslab.exampleplugin.command.example.durability` |
| `/invsee <player>` | View another player's inventory | `vulpeslab.exampleplugin.command.invsee` |
| `/ui <subcommand>` | Open various UI example pages | `vulpeslab.exampleplugin.command.ui` |
| `/gmc` | Shortcut for `gamemode creative` | (inherits from gamemode) |
| `/gma` | Shortcut for `gamemode adventure` | (inherits from gamemode) |

### UI Subcommands

| Subcommand | Description |
|------------|-------------|
| `/ui basic` | Basic UI with raw text |
| `/ui translated` | UI with i18n support |
| `/ui register` | Registration form with inputs |
| `/ui login` | Login form |
| `/ui users` | User management UI |
| `/ui components` | All UI components showcase |
| `/ui lifetime-cantclose` | UI only closable via close button |
| `/ui lifetime-candismiss` | UI closable with escape |
| `/ui lifetime-candissmissorclose` | UI closable with escape or click outside |

## NPC Trading System

The NPC trading system allows you to create trader NPCs that players can interact with to exchange items.

### Setup

1. **Create an NPC**: `/npc create <name>` - Creates a trader NPC at your current location
2. **Enter edit mode**: `/npc edit` - Toggle edit mode to configure trades
3. **Configure trades**: Interact (press F) with the NPC while in edit mode to open the trade configuration UI
4. **Add trades**: Select input item, quantity, output item, and quantity
5. **Exit edit mode**: `/npc edit` - Toggle off to allow normal trading

### Player Trading

When not in edit mode, interacting with a trader NPC opens the trade execution UI where players can:
- View all available trades
- See trade affordability (green = can afford, red = cannot afford)
- Execute trades by clicking the trade button

## Hologram System

Create floating text displays that always face the player (billboard effect).

### Features
- Multi-line text support
- Automatic billboard rotation to face players
- Persistent storage (survives server restarts)
- Management UI for creating, editing, and deleting holograms

### Usage

1. Run `/holograms` to open the management UI
2. Click "Create Hologram" to add a new hologram at your location
3. Edit text by clicking on existing holograms
4. Delete holograms through the UI

## Permissions

### Command Permissions

| Permission | Description |
|------------|-------------|
| `vulpeslab.exampleplugin.command.back` | Use `/back` |
| `vulpeslab.exampleplugin.command.clearinventory` | Use `/clearinventory` |
| `vulpeslab.exampleplugin.command.editkit` | Use `/editkit` |
| `vulpeslab.exampleplugin.command.editkits` | Use `/editkits` |
| `vulpeslab.exampleplugin.command.example.durability` | Use `/example durability` |
| `vulpeslab.exampleplugin.command.fly` | Use `/fly` |
| `vulpeslab.exampleplugin.command.god` | Use `/god` |
| `vulpeslab.exampleplugin.command.holograms` | Use `/holograms` |
| `vulpeslab.exampleplugin.command.invsee` | Use `/invsee` (view only) |
| `vulpeslab.exampleplugin.command.invsee.modify` | Move items in `/invsee` |
| `vulpeslab.exampleplugin.command.kit` | Use `/kit` (base permission) |
| `vulpeslab.exampleplugin.command.npc.create` | Use `/npc create` |
| `vulpeslab.exampleplugin.command.npc.edit` | Use `/npc edit` |
| `vulpeslab.exampleplugin.command.npc.clear` | Use `/npc clear` |
| `vulpeslab.exampleplugin.command.repair` | Use `/repair` |
| `vulpeslab.exampleplugin.command.suicide` | Use `/suicide` |
| `vulpeslab.exampleplugin.command.ui` | Use `/ui` |
| `vulpeslab.exampleplugin.command.waypoint` | Use `/waypoint` (base permission) |
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
| `vulpeslab.exampleplugin.updatenotify` | See update notifications in `/example info` |

## ECS Systems

The plugin registers several Entity Component Systems:

| System | Description |
|--------|-------------|
| `FlyFallDamageFilterSystem` | Prevents fall damage for players with flight enabled |
| `PlayerDeathPositionSystem` | Tracks player death positions for `/back` command |
| `ArmorHudUpdateSystem` | Updates the armor HUD display for players |
| `HologramBillboardSystem` | Rotates holograms to face nearby players |
| `TraderNpcInteractionSystem` | Handles player interactions with trader NPCs |

## Data Storage

- **SQLite Database** (`data/exampleplugin.db`) - User accounts, god mode state
- **JSON Files**:
  - `data/kits.json` - Kit definitions
  - `data/waypoints.json` - Waypoint locations
  - `data/holograms.json` - Hologram data
  - `data/trader_npcs.json` - NPC and trade configurations

## Building

```bash
./gradlew :plugin-example:build
```

The plugin JAR outputs to `../mods/` where the Hytale Server loads plugins from.

## License

This project is licensed under the [MIT License](LICENSE). Feel free to use it as a reference for your own Hytale plugins.
