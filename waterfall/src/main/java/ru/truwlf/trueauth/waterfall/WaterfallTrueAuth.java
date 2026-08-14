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
import ru.truwlf.trueauth.proxy.ProxyAuthState;
import ru.truwlf.trueauth.proxy.ProxyPasswordPolicy;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public final class WaterfallTrueAuth extends Plugin implements Listener {
    private enum AuthType { REGISTER, LOGIN, CHANGE_PASSWORD }
    private static final long AUTH_TIMEOUT_SECONDS = 60;
    private static final long AUTH_COOLDOWN_MILLIS = 1000;
    private static final int MAX_ATTEMPTS = 5;
    private final ProxyAuthState auth = new ProxyAuthState();
    private final Map<UUID, ScheduledTask> timeouts = new ConcurrentHashMap<>();
    private ProxyDatabase database;

    @Override
    public void onEnable() {
        try {
            database = new ProxyDatabase(Paths.get(getDataFolder().toURI()));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize TrueAuth database", exception);
        }
        ProxyServer.getInstance().getPluginManager().registerListener(this, this);
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new AuthCommand("register", AuthType.REGISTER, "reg"));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new AuthCommand("login", AuthType.LOGIN, "l"));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new AuthCommand("changepassword", AuthType.CHANGE_PASSWORD, "cp"));
        getLogger().info("TrueAuth Waterfall adapter enabled");
    }

    @Override
    public void onDisable() {
        timeouts.values().forEach(ScheduledTask::cancel);
        timeouts.clear();
        if (database != null) database.close();
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        UUID session = auth.open(id);
        prompt(event.getPlayer());
        ScheduledTask timeout = ProxyServer.getInstance().getScheduler().schedule(this, () -> {
            if (!auth.current(id, session) || auth.authenticated(id, session)) return;
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(id);
            if (player != null) player.disconnect(new TextComponent("[TrueAuth] Authentication timed out."));
        }, AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        timeouts.put(id, timeout);
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (ProxyServer.getInstance().getPlayer(id) != null && ProxyServer.getInstance().getPlayer(id) != event.getPlayer()) return;
        ScheduledTask timeout = timeouts.remove(id);
        if (timeout != null) timeout.cancel();
        auth.remove(id);
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
        if (name.startsWith("/")) name = name.substring(1);
        return name.equals("register") || name.equals("reg") || name.equals("login") || name.equals("l") || name.equals("changepassword") || name.equals("cp");
    }

    private boolean isAuthenticated(ProxiedPlayer player) { return auth.authenticated(player.getUniqueId()); }
    private void prompt(ProxiedPlayer player) { send(player, "Use /login <password> or /register <password> <password>."); }
    private void send(ProxiedPlayer player, String text) { player.sendMessage(new TextComponent(ChatColor.GRAY + "[TrueAuth] " + text)); }

    private void connectToBackend(ProxiedPlayer player) {
        net.md_5.bungee.api.config.ServerInfo server = player.getReconnectServer();
        if (server == null && !ProxyServer.getInstance().getServers().isEmpty()) server = ProxyServer.getInstance().getServers().values().iterator().next();
        if (server != null) player.connect(server);
    }

    private void register(ProxiedPlayer player, String password, String confirmation) {
        UUID id = player.getUniqueId();
        UUID session = auth.session(id);
        if (!password.equals(confirmation)) { send(player, "Passwords do not match."); return; }
        if (!ProxyPasswordPolicy.valid(password)) { send(player, "Password must contain 6-32 characters and be at most 72 bytes."); return; }
        if (!beginAttempt(player, session)) return;
        runDatabase(player, () -> {
            if (!auth.current(id, session)) return;
            if (database.registered(id)) send(player, "You are already registered.");
            else { database.register(id, password); if (auth.authenticate(id, session)) { send(player, "Registration successful."); connectToBackend(player); } }
        }, id, session);
    }

    private void login(ProxiedPlayer player, String password) {
        UUID id = player.getUniqueId();
        UUID session = auth.session(id);
        if (!ProxyPasswordPolicy.valid(password) || !beginAttempt(player, session)) return;
        runDatabase(player, () -> {
            if (!auth.current(id, session)) return;
            if (database.authenticate(id, password)) { if (auth.authenticate(id, session)) { send(player, "Login successful."); connectToBackend(player); } }
            else send(player, "Invalid password.");
        }, id, session);
    }

    private void changePassword(ProxiedPlayer player, String oldPassword, String newPassword) {
        UUID id = player.getUniqueId();
        UUID session = auth.session(id);
        if (!isAuthenticated(player) || !ProxyPasswordPolicy.valid(oldPassword) || !ProxyPasswordPolicy.valid(newPassword) || !beginAttempt(player, session)) return;
        runDatabase(player, () -> send(player, database.changePassword(id, oldPassword, newPassword) ? "Password changed." : "Invalid current password."), id, session);
    }

    private boolean beginAttempt(ProxiedPlayer player, UUID session) {
        if (session != null && auth.beginAttempt(player.getUniqueId(), session, System.currentTimeMillis(), AUTH_COOLDOWN_MILLIS, MAX_ATTEMPTS)) return true;
        send(player, "Please wait before trying again or reconnect.");
        return false;
    }

    private void runDatabase(ProxiedPlayer player, DatabaseAction action, UUID id, UUID session) {
        CompletableFuture.runAsync(() -> {
            try { action.run(); }
            catch (SQLException exception) { getLogger().severe("Database operation failed: " + exception.getMessage()); send(player, "Authentication storage is temporarily unavailable."); }
            finally { auth.finishAttempt(id, session); }
        });
    }

    private interface DatabaseAction { void run() throws SQLException; }

    private final class AuthCommand extends Command {
        private final AuthType type;
        AuthCommand(String name, AuthType type, String... aliases) { super(name, null, aliases); this.type = type; }
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) { sender.sendMessage(new TextComponent("Only players can use this command.")); return; }
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (type == AuthType.REGISTER && args.length == 2) register(player, args[0], args[1]);
            else if (type == AuthType.LOGIN && args.length == 1) login(player, args[0]);
            else if (type == AuthType.CHANGE_PASSWORD && args.length == 2) changePassword(player, args[0], args[1]);
            else send(player, "Invalid command usage.");
        }
    }
}
