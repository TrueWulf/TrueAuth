package ru.truwlf.trueauth.waterfall;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import ru.truwlf.trueauth.proxy.ProxyDatabase;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class WaterfallTrueAuth extends Plugin implements Listener {
    private enum AuthType { REGISTER, LOGIN, CHANGE_PASSWORD }
    private final Map<UUID, Boolean> authenticated = new ConcurrentHashMap<>();
    private ProxyDatabase database;

    @Override
    public void onEnable() {
        try {
            database = new ProxyDatabase(Paths.get(getDataFolder().toURI()));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize TrueAuth database", exception);
        }
        ProxyServer.getInstance().getPluginManager().registerListener(this, this);
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new AuthCommand("register", AuthType.REGISTER));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new AuthCommand("login", AuthType.LOGIN));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new AuthCommand("changepassword", AuthType.CHANGE_PASSWORD));
        getLogger().info("TrueAuth Waterfall adapter enabled");
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        authenticated.put(event.getPlayer().getUniqueId(), false);
        prompt(event.getPlayer());
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        authenticated.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
            prompt(event.getPlayer());
        }
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) event.getSender();
            if (!isAuthenticated(player) && !isAuthCommand(event.getMessage())) {
                event.setCancelled(true);
                send(player, "Please login or register first.");
            }
        }
    }

    private boolean isAuthCommand(String command) {
        String name = command.toLowerCase(java.util.Locale.ROOT).split(" ", 2)[0];
        return name.equals("/register") || name.equals("/reg") || name.equals("/login") || name.equals("/l") || name.equals("/changepassword") || name.equals("/cp");
    }

    private boolean isAuthenticated(ProxiedPlayer player) { return authenticated.getOrDefault(player.getUniqueId(), false); }
    private void prompt(ProxiedPlayer player) { send(player, "Use /login <password> or /register <password> <password>."); }
    private void send(ProxiedPlayer player, String text) { player.sendMessage(new TextComponent(ChatColor.GRAY + "[TrueAuth] " + text)); }

    private void connectToBackend(ProxiedPlayer player) {
        if (!ProxyServer.getInstance().getServers().isEmpty()) {
            player.connect(ProxyServer.getInstance().getServers().values().iterator().next());
        }
    }

    private void register(ProxiedPlayer player, String password, String confirmation) {
        if (!password.equals(confirmation)) { send(player, "Passwords do not match."); return; }
        runDatabase(player, () -> {
            if (database.registered(player.getUniqueId())) send(player, "You are already registered.");
            else { database.register(player.getUniqueId(), password); authenticated.put(player.getUniqueId(), true); send(player, "Registration successful."); connectToBackend(player); }
        });
    }

    private void login(ProxiedPlayer player, String password) {
        runDatabase(player, () -> {
            if (database.authenticate(player.getUniqueId(), password)) { authenticated.put(player.getUniqueId(), true); send(player, "Login successful."); connectToBackend(player); }
            else send(player, "Invalid password.");
        });
    }

    private void changePassword(ProxiedPlayer player, String oldPassword, String newPassword) {
        runDatabase(player, () -> send(player, database.changePassword(player.getUniqueId(), oldPassword, newPassword) ? "Password changed." : "Invalid current password."));
    }

    private void runDatabase(ProxiedPlayer player, DatabaseAction action) {
        CompletableFuture.runAsync(() -> {
            try { action.run(); }
            catch (SQLException exception) { getLogger().severe("Database operation failed: " + exception.getMessage()); send(player, "Authentication storage is temporarily unavailable."); }
        });
    }

    private interface DatabaseAction { void run() throws SQLException; }

    private final class AuthCommand extends Command {
        private final AuthType type;
        AuthCommand(String name, AuthType type) { super(name); this.type = type; }
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) { sender.sendMessage(new TextComponent("Only players can use this command.")); return; }
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (type == AuthType.REGISTER && args.length >= 2) register(player, args[0], args[1]);
            else if (type == AuthType.LOGIN && args.length >= 1) login(player, args[0]);
            else if (type == AuthType.CHANGE_PASSWORD && args.length >= 2) changePassword(player, args[0], args[1]);
            else send(player, "Invalid command usage.");
        }
    }
}
