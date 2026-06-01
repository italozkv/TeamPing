# TeamPing Overview

TeamPing is a Minecraft 1.21.1 NeoForge mod for co-op survival and team communication. It adds a team-based ping system, a radial selection wheel, quick acknowledgement responses, and support for custom icons.

## Core Goals

- Keep communication fast in multiplayer survival
- Let the server remain authoritative
- Keep the client focused on input and rendering
- Make the UI easy to expand with new icons and actions

## Current Features

### Teams

- Join one of four teams: blue, red, green, or yellow
- Leave your team with a command
- Store the selected team per player
- Support dedicated servers and LAN play

### Ping System

- Hold `G` to open the radial ping wheel
- Release `G` to send the selected ping
- Quick tap `G` for a fast default ping
- Server-side validation for target, range, cooldown, and visibility
- Configurable duration, cooldown, range, and active ping limit
- Optional cross-dimension support

### Targets

- Blocks
- Dropped items
- Hostile mobs
- Passive mobs

Pings can follow moving entities while they are still valid.

### Acknowledgements

- Hold `R` to open a small response wheel
- Quick replies: Understood, On my way, Negative, and Checking
- Same-team players can see responses
- Creator and teammates receive acknowledgement updates

### Rendering

- Distance shown above each ping
- Icon changes based on ping type or selected custom icon
- HUD details appear when the player looks at a ping
- Compact notifications appear when acknowledgements arrive

### Custom Icons

- Main ping icons are stored as PNG assets
- Extra icons are available for player customization
- The radial UI can swap icons per segment

## Important Files

- `src/main/java/dev/ithalo/teamping/TeamPing.java`
- `src/main/java/dev/ithalo/teamping/client/TeamPingClient.java`
- `src/main/java/dev/ithalo/teamping/client/PingWheelInputHandler.java`
- `src/main/java/dev/ithalo/teamping/client/PingWheelRenderer.java`
- `src/main/java/dev/ithalo/teamping/client/ClientPingRenderer.java`
- `src/main/java/dev/ithalo/teamping/server/ServerPingManager.java`
- `src/main/java/dev/ithalo/teamping/network/*`
- `src/main/java/dev/ithalo/teamping/config/TeamPingConfig.java`
- `src/main/java/dev/ithalo/teamping/ping/*`

## Client Only

- Keybinds and mouse input
- Radial rendering
- HUD rendering
- Local icon selection state

## Server Side

- Ping validation
- Team validation
- Expiration and cleanup
- Acknowledgement distribution

## Future Ideas

- Better extra-icon browser
- Favorites and presets per team
- More ping target types
- Ping sounds per type
- Per-world or per-server presets

## Notes

- The server decides who receives each ping and acknowledgement.
- The client never decides distribution.
- The mod is designed to stay lightweight and easy to extend.
