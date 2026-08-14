# Compatibility

## Build Matrix

| Minecraft | API used for compilation | Java to build | Java to run | Artifact | Status |
| --- | --- | --- | --- | --- | --- |
| 1.20.x | Paper API 1.20.1 | 17+ | 17+ | `TrueAuth-1.20.x.jar` | Supported |
| 1.21.x | Paper API 1.21.11 | 21+ | 21+ | `TrueAuth-1.21.x.jar` | Supported |
| 26.1 / 26.1.1 | Paper API 26.1.1 build 29 alpha | 25+ | 25+ | `TrueAuth-26.1.x.jar` | Supported |
| 26.2 | Paper API 26.2 build 112 stable | 25+ | 25+ | `TrueAuth-26.2.jar` | Supported |
| 26.3 | No published Paper API/build | 25+ | 25+ | None | Pending |

Minecraft 26.x requires Java 25 or newer. The local `mc-26` build was verified with JDK 26 because JDK 25 is not installed in the development environment.

## Paper-family Forks

The plugin uses Paper API and should work on forks that preserve the selected Paper API, including Purpur, Pufferfish, Leaf, Patina, Mohist, and Magma. These forks are not independently tested in CI yet.

Spigot-only servers are not supported by the current artifact because the implementation uses Paper's `AsyncChatEvent`. Folia requires a separate scheduler adapter.

## Adapter Roadmap

1. Extract platform-neutral authentication, password, locale, and database services.
2. Add a Bukkit/Spigot adapter using Bukkit chat events and schedulers.
3. Add a Folia adapter using entity, region, global, and async schedulers.
4. Add a separate Sponge implementation.
5. Keep Velocity and Waterfall as proxy modules; they cannot load a Bukkit plugin directly.

## Build Commands

```text
mvn clean package -Pmc-1.20 -DskipTests
mvn clean package -Pmc-1.21 -DskipTests
mvn clean package -Pmc-26-1 -DskipTests
mvn clean package -Pmc-26 -DskipTests
```

The `26.3` target must not be advertised until Paper publishes a matching API and server build.
