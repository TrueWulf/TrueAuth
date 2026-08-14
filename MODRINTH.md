# Modrinth Compatibility

| File | Platform | Minecraft versions | Java | Status |
| --- | --- | --- | --- | --- |
| `TrueAuth-1.20.x.jar` | Paper plugin | 1.20.x | 17+ | Paper-family build verified; Folia, Purpur, Patina, and Leaf use the Bukkit API layer |
| `TrueAuth-Spigot-1.20.x.jar` | Bukkit/Spigot plugin | 1.20.x | 17+ | Spigot build verified |
| `TrueAuth-1.21.x.jar` | Paper plugin | 1.21.x | 21+ | Paper-family build verified; Folia, Purpur, Patina, and Leaf use the Bukkit API layer |
| `TrueAuth-Spigot-1.21.x.jar` | Bukkit/Spigot plugin | 1.21.x | 21+ | Spigot build verified |
| `TrueAuth-26.1.x.jar` | Paper plugin | 26.1.x | 25+ | Matching Paper API build |
| `TrueAuth-26.2.jar` | Paper plugin | 26.2 | 25+ | Matching Paper API build |
| `TrueAuth-Sponge-8.x.jar` | Sponge plugin | Sponge API 8.2.x | 17+ | Separate adapter; build verified, runtime pending |
| `TrueAuth-Velocity.jar` | Velocity plugin | Velocity 3.4.x+ | 17+ | Separate proxy adapter; build verified, runtime pending |
| `TrueAuth-Waterfall.jar` | Waterfall/BungeeCord plugin | Waterfall-compatible | 17+ | Separate proxy adapter; build verified, runtime pending |

For `TrueAuth-1.20.x.jar` and `TrueAuth-1.21.x.jar`, list Paper, Folia, Purpur, Bukkit-compatible Spigot implementations, Patina, and Leaf as Bukkit API targets. Arclight and Mohist are best-effort and not runtime-tested.

Velocity and Waterfall require their own JARs. They are not supported by the Bukkit/Paper JARs.
