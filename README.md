# TeamPing

TeamPing is a Minecraft 1.21.1 NeoForge mod focused on co-op survival and team-based communication. It adds team pings, a radial ping wheel, acknowledgement responses, and customizable icons.

## What It Does

- Team-based pings visible only to teammates
- Hold `G` to open the radial ping menu
- Hold `R` to send quick acknowledgement responses
- Server-side validation for ping creation and acknowledgements
- Configurable ping duration, cooldown, and range
- Client-side HUD rendering with distance and icon display

## Useful Files

- [Mod entry](src/main/java/dev/ithalo/teamping/TeamPing.java)
- [Client entry](src/main/java/dev/ithalo/teamping/client/TeamPingClient.java)
- [Ping input](src/main/java/dev/ithalo/teamping/client/PingWheelInputHandler.java)
- [Ping renderer](src/main/java/dev/ithalo/teamping/client/PingWheelRenderer.java)
- [HUD renderer](src/main/java/dev/ithalo/teamping/client/ClientPingRenderer.java)
- [Server ping logic](src/main/java/dev/ithalo/teamping/server/ServerPingManager.java)
- [Network payloads](src/main/java/dev/ithalo/teamping/network)
- [Config](src/main/java/dev/ithalo/teamping/config/TeamPingConfig.java)
- [Project overview](docs/TEAMPING_OVERVIEW.md)

## Setup

1. Open the project in IntelliJ IDEA or another Java IDE with NeoForge support.
2. Run `.\gradlew.bat build` to compile the mod.
3. Copy the jar from `build/libs/` into your Minecraft `mods` folder.

## Notes

- The mod is designed to work on dedicated servers.
- The server decides who receives each ping.
- The client only handles input, rendering, and local selection state.
- Icons are stored as PNG assets in the mod resources.

