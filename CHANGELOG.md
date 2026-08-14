# Changelog

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
