package ru.truwlf.trueauth;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class LimboListener implements Listener {
    private final TrueAuthPlugin plugin;
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();
    LimboListener(TrueAuthPlugin plugin) { this.plugin = plugin; }
    private boolean blocked(Player player) { return !plugin.auth().isAuthenticated(player); }
    @EventHandler(priority = EventPriority.HIGHEST) void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.auth().enterLimbo(player);
        plugin.getServer().getOnlinePlayers().stream().filter(other -> !other.equals(player) && !plugin.auth().isAuthenticated(other))
                .forEach(other -> { other.hidePlayer(plugin, player); player.hidePlayer(plugin, other); });
    }
    @EventHandler void quit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        boolean skipSave = plugin.auth().consumeSkipLogoutSave(player);
        boolean authenticated = plugin.auth().isAuthenticated(player);
        if (authenticated && !skipSave) saveLocation(player);
        else if (!authenticated) plugin.auth().clearLocationSaveVersion(player);
        plugin.auth().leave(player);
    }
    @EventHandler(ignoreCancelled = true) void chat(AsyncPlayerChatEvent event) { if (blocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void command(PlayerCommandPreprocessEvent event) {
        if (!blocked(event.getPlayer())) return;
        String command = event.getMessage().split(" ", 2)[0].toLowerCase();
        if (!command.equals("/login") && !command.equals("/log") && !command.equals("/l") && !command.equals("/register") && !command.equals("/reg") && !command.equals("/r")) { event.setCancelled(true); event.getPlayer().sendMessage(plugin.locale().message("auth-required")); }
    }
    @EventHandler(ignoreCancelled = true) void damage(EntityDamageEvent event) { if (event.getEntity() instanceof Player player && blocked(player)) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void damageOther(EntityDamageByEntityEvent event) { if (event.getDamager() instanceof Player player && blocked(player)) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void drop(PlayerDropItemEvent event) { if (blocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void pickup(EntityPickupItemEvent event) { if (event.getEntity() instanceof Player player && blocked(player)) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void inventory(InventoryClickEvent event) { if (event.getWhoClicked() instanceof Player player && blocked(player)) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void inventoryDrag(InventoryDragEvent event) { if (event.getWhoClicked() instanceof Player player && blocked(player)) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void swapHand(PlayerSwapHandItemsEvent event) { if (blocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void interact(PlayerInteractEvent event) { if (blocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void breakBlock(BlockBreakEvent event) { if (blocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void placeBlock(BlockPlaceEvent event) { if (blocked(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) void target(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player && blocked(player) && event.getEntity() instanceof Mob) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true) void move(PlayerMoveEvent event) {
        if (!blocked(event.getPlayer()) || event.getTo() == null) return;
        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ()) event.setTo(event.getFrom());
    }
    @EventHandler void respawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = deathLocations.remove(player.getUniqueId());
        if (!plugin.auth().isAuthenticated(player) || event.isBedSpawn() || event.isAnchorSpawn() || !plugin.getConfig().getBoolean("spawns.random-respawn.enabled", false)) return;
        var world = plugin.getServer().getWorld(plugin.getConfig().getString("spawns.random-respawn.world", "world"));
        if (world == null) return;
        int radius = Math.max(1, plugin.getConfig().getInt("spawns.random-respawn.radius", 300));
        double centerX = plugin.getConfig().getDouble("spawns.random-respawn.center-x", 0.5);
        double centerZ = plugin.getConfig().getDouble("spawns.random-respawn.center-z", 0.5);
        Location location = randomSafeLocation(world, centerX, centerZ, radius);
        if (location == null) return;
        event.setRespawnLocation(location);
        if (deathLocation == null) deathLocation = player.getLocation();
        Location messageLocation = deathLocation;
        plugin.scheduler().run(player, () -> {
            if (player.isOnline()) player.sendMessage(plugin.locale().message("random-respawn", Map.of("x", String.valueOf(messageLocation.getBlockX()), "y", String.valueOf(messageLocation.getBlockY()), "z", String.valueOf(messageLocation.getBlockZ()))));
        });
    }
    @EventHandler void death(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.auth().isAuthenticated(player) && plugin.getConfig().getBoolean("spawns.random-respawn.enabled", false)) deathLocations.put(player.getUniqueId(), player.getLocation().clone());
    }
    private Location randomSafeLocation(org.bukkit.World world, double centerX, double centerZ, int radius) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double distance = ThreadLocalRandom.current().nextDouble(radius / 2.0, radius + 1.0);
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
            int x = (int) Math.floor(centerX + Math.cos(angle) * distance);
            int z = (int) Math.floor(centerZ + Math.sin(angle) * distance);
            var ground = world.getHighestBlockAt(x, z);
            var feet = ground.getRelative(0, 1, 0);
            var head = ground.getRelative(0, 2, 0);
            if (ground.getType().isSolid() && !ground.isLiquid() && feet.isPassable() && head.isPassable()) return feet.getLocation().add(0.5, 0.0, 0.5);
        }
        return null;
    }
    private void saveLocation(Player player) {
        var location = player.getLocation();
        if (location.getWorld() == null || location.getWorld().equals(plugin.limboWorld())) return;
        Database.SavedLocation saved = new Database.SavedLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        long version = plugin.auth().locationSaveVersion(player);
        plugin.scheduler().runAsync(() -> {
            try {
                if (plugin.auth().isCurrentLocationSave(player, version)) plugin.database().saveLastLocation(player.getUniqueId(), saved);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not save logout location for " + player.getName() + ": " + exception.getMessage());
            } finally {
                plugin.auth().finishLocationSave(player, version);
            }
        });
    }
}
