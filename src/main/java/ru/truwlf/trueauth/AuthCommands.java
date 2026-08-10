package ru.truwlf.trueauth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class AuthCommands implements CommandExecutor {
    private final TrueAuthPlugin plugin;
    private final Map<UUID, Long> attempts = new ConcurrentHashMap<>();
    AuthCommands(TrueAuthPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        return switch (command.getName()) {
            case "register" -> register(player, args);
            case "login" -> login(player, args);
            case "changepassword" -> changePassword(player, args);
            default -> false;
        };
    }
    private boolean register(Player player, String[] args) {
        if (plugin.auth().isAuthenticated(player)) { player.sendMessage(plugin.locale().message("already-authenticated")); return true; }
        if (!allowed(player)) return true;
        if (args.length != 2) { player.sendMessage(plugin.locale().message("usage-register")); return true; }
        if (!args[0].equals(args[1])) { player.sendMessage(plugin.locale().message("password-mismatch")); return true; }
        if (!valid(player, args[0])) return true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (plugin.database().passwordHash(player.getUniqueId()).isPresent()) result(player, "already-registered", false);
                else {
                    plugin.database().create(player.getUniqueId(), BCrypt.hashpw(args[0], BCrypt.gensalt(12)));
                    result(player, "register-success", true, true, null);
                }
            } catch (SQLException exception) { plugin.getLogger().warning("Registration database error: " + exception.getMessage()); result(player, "database-error", false); }
        });
        return true;
    }
    private boolean login(Player player, String[] args) {
        if (plugin.auth().isAuthenticated(player)) { player.sendMessage(plugin.locale().message("already-authenticated")); return true; }
        if (!allowed(player)) return true;
        if (args.length != 1) { player.sendMessage(plugin.locale().message("usage-login")); return true; }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var hash = plugin.database().passwordHash(player.getUniqueId());
                if (hash.isEmpty()) result(player, "not-registered", false);
                else if (!BCrypt.checkpw(args[0], hash.get())) result(player, "wrong-password", false);
                else result(player, "login-success", true, false, plugin.database().lastLocation(player.getUniqueId()).orElse(null));
            } catch (SQLException exception) { plugin.getLogger().warning("Login database error: " + exception.getMessage()); result(player, "database-error", false); }
        });
        return true;
    }
    private boolean changePassword(Player player, String[] args) {
        if (!plugin.auth().isAuthenticated(player)) { player.sendMessage(plugin.locale().message("auth-required")); return true; }
        if (args.length != 2) { player.sendMessage(plugin.locale().message("usage-change-password")); return true; }
        if (!valid(player, args[1])) return true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var hash = plugin.database().passwordHash(player.getUniqueId());
                if (hash.isEmpty() || !BCrypt.checkpw(args[0], hash.get())) result(player, "wrong-password", false);
                else { plugin.database().update(player.getUniqueId(), BCrypt.hashpw(args[1], BCrypt.gensalt(12))); result(player, "password-changed", false); }
            } catch (SQLException exception) { plugin.getLogger().warning("Password change database error: " + exception.getMessage()); result(player, "database-error", false); }
        });
        return true;
    }
    private boolean valid(Player player, String password) {
        int min = plugin.getConfig().getInt("auth.min-password-length", 6), max = plugin.getConfig().getInt("auth.max-password-length", 32);
        if (password.length() < min || password.length() > max) { player.sendMessage(plugin.locale().message("password-length", Map.of("min", String.valueOf(min), "max", String.valueOf(max)))); return false; }
        return true;
    }
    private boolean allowed(Player player) {
        long cooldown = Math.max(0, plugin.getConfig().getLong("auth.command-cooldown-millis", 1000));
        long now = System.currentTimeMillis();
        Long previous = attempts.put(player.getUniqueId(), now);
        if (previous == null || now - previous >= cooldown) return true;
        player.sendMessage(plugin.locale().message("command-cooldown", Map.of("seconds", String.format(java.util.Locale.ROOT, "%.1f", cooldown / 1000.0))));
        return false;
    }
    private void result(Player player, String key, boolean authenticate) { result(player, key, authenticate, false, null); }
    private void result(Player player, String key, boolean authenticate, boolean newlyRegistered) { result(player, key, authenticate, newlyRegistered, null); }
    private void result(Player player, String key, boolean authenticate, boolean newlyRegistered, Database.SavedLocation savedLocation) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (authenticate) plugin.auth().authenticate(player, newlyRegistered, savedLocation);
            player.sendMessage(plugin.locale().message(key));
        });
    }
}
