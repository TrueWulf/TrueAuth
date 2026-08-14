# Changelog

## 2.3.0

- Added Velocity and Waterfall proxy adapters with shared BCrypt and SQL account storage.
- Added Ko-fi support links and Modrinth compatibility guidance.
- Added proxy builds to CI and GitHub releases.

## 2.1.0

- Added build profiles and versioned artifacts for Minecraft 1.20.x, 1.21.x, 26.1.x, and 26.2.
- Lowered the shared bytecode target to Java 17 for 1.20.x compatibility.
- Replaced the deprecated player-only pickup event with `EntityPickupItemEvent`.
- Added a documented compatibility matrix and adapter roadmap.
- Added GitHub Actions builds and tag-based release publishing.

## 2.2.0

- Removed the Paper-only chat event and isolated Adventure formatting behind shaded dependencies.
- Added Bukkit/Spigot API profiles for 1.20.x and 1.21.x.
- Added runtime scheduler selection for Bukkit and Folia servers.
- Added Spigot, Arclight, and Bukkit-compatible core documentation.
- Replaced Magma with Arclight in the supported-core list.
- Added an independent Sponge API 8.2 adapter artifact.

## 1.6.0

- Disabled post-authentication teleports and random respawn by default.
- Added configurable post-authentication teleport options while preserving the player's pre-limbo location by default.
- Preserved vanilla Minecraft respawn behaviour by default.
- Improved invalid locale and configuration value handling.
- Documented Paper and Purpur compatibility for 1.21 through 1.21.11.

## 1.5.0

- Random respawn messages now show the player's death coordinates.
- Added German, French, Italian, Spanish, and Brazilian Portuguese locales.
- Updated project metadata and author to TrueWulf.

## 1.4.0

- Added a fresh void limbo world with an optional bedrock platform.
- Added persistent localized ActionBar authentication prompts.
- Added configurable registration and login fallback spawns.
- Added saved logout locations and safe random respawn handling.
- Added `/trueauth` administrative commands, permissions, and tab completion.
- Added command cooldown protection for registration and login attempts.
- Added English and Russian messages for random respawn coordinates.

## 2.0.0

- Prevented concurrent authentication requests for the same player.
- Preserved cursor items and player flight/invulnerability state through limbo.
- Saved authenticated logout locations during plugin shutdown.
- Secured dynamic locale values against MiniMessage tag injection.
- Added inventory drag and off-hand swap protection before authentication.
- Enabled configurable TLS for MySQL connections by default.
- Validated limbo world and authentication timeout configuration.
