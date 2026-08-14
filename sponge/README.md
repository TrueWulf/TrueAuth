# TrueAuth Sponge Adapter

This module is a separate Sponge plugin and cannot be loaded as a Bukkit/Paper plugin.

## Compatibility

- Sponge API 8.2.x
- Java 17+
- SQLite storage
- BCrypt password hashing

The first Sponge adapter provides `/register` and `/login`, authentication timeout, chat and command restrictions, movement/damage/interact restrictions, and account persistence. The Bukkit limbo-world and inventory snapshot implementation is intentionally not shared with this module.

## Build

```text
mvn -f sponge/pom.xml clean package -DskipTests
```

The artifact is `sponge/target/TrueAuth-Sponge-8.x.jar`.
