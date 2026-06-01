# TeamPing

[![Build](https://github.com/italozkv/TeamPing/actions/workflows/build.yml/badge.svg)](https://github.com/italozkv/TeamPing/actions/workflows/build.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.209%2B-FF7A18)](https://neoforged.net/)

TeamPing is a Minecraft 1.21.1 NeoForge mod made for co-op survival and small multiplayer servers.

It gives teams a fast way to ping locations, call out danger, mark loot, and answer without stopping to type in chat.

The server stays in charge of validation and visibility. Players get a clean, flexible way to communicate.

## Highlights

- Team-only pings for blue, red, green, and yellow teams
- Hold `G` for the ping radial, or tap `G` for a quick location ping
- Hold `R` for quick ping responses
- Use `U` to customize radial icons and quick response messages
- Open the gear menu for client ping settings
- Pings show distance, team color, and a type icon in-world
- Dropped items and mobs can be pinged, including moving entities
- Ping acknowledgements appear in chat, such as `Player B replied to your ping: On my way`
- Works with dedicated servers and LAN play

## Controls

| Action | Default |
| --- | --- |
| Quick ping | Tap `G` |
| Ping radial | Hold `G` |
| Response radial | Hold `R` while looking at a ping |
| Extra customization menu | Press `U` |
| Understood | `Z` while looking at a ping |
| On my way | `X` while looking at a ping |
| Negative | `C` while looking at a ping |
| Checking | `V` while looking at a ping |

## Team Commands

```text
/pingteam azul
/pingteam vermelho
/pingteam verde
/pingteam amarelo
/pingteam sair
/pingteam reload
```

Only players on the same TeamPing team can see each other's pings and responses.

## Customization

The `U` menu lets players customize their client-side experience:

- replace icons used by radial ping slots
- edit quick response messages
- keep those choices locally between sessions
- open client ping settings from the gear button

Quick messages are still validated and distributed by the server when sent. The client chooses the wording, but it does not choose who receives the response.

The client settings screen lets each player tune:

- ping duration
- ping icon scale
- marker brightness
- ACK toast duration

The server can allow or lock those values through `config/PingTeam/server.properties`. Personal client settings are stored in `config/PingTeam/client.properties`.

## Config

Server config includes:

- ping duration
- ping cooldown
- maximum ping range
- maximum active pings per player
- cross-dimension ping behavior
- client override permissions
- minimum and maximum allowed ping duration
- default icon scale and marker brightness
- acknowledgement interaction distance
- acknowledgement response cooldown
- ACK chat messages and sound toggles

Defaults are tuned for survival play: pings last 10 seconds and can target up to 216 blocks away.

## Build

```powershell
.\gradlew.bat build
```

The test jar is generated in:

```text
build/libs/
```

## Useful Files

- [Project overview](docs/TEAMPING_OVERVIEW.md)
- [Changelog](CHANGELOG.md)
- [Mod entry](src/main/java/dev/ithalo/teamping/TeamPing.java)
- [Client entry](src/main/java/dev/ithalo/teamping/client/TeamPingClient.java)
- [Input handler](src/main/java/dev/ithalo/teamping/client/PingWheelInputHandler.java)
- [Radial renderer](src/main/java/dev/ithalo/teamping/client/PingWheelRenderer.java)
- [World ping renderer](src/main/java/dev/ithalo/teamping/client/ClientPingRenderer.java)
- [Client settings screen](src/main/java/dev/ithalo/teamping/client/ClientPingSettingsScreen.java)
- [Server ping logic](src/main/java/dev/ithalo/teamping/server/ServerPingManager.java)
- [Networking](src/main/java/dev/ithalo/teamping/network)
- [Server config](src/main/java/dev/ithalo/teamping/config/PingTeamServerSettings.java)

## Project Notes

- Minecraft: `1.21.1`
- Built with NeoForge: `21.1.231`
- Required NeoForge range: `21.1.209+`
- License: All Rights Reserved

TeamPing is still evolving. The current code is intentionally kept approachable so future addons, icon packs, presets, and extra radial actions can build on top of it.
