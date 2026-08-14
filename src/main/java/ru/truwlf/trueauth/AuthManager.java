package ru.truwlf.trueauth;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class AuthManager {
    private final TrueAuthPlugin plugin;
    private final Set<UUID> unauthenticated = ConcurrentHashMap.newKeySet();
    private final Map<UUID, InventorySnapshot> inventories = new ConcurrentHashMap<>();
    private final Map<UUID, GameMode> gameModes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> invulnerable = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> allowFlight = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> flying = new ConcurrentHashMap<>();
    private final Map<UUID, Location> locations = new ConcurrentHashMap<>();
    private final Map<UUID, PlatformScheduler.TaskHandle> timeouts = new ConcurrentHashMap<>();
    private final Set<UUID> registered = ConcurrentHashMap.newKeySet();
    private final Set<UUID> skipLogoutSave = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> locationVersions = new ConcurrentHashMap<>();
    AuthManager(TrueAuthPlugin plugin) { this.plugin = plugin; }
    boolean isAuthenticated(Player player) { return !unauthenticated.contains(player.getUniqueId()); }
    boolean isRegistered(Player player) { return registered.contains(player.getUniqueId()); }
    void enterLimbo(Player player) {
        UUID id = player.getUniqueId();
        locationVersions.merge(id, 1L, Long::sum);
        unauthenticated.add(id);
        gameModes.put(id, player.getGameMode());
        invulnerable.put(id, player.isInvulnerable());
        allowFlight.put(id, player.getAllowFlight());
        flying.put(id, player.isFlying());
        locations.put(id, player.getLocation().clone());
        if (plugin.getConfig().getBoolean("limbo.hide-inventory", true)) {
            inventories.put(id, InventorySnapshot.capture(player.getInventory()));
            InventorySnapshot.clear(player.getInventory());
        }
        player.setInvulnerable(true);
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(limboLocation());
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.hidePlayer(plugin, player);
                player.hidePlayer(plugin, other);
            }
        }
        int seconds = plugin.getConfig().getInt("auth.login-timeout-seconds", 60);
        if (plugin.getConfig().getBoolean("auth.kick-on-timeout", true) && seconds > 0) timeouts.put(id, plugin.scheduler().runLater(player, () -> {
            if (!isAuthenticated(player)) player.kickPlayer(plugin.locale().message("timeout"));
        }, seconds * 20L));
        plugin.scheduler().runAsync(() -> {
            try {
                boolean exists = plugin.database().passwordHash(id).isPresent();
                plugin.scheduler().run(player, () -> {
                    if (!player.isOnline() || isAuthenticated(player)) return;
                    if (exists) registered.add(id);
                    showAuthenticationTitle(player, exists);
                });
            } catch (Exception exception) { plugin.getLogger().warning("Account state database error: " + exception.getMessage()); }
        });
    }
    void startPrompts() {
        plugin.scheduler().runTimer(() -> plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> !isAuthenticated(player))
                .forEach(player -> plugin.scheduler().run(player, () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(plugin.locale().message(isRegistered(player) ? "login-prompt" : "register-prompt"))))), 1L, 20L);
    }
    void showAuthenticationTitle(Player player, boolean isRegistered) {
        if (isAuthenticated(player)) return;
        String prefix = isRegistered ? "login" : "register";
        player.sendTitle(plugin.locale().message(prefix + "-title"), plugin.locale().message(prefix + "-subtitle"), 0, 100, 5);
    }
    boolean authenticate(Player player, boolean newlyRegistered, Database.SavedLocation savedLocation) {
        UUID id = player.getUniqueId();
        if (!unauthenticated.remove(id)) return false;
        PlatformScheduler.TaskHandle timeout = timeouts.remove(id); if (timeout != null) timeout.cancel();
        restorePlayerState(player, id);
        GameMode mode = gameModes.remove(id); if (mode != null) player.setGameMode(mode);
        InventorySnapshot inventory = inventories.remove(id); if (inventory != null) inventory.restore(player.getInventory());
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (!other.equals(player) && isAuthenticated(other)) {
                other.showPlayer(plugin, player);
                player.showPlayer(plugin, other);
            }
        }
        Location origin = locations.remove(id);
        Location destination = postAuthLocation(newlyRegistered, savedLocation, origin);
        if (destination != null) player.teleport(destination);
        registered.remove(id);
        player.resetTitle();
        return true;
    }
    void leave(Player player) {
        UUID id = player.getUniqueId();
        unauthenticated.remove(id);
        PlatformScheduler.TaskHandle timeout = timeouts.remove(id); if (timeout != null) timeout.cancel();
        restorePlayerState(player, id);
        InventorySnapshot inventory = inventories.remove(id); if (inventory != null) inventory.restore(player.getInventory());
        GameMode mode = gameModes.remove(id); if (mode != null) player.setGameMode(mode);
        locations.remove(id);
        registered.remove(id);
    }
    void forceLogout(Player player) {
        locationVersions.remove(player.getUniqueId());
        skipLogoutSave.add(player.getUniqueId());
        player.kickPlayer("Your TrueAuth account was reset by an administrator.");
    }
    boolean consumeSkipLogoutSave(Player player) { return skipLogoutSave.remove(player.getUniqueId()); }
    long locationSaveVersion(Player player) { return locationVersions.merge(player.getUniqueId(), 1L, Long::sum); }
    boolean isCurrentLocationSave(Player player, long version) { return locationVersions.getOrDefault(player.getUniqueId(), 0L) == version; }
    void finishLocationSave(Player player, long version) { locationVersions.remove(player.getUniqueId(), version); }
    void clearLocationSaveVersion(Player player) { locationVersions.remove(player.getUniqueId()); }
    void restoreAll() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            UUID id = player.getUniqueId();
            if (!unauthenticated.remove(id)) return;
            PlatformScheduler.TaskHandle timeout = timeouts.remove(id); if (timeout != null) timeout.cancel();
            restorePlayerState(player, id);
            GameMode mode = gameModes.remove(id); if (mode != null) player.setGameMode(mode);
            InventorySnapshot inventory = inventories.remove(id); if (inventory != null) inventory.restore(player.getInventory());
            Location location = locations.remove(id); if (location != null) player.teleport(location);
            registered.remove(id);
            locationVersions.remove(id);
            player.resetTitle();
            plugin.getServer().getOnlinePlayers().forEach(other -> {
                if (!other.equals(player)) {
                    other.showPlayer(plugin, player);
                    player.showPlayer(plugin, other);
                }
            });
        });
    }
    void saveAuthenticatedLocations() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isAuthenticated(player)) saveLocationSynchronously(player);
        }
    }
    private void saveLocationSynchronously(Player player) {
        Location location = player.getLocation();
        if (location.getWorld() == null || location.getWorld().equals(plugin.limboWorld())) return;
        Database.SavedLocation saved = new Database.SavedLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        try {
            plugin.database().saveLastLocation(player.getUniqueId(), saved);
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not save logout location for " + player.getName() + ": " + exception.getMessage());
        }
    }
    private void restorePlayerState(Player player, UUID id) {
        Boolean previousInvulnerable = invulnerable.remove(id);
        if (previousInvulnerable != null) player.setInvulnerable(previousInvulnerable);
        Boolean previousAllowFlight = allowFlight.remove(id);
        if (previousAllowFlight != null) player.setAllowFlight(previousAllowFlight);
        Boolean previousFlying = flying.remove(id);
        if (previousFlying != null && Boolean.TRUE.equals(previousAllowFlight)) player.setFlying(previousFlying);
    }
    private Location registrationLocation() { return configuredLocation("spawns.registration"); }
    private Location loginFallbackLocation() { return configuredLocation("spawns.login-fallback"); }
    private Location postAuthLocation(boolean newlyRegistered, Database.SavedLocation savedLocation, Location origin) {
        if (newlyRegistered) return plugin.getConfig().getBoolean("spawns.post-auth.registration-teleport", false) ? registrationLocation() : origin;
        if (!plugin.getConfig().getBoolean("spawns.post-auth.login-teleport", false)) return origin;
        if (plugin.getConfig().getBoolean("spawns.post-auth.use-last-logout-location", true) && savedLocation != null) return savedLocation(savedLocation);
        return loginFallbackLocation();
    }
    private Location limboLocation() {
        World world = plugin.limboWorld();
        int x = plugin.getConfig().getInt("limbo.x", 0);
        int y = plugin.getConfig().getInt("limbo.y", 128);
        int z = plugin.getConfig().getInt("limbo.z", 0);
        if (plugin.getConfig().getBoolean("limbo.bedrock-platform", true)) world.getBlockAt(x, y - 1, z).setType(Material.BEDROCK, false);
        return new Location(world, x + 0.5, y, z + 0.5, 0.0F, 0.0F);
    }
    private Location configuredLocation(String path) {
        String worldName = plugin.getConfig().getString(path + ".world", "world");
        var world = plugin.getServer().getWorld(worldName);
        if (world == null) world = plugin.getServer().getWorlds().get(0);
        return new Location(world, plugin.getConfig().getDouble(path + ".x"), plugin.getConfig().getDouble(path + ".y"), plugin.getConfig().getDouble(path + ".z"), (float) plugin.getConfig().getDouble(path + ".yaw"), (float) plugin.getConfig().getDouble(path + ".pitch"));
    }
    private Location savedLocation(Database.SavedLocation location) {
        World world = plugin.getServer().getWorld(location.world());
        if (world == null) return loginFallbackLocation();
        return new Location(world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
    }
}
