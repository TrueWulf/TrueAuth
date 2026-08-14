<div align="center">

# TrueAuth

**Fast, lightweight registration and authentication for Minecraft servers.**

Secure player accounts with BCrypt, SQLite or MySQL, a protected pre-login state, and a clean setup for modern Bukkit-compatible cores.

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x%20%7C%201.21.x%20%7C%2026.x-2ea043?style=flat-square)
![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-e76f00?style=flat-square)
![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square)
![Build](https://img.shields.io/github/actions/workflow/status/TrueWulf/TrueAuth/build.yml?branch=main&style=flat-square&label=build)

</div>

## Overview

TrueAuth keeps players in a protected state until they register or log in. It is designed to stay small, quick to start, and easy to configure on both classic Bukkit servers and modern Paper-family forks.

## Features

- Protected void limbo with configurable bedrock platform
- Locked movement and interaction before authentication
- Temporary inventory protection and health HUD handling
- Localized titles, ActionBar prompts, and messages
- BCrypt password hashing
- SQLite or MySQL storage on Bukkit/Paper
- Saved logout locations and configurable login fallbacks
- Optional safe random respawn ring
- Permission-aware administration commands

## Compatibility

### Bukkit and Paper

| Minecraft | Artifact | Java | Build status |
| --- | --- | --- | --- |
| 1.20.x | `TrueAuth-1.20.x.jar` or `TrueAuth-Spigot-1.20.x.jar` | 17 | Build verified |
| 1.21.x | `TrueAuth-1.21.x.jar` or `TrueAuth-Spigot-1.21.x.jar` | 21 | Build verified |
| 26.x | `TrueAuth-26.1.x.jar` and `TrueAuth-26.2.jar` | 25 | Build verified |

Supported Bukkit-compatible cores include Paper, Purpur, Spigot, Pufferfish, Leaf, Patina, Arclight, Mohist, and Folia. Folia uses the platform scheduler adapter.

### Sponge

Sponge uses a separate plugin and must not load the Bukkit/Paper artifact.

- API: Sponge 8.2.x
- Artifact: `TrueAuth-Sponge-8.x.jar`
- Includes registration, login, BCrypt/SQLite storage, timeout handling, and pre-authentication restrictions
- Build verified; runtime testing on a Sponge server is still pending

### Not Included

Velocity and Waterfall require separate proxy modules and are not Bukkit or Sponge server cores.

## Installation

1. Download the artifact for the server API and Minecraft version.
2. Put the JAR in the server `plugins` directory.
3. Start the server once.
4. Edit `plugins/TrueAuth/config.yml` if needed.

The default limbo world is `trueauth_void`. Stop the server before removing an old limbo world folder.

## Commands

### Players

```text
/register <password> <repeat password>
/login <password>
/changepassword <old password> <new password>
```

### Administrators

```text
/trueauth help
/trueauth reload
/trueauth status <player>
/trueauth unregister <player>
/trueauth resetpassword <player> <new password>
/trueauth setregistration
/trueauth setloginfallback
```

Grant `trueauth.admin` for full access, or use the individual permissions `trueauth.admin.reload`, `trueauth.admin.account`, and `trueauth.admin.spawn`.

## Configuration

Player-facing text is stored in `lang/<locale>.yml`. Included locales are English, Russian, German, French, Italian, Spanish, and Brazilian Portuguese.

Post-authentication teleportation and random respawn are disabled by default. Enable `spawns.post-auth` or `spawns.random-respawn` when those behaviors are required.

## Building

```text
mvn clean package -Pmc-1.20 -DskipTests
mvn clean package -Pmc-1.21 -DskipTests
mvn clean package -Pspigot-1.20 -DskipTests
mvn clean package -Pspigot-1.21 -DskipTests
mvn clean package -Pmc-26-1 -DskipTests
mvn clean package -Pmc-26 -DskipTests
mvn -f sponge/pom.xml clean package -DskipTests
```

The 26.x profiles remain separate internally because 26.1 and 26.2 use different Paper API builds and produce separate release artifacts.

## License

TrueAuth is distributed under the GNU General Public License v3.0. See [`LICENSE`](LICENSE).

For the detailed matrix and platform notes, see [`COMPATIBILITY.md`](COMPATIBILITY.md).
