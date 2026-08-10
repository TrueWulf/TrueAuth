package ru.truwlf.trueauth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TrueAuthCommand implements CommandExecutor, TabCompleter {
    private final TrueAuthPlugin plugin;

    TrueAuthCommand(TrueAuthPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) return help(sender);
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("reload")) {
            if (!sender.hasPermission("trueauth.admin.reload")) return denied(sender);
            plugin.reloadPluginConfig();
            sender.sendMessage(plugin.locale().message("admin-reloaded"));
            return true;
        }
        if (subcommand.equals("setregistration") || subcommand.equals("setloginfallback")) {
            if (!sender.hasPermission("trueauth.admin.spawn")) return denied(sender);
            if (!(sender instanceof Player player)) { sender.sendMessage(plugin.locale().message("player-only")); return true; }
            String path = subcommand.equals("setregistration") ? "spawns.registration" : "spawns.login-fallback";
            var location = player.getLocation();
            plugin.getConfig().set(path + ".world", location.getWorld().getName());
            plugin.getConfig().set(path + ".x", location.getX());
            plugin.getConfig().set(path + ".y", location.getY());
            plugin.getConfig().set(path + ".z", location.getZ());
            plugin.getConfig().set(path + ".yaw", location.getYaw());
            plugin.getConfig().set(path + ".pitch", location.getPitch());
            plugin.saveConfig();
            sender.sendMessage(plugin.locale().message("admin-spawn-set"));
            return true;
        }
        if (!sender.hasPermission("trueauth.admin.account")) return denied(sender);
        if (args.length < 2) return help(sender);
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(plugin.locale().message("player-not-found")); return true; }
        if (subcommand.equals("status")) return status(sender, target);
        if (subcommand.equals("unregister")) return unregister(sender, target);
        if (subcommand.equals("resetpassword")) {
            if (args.length != 3) { sender.sendMessage(plugin.locale().message("usage-reset-password")); return true; }
            int min = plugin.getConfig().getInt("auth.min-password-length", 6);
            int max = plugin.getConfig().getInt("auth.max-password-length", 32);
            if (args[2].length() < min || args[2].length() > max) {
                sender.sendMessage(plugin.locale().message("password-length", java.util.Map.of("min", String.valueOf(min), "max", String.valueOf(max))));
                return true;
            }
            return resetPassword(sender, target, args[2]);
        }
        return help(sender);
    }

    private boolean status(CommandSender sender, Player target) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean registered = plugin.database().passwordHash(target.getUniqueId()).isPresent();
                String location = plugin.database().lastLocation(target.getUniqueId()).map(value -> value.world() + " " + String.format(Locale.ROOT, "%.1f %.1f %.1f", value.x(), value.y(), value.z())).orElse("-");
                plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(plugin.locale().message("admin-status", java.util.Map.of("player", target.getName(), "account", registered ? "registered" : "not registered", "auth", plugin.auth().isAuthenticated(target) ? "authenticated" : "waiting", "location", location))));
            } catch (SQLException exception) { databaseError(sender); }
        });
        return true;
    }

    private boolean unregister(CommandSender sender, Player target) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.database().delete(target.getUniqueId());
                plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(plugin.locale().message("admin-unregistered", java.util.Map.of("player", target.getName()))));
            } catch (SQLException exception) { databaseError(sender); }
        });
        return true;
    }

    private boolean resetPassword(CommandSender sender, Player target, String password) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (plugin.database().passwordHash(target.getUniqueId()).isEmpty()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(plugin.locale().message("not-registered")));
                    return;
                }
                plugin.database().update(target.getUniqueId(), BCrypt.hashpw(password, BCrypt.gensalt(12)));
                plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(plugin.locale().message("admin-password-reset", java.util.Map.of("player", target.getName()))));
            } catch (SQLException exception) { databaseError(sender); }
        });
        return true;
    }

    private boolean help(CommandSender sender) { sender.sendMessage(plugin.locale().message("admin-help")); return true; }
    private boolean denied(CommandSender sender) { sender.sendMessage(plugin.locale().message("no-permission")); return true; }
    private void databaseError(CommandSender sender) { plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(plugin.locale().message("database-error"))); }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of("help"));
            if (sender.hasPermission("trueauth.admin.reload")) commands.add("reload");
            if (sender.hasPermission("trueauth.admin.account")) commands.addAll(List.of("status", "unregister", "resetpassword"));
            if (sender.hasPermission("trueauth.admin.spawn")) commands.addAll(List.of("setregistration", "setloginfallback"));
            return filter(args[0], commands);
        }
        if (args.length == 2 && sender.hasPermission("trueauth.admin.account") && List.of("status", "unregister", "resetpassword").contains(args[0].toLowerCase(Locale.ROOT))) return filter(args[1], plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
        return List.of();
    }

    private List<String> filter(String input, List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.regionMatches(true, 0, input, 0, input.length())) result.add(value);
        return result;
    }
}
