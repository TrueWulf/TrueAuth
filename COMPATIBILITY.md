# Compatibility

## Build Matrix

| Minecraft | API used for compilation | Java to build | Java to run | Artifact | Status |
| --- | --- | --- | --- | --- | --- |
| 1.20.x | Paper API 1.20.1 | 17+ | 17+ | `TrueAuth-1.20.x.jar` | Supported |
| 1.21.x | Paper API 1.21.11 | 21+ | 21+ | `TrueAuth-1.21.x.jar` | Supported |
| 1.20.x | Spigot API 1.20.1 | 17+ | 17+ | `TrueAuth-Spigot-1.20.x.jar` | Supported |
| 1.21.x | Spigot API 1.21.1 | 21+ | 21+ | `TrueAuth-Spigot-1.21.x.jar` | Supported |
| 26.x (26.1 / 26.1.1) | Paper API 26.1.1 build 29 alpha | 25+ | 25+ | `TrueAuth-26.1.x.jar` | Supported |
| 26.x (26.2) | Paper API 26.2 build 112 stable | 25+ | 25+ | `TrueAuth-26.2.jar` | Supported |
| Sponge 8.2.x | Sponge API 8.2.0 | 17+ | 17+ | `TrueAuth-Sponge-8.x.jar` | Build verified; runtime pending |
| 26.3 | No published Paper API/build | 25+ | 25+ | None | Pending |

Minecraft 26.x requires Java 25 or newer. The local `mc-26` build was verified with JDK 26 because JDK 25 is not installed in the development environment.

## Bukkit-Compatible Cores

The main artifact uses Bukkit APIs only and is intended for these cores when the selected Minecraft API is available:

- Paper
- Purpur
- Spigot
- Pufferfish
- Leaf
- Patina
- Arclight
- Mohist

Arclight and Mohist may expose extra Forge behavior, but TrueAuth does not depend on Forge internals. Use the matching `Spigot-*` artifact on these cores; these cores should be tested with the matching server version before production use.

Folia is supported through the runtime scheduler adapter and `folia-supported: true`. The current build still requires the corresponding Bukkit/Paper API at compile time for the Minecraft-version profiles.

Sponge is a separate platform. Install `TrueAuth-Sponge-8.x.jar` on Sponge API 8.2.x servers; do not install the Bukkit/Paper artifact there. The Sponge adapter currently provides registration, login, BCrypt/SQLite storage, authentication timeout, command/chat restrictions, and basic interaction restrictions. Its runtime behavior still requires a real Sponge server test.

## Adapter Roadmap

1. Add independent runtime tests for Spigot, Arclight, Mohist, and Sponge.
2. Keep Velocity and Waterfall as proxy modules; they cannot load a Bukkit or Sponge plugin directly.

## Build Commands

```text
mvn clean package -Pmc-1.20 -DskipTests
mvn clean package -Pmc-1.21 -DskipTests
mvn clean package -Pmc-26-1 -DskipTests
mvn clean package -Pmc-26 -DskipTests
mvn -f sponge/pom.xml clean package -DskipTests
```

The `26.3` target must not be advertised until Paper publishes a matching API and server build.
