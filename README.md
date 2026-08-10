# TrueAuth

TrueAuth is a lightweight registration and login plugin for Paper and Purpur servers running Minecraft 1.21.x. It isolates unauthenticated players in a void limbo and protects their inventory.

![TrueAuth icon](TrueAuth-icon.png)

## Features

- Void limbo with one configurable bedrock block and locked player movement
- Empty inventory and hidden health HUD before authentication
- Persistent localized ActionBar prompts in English and Russian
- BCrypt password hashing and SQLite or MySQL storage
- Saved logout locations, configurable registration spawn, and login fallback
- Safe random respawn ring for players without a bed or respawn anchor
- Administrative command with permission-aware tab completion

## Requirements

- Java 21
- Paper or Purpur for Minecraft 1.21.x

TrueAuth uses Paper API and is not advertised as compatible with Spigot, Bukkit, Folia, Velocity, or Waterfall.

## Installation

1. Download `TrueAuth.jar` from the release assets.
2. Place it in the server `plugins` directory.
3. Start the server and configure `plugins/TrueAuth/config.yml`.
4. Restart the server after changing the limbo world name or database settings.

The default limbo world is `trueauth_void`. Do not reuse a previously generated flat limbo world. If an old limbo folder is no longer needed, delete it only while the server is stopped.

## Player Commands

`/register <password> <repeat password>`

`/login <password>`

`/changepassword <old password> <new password>`

## Administration

`/trueauth help`

`/trueauth reload`

`/trueauth status <player>`

`/trueauth unregister <player>`

`/trueauth resetpassword <player> <new password>`

`/trueauth setregistration`

`/trueauth setloginfallback`

Grant `trueauth.admin` for all administrative commands, or grant `trueauth.admin.reload`, `trueauth.admin.account`, and `trueauth.admin.spawn` separately.

## Configuration

All player-facing text, including messages, Titles, and ActionBars, is editable in the active `lang/<locale>.yml` file. Available locales are `en_US`, `ru_RU`, `de_DE`, `fr_FR`, `it_IT`, `es_ES`, and `pt_BR`. Set `lang` in `config.yml`; an unknown locale safely falls back to English.

Post-authentication teleports and random respawn are disabled by default. With defaults, players return to their exact pre-limbo position after registering or logging in, and Minecraft handles respawning normally. Enable and configure `spawns.post-auth` or `spawns.random-respawn` only when needed.

## Publishing on Modrinth

- Project type: Plugin
- Supported loaders: Paper, Purpur, Leaves
- Supported game version: 1.21.11
- Upload file: `target/TrueAuth.jar`
- Use `TrueAuth-icon.png` as the project icon
- License: GPL-3.0-only
