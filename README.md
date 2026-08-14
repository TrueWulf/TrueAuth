# TrueAuth

TrueAuth is a lightweight registration and login plugin for Paper and Purpur servers running Minecraft 1.20.x, 1.21.x, and 26.x. It isolates unauthenticated players in a void limbo and protects their inventory.

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
- Paper or Purpur for Minecraft 1.20.x, 1.21.x, or 26.x

TrueAuth uses the Paper API. Paper-family forks such as Purpur, Pufferfish, Leaf, Patina, Mohist, and Magma may work when they expose the selected Paper API, but the release artifacts are compiled and tested against Paper. Folia, Spigot-only servers, Velocity, Waterfall, and Sponge require separate adapters and are not included in this release.

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
| Paper/Purpur 1.20.x | `mvn package -Pmc-1.20` | 17 | Supported |
| Paper/Purpur 1.21.x | `mvn package -Pmc-1.21` | 21 | Supported |
| Paper/Purpur 26.1/26.1.1 | `mvn package -Pmc-26-1` | 25 to build/run | Supported |
| Paper/Purpur 26.2 | `mvn package -Pmc-26` | 25 to build/run | Supported |
| Paper/Purpur 26.3 | Not available yet | 25 | Pending official Paper API/build |

The `mc-26-1` profile uses `paper-api:26.1.1.build.29-alpha`; the `mc-26` profile uses `paper-api:26.2.build.112-stable`. A dedicated 26.3 build will be added when Paper publishes its API and server build.

For the detailed compatibility matrix and adapter roadmap, see [`COMPATIBILITY.md`](COMPATIBILITY.md).

## Publishing on Modrinth

- Project type: Plugin
- Supported loaders: Paper, Purpur, Leaves
- Supported game versions: 1.20.x, 1.21.x, 26.1.x, and 26.2
- Upload the matching artifact: `target/TrueAuth-1.20.x.jar`, `target/TrueAuth-1.21.x.jar`, `target/TrueAuth-26.1.x.jar`, or `target/TrueAuth-26.2.jar`
- Use `TrueAuth-icon.png` as the project icon
- License: GPL-3.0-only
