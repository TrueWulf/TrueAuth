# TrueAuth

TrueAuth is a lightweight registration and login plugin for Bukkit-compatible servers running Minecraft 1.20.x, 1.21.x, and 26.x. It isolates unauthenticated players in a void limbo and protects their inventory.

## Features

- Void limbo with one configurable bedrock block and locked player movement
- Empty inventory and hidden health HUD before authentication
- Persistent localized ActionBar prompts in English and Russian
- BCrypt password hashing and SQLite or MySQL storage
- Saved logout locations, configurable registration spawn, and login fallback
- Safe random respawn ring for players without a bed or respawn anchor
- Administrative command with permission-aware tab completion

## Requirements

- Java 17 for 1.20.x, Java 21 for 1.21.x, and Java 25 for 26.x
- A Bukkit-compatible server core for Minecraft 1.20.x, 1.21.x, or 26.x

TrueAuth targets Bukkit-compatible server cores: Paper, Purpur, Spigot, Pufferfish, Leaf, Patina, Arclight, Mohist, and Folia. The same versioned artifact is used on compatible cores. Sponge is supported by a separate Sponge API 8.2 adapter; Velocity and Waterfall require separate proxy modules.

## Installation

1. Download the artifact matching your server version from the release assets.
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

## Supported targets

| Target | Build command | Java | Status |
| --- | --- | --- | --- |
| Bukkit-compatible 1.20.x | `mvn package -Pspigot-1.20` | 17 | Supported |
| Bukkit-compatible 1.21.x | `mvn package -Pspigot-1.21` | 21 | Supported |
| Paper-family 1.20.x | `mvn package -Pmc-1.20` | 17 | Supported |
| Paper-family 1.21.x | `mvn package -Pmc-1.21` | 21 | Supported |
| Paper-family 26.x | See separate 26.1/26.2 artifacts below | 25 to build/run | Supported |
| 26.3 | Not available yet | 25 | Pending official Paper API/build |
| Sponge API 8.2.x | `mvn -f sponge/pom.xml package` | 17 | Build verified; runtime pending |

 The Bukkit-compatible core list is: Paper, Purpur, Spigot, Pufferfish, Leaf, Patina, Arclight, Mohist, and Folia. Sponge servers use the separate `TrueAuth-Sponge-8.x.jar` adapter and must not use the Bukkit/Paper JAR.
 
 Minecraft 26.x currently remains two separate release artifacts: `TrueAuth-26.1.x.jar` is built with `paper-api:26.1.1.build.29-alpha`, and `TrueAuth-26.2.jar` with `paper-api:26.2.build.112-stable`. A dedicated 26.3 build will be added when Paper publishes its API and server build.
 
 For the detailed compatibility matrix and adapter roadmap, see [`COMPATIBILITY.md`](COMPATIBILITY.md).
