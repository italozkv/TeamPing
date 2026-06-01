# TeamPing

[![Build](https://github.com/italozkv/TeamPing/actions/workflows/build.yml/badge.svg)](https://github.com/italozkv/TeamPing/actions/workflows/build.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.231-FF7A18)](https://neoforged.net/)

TeamPing is a small co-op survival mod for Minecraft 1.21.1 that helps players talk without typing. It adds team pings, a radial menu, quick acknowledgement responses, and room for custom icons.

## ✨ What it feels like

- Mark danger, loot, mobs, or a place in one quick action
- Keep pings visible only to your team
- Hold `G` to open the radial wheel
- Hold `R` to send a fast response
- See distance and icon info right on the ping

## 🚀 Install

1. Build the mod with `.\gradlew.bat build`
2. Grab the jar from `build/libs/`
3. Drop it into your Minecraft `mods` folder

## 🔧 For players

- Use `/pingteam azul`, `/pingteam vermelho`, `/pingteam verde`, or `/pingteam amarelo`
- Use `/pingteam sair` to leave your team
- Open the config to tune ping duration, cooldown, and range

## 🧩 For builders

- [Project overview](docs/TEAMPING_OVERVIEW.md)
- [Changelog](CHANGELOG.md)
- [Mod entry](src/main/java/dev/ithalo/teamping/TeamPing.java)
- [Client input](src/main/java/dev/ithalo/teamping/client/PingWheelInputHandler.java)
- [Renderer](src/main/java/dev/ithalo/teamping/client/PingWheelRenderer.java)
- [Server ping logic](src/main/java/dev/ithalo/teamping/server/ServerPingManager.java)

## Notes

- The server decides who sees each ping.
- The client handles input and rendering only.
- Icons live in the mod resources as PNG files.
- The project is meant to feel light, readable, and easy to extend.
