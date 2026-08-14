<div align="center">

# TrueAuth

**Fast, lightweight authentication for Minecraft servers.**

Protect player accounts with BCrypt, SQLite or MySQL and a secure pre-login state. TrueAuth is available for Bukkit, Paper, Folia, Sponge, Velocity, and Waterfall, with separate artifacts for each platform.

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x%20%7C%201.21.x%20%7C%2026.x-2ea043?style=flat-square)
![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-e76f00?style=flat-square)
![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square)
![Build](https://img.shields.io/github/actions/workflow/status/TrueWulf/TrueAuth/build.yml?branch=main&style=flat-square&label=build)

[Support TrueAuth on Ko-fi](https://ko-fi.com/truewulf/goal?g=0)

</div>

## Overview

TrueAuth is a small, fast, and easy-to-configure authentication solution for Minecraft networks and standalone servers. Players remain in a protected state until they register or log in, while password handling, database access, timeouts, and platform-specific scheduling are kept safe and efficient.

It works across the most common Minecraft server and proxy platforms without forcing one plugin artifact to do everything:

- **Bukkit, Paper, and Folia** for standalone servers and Paper-family networks
- **Sponge** through a dedicated Sponge 8.2.x plugin
- **Velocity** and **Waterfall** through dedicated proxy adapters

The project is designed to be lightweight in runtime overhead, quick to start, and straightforward to deploy on both small servers and multi-server networks.

## Features

- Protected void limbo with configurable bedrock platform
- Locked movement and interaction before authentication
- Temporary inventory protection and health HUD handling
- Localized titles, ActionBar prompts, and messages
- BCrypt password hashing
- SQLite or MySQL storage on Bukkit/Paper
- SQLite or MySQL storage for proxy authentication
- TLS support for proxy MySQL connections
- Authentication timeouts, rate limits, and protection against concurrent login attempts
- Saved logout locations and configurable login fallbacks
- Optional safe random respawn ring
- Permission-aware administration commands

## Compatibility

### Bukkit, Paper, and Folia

| Minecraft | Artifact | Java | Build status |
| --- | --- | --- | --- |
| 1.20.x | `TrueAuth-1.20.x.jar` or `TrueAuth-Spigot-1.20.x.jar` | 17 | Build verified |
| 1.21.x | `TrueAuth-1.21.x.jar` or `TrueAuth-Spigot-1.21.x.jar` | 21 | Build verified |
| 26.x | `TrueAuth-26.1.x.jar` and `TrueAuth-26.2.jar` | 25 | Build verified |

`TrueAuth-1.20.x.jar` is compiled against Paper API 1.20.1 and is intended for Paper, Folia, Purpur, Bukkit-compatible Spigot implementations, Patina, and Leaf on Minecraft 1.20.x. Use `TrueAuth-Spigot-1.20.x.jar` when you specifically want the Spigot API build.

`TrueAuth-1.21.x.jar` and `TrueAuth-Spigot-1.21.x.jar` follow the same distinction for Minecraft 1.21.x. Arclight and Mohist are not separately runtime-tested and should be treated as best-effort compatibility.

Folia uses the platform scheduler adapter and `folia-supported: true`.

### Sponge

Sponge uses a separate plugin and must not load the Bukkit/Paper artifact.

- API: Sponge 8.2.x
- Artifact: `TrueAuth-Sponge-8.x.jar`
- Includes registration, login, BCrypt/SQLite storage, timeout handling, and pre-authentication restrictions
- Build verified; runtime testing on a Sponge server is still pending

### Proxy adapters

Proxy authentication is provided by separate JARs:

- `TrueAuth-Velocity.jar` for Velocity 3.4.x and newer compatible Velocity 3.x proxies
- `TrueAuth-Waterfall.jar` for Waterfall/BungeeCord-compatible proxies using the BungeeCord API

Do not install proxy JARs in a Bukkit/Paper/Sponge server, or Bukkit JARs in a proxy.

### Proxy deployment notes

The proxy adapters authenticate players before backend forwarding. Use MySQL when multiple proxy instances must share accounts; SQLite is intended for a single proxy instance.

## Choose The Right Artifact

Install only the artifact matching your platform:

| Platform | Artifact |
| --- | --- |
| Paper, Folia, Purpur, or Bukkit-compatible server on Minecraft 1.20.x | `TrueAuth-1.20.x.jar` |
| Spigot or a Spigot-compatible server on Minecraft 1.20.x | `TrueAuth-Spigot-1.20.x.jar` |
| Paper-family or Bukkit-compatible server on Minecraft 1.21.x | `TrueAuth-1.21.x.jar` |
| Spigot or a Spigot-compatible server on Minecraft 1.21.x | `TrueAuth-Spigot-1.21.x.jar` |
| Paper-family server on Minecraft 26.1.x or 26.2 | matching `TrueAuth-26.*.jar` |
| Sponge API 8.2.x | `TrueAuth-Sponge-8.x.jar` |
| Velocity 3.4.x+ | `TrueAuth-Velocity.jar` |
| Waterfall or BungeeCord-compatible proxy | `TrueAuth-Waterfall.jar` |

Do not install proxy adapters on a Bukkit, Paper, or Sponge server. Do not install Bukkit/Paper artifacts on a proxy or Sponge server.

## Installation

1. Download the artifact for your platform and Minecraft version.
2. Put the JAR in the server `plugins` directory.
3. Start the server once.
4. Edit `plugins/TrueAuth/config.yml` if needed.

For a proxy deployment, install the proxy adapter on the proxy and use MySQL when multiple proxy instances need to share accounts. SQLite is intended for a single proxy instance.

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
mvn -f proxy-common/pom.xml clean install -DskipTests
mvn -f velocity/pom.xml clean package -DskipTests
mvn -f waterfall/pom.xml clean package -DskipTests
```

The 26.x profiles remain separate internally because 26.1 and 26.2 use different Paper API builds and produce separate release artifacts.

## License

TrueAuth is distributed under the GNU General Public License v3.0. See [`LICENSE`](LICENSE).

## Support

If TrueAuth is useful to your server, you can support development on [Ko-fi](https://ko-fi.com/truewulf/goal?g=0).

For the detailed matrix and platform notes, see [`COMPATIBILITY.md`](COMPATIBILITY.md).
