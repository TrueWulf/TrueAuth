package ru.truwlf.trueauth.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import ru.truwlf.trueauth.proxy.ProxyDatabase;

import com.google.inject.Inject;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.velocitypowered.api.scheduler.ScheduledTask;
import ru.truwlf.trueauth.proxy.ProxyAuthState;
import ru.truwlf.trueauth.proxy.ProxyPasswordPolicy;

@Plugin(id = "trueauth", name = "TrueAuth", version = "2.3.0", description = "Lightweight authentication for Velocity proxies.", authors = {"TrueWulf"})
public final class VelocityTrueAuth {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private static final long AUTH_TIMEOUT_SECONDS = 60;
    private static final long AUTH_COOLDOWN_MILLIS = 1000;
    private static final int MAX_ATTEMPTS = 5;
    private final ProxyAuthState auth = new ProxyAuthState();
    private final Map<UUID, ScheduledTask> timeouts = new ConcurrentHashMap<>();
    private ProxyDatabase database;

    @Inject
    public VelocityTrueAuth(ProxyServer proxy, Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            database = new ProxyDatabase(dataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize TrueAuth database", exception);
        }
        register("register", new AuthCommand(this, AuthCommand.Type.REGISTER));
        register("login", new AuthCommand(this, AuthCommand.Type.LOGIN));
        register("changepassword", new AuthCommand(this, AuthCommand.Type.CHANGE_PASSWORD));
        logger.info("TrueAuth Velocity adapter enabled");
    }

    private void register(String name, SimpleCommand command) {
        proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder(name).aliases(name.equals("register") ? "reg" : name.equals("login") ? "l" : "cp").build(), command);
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        UUID session = auth.open(id);
        ScheduledTask timeout = proxy.getScheduler().buildTask(this, () -> {
            if (!auth.current(id, session) || auth.authenticated(id, session)) return;
            proxy.getPlayer(id).ifPresent(player -> player.disconnect(message("Authentication timed out.")));
        }).delay(Duration.ofSeconds(AUTH_TIMEOUT_SECONDS)).schedule();
        timeouts.put(id, timeout);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        prompt(player);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (proxy.getPlayer(id).filter(current -> current != event.getPlayer()).isPresent()) return;
        ScheduledTask timeout = timeouts.remove(id);
        if (timeout != null) timeout.cancel();
        auth.remove(id);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        timeouts.values().forEach(ScheduledTask::cancel);
        timeouts.clear();
        if (database != null) database.close();
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            prompt(event.getPlayer());
        }
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            event.getPlayer().sendMessage(message("Please login or register first."));
        }
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (event.getCommandSource() instanceof Player player && !isAuthenticated(player) && !isAuthCommand(event.getCommand())) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            prompt(player);
        }
    }

    private boolean isAuthCommand(String command) {
        String name = command.toLowerCase(java.util.Locale.ROOT).split(" ", 2)[0];
        if (name.startsWith("/")) name = name.substring(1);
        return name.equals("register") || name.equals("reg") || name.equals("login") || name.equals("l") || name.equals("changepassword") || name.equals("cp");
    }

    boolean isAuthenticated(Player player) { return auth.authenticated(player.getUniqueId()); }

    void register(Player player, String password, String confirmation) {
        UUID id = player.getUniqueId();
        UUID session = auth.session(id);
        if (!ProxyPasswordPolicy.valid(password) || !password.equals(confirmation)) {
            send(player, password.equals(confirmation) ? "Password must contain 6-32 characters and be at most 72 bytes." : "Passwords do not match.");
            return;
        }
        if (!beginAttempt(player, session)) return;
        runDatabase(player, () -> {
            if (!auth.current(id, session)) return;
            if (database.registered(id)) {
                send(player, "You are already registered.");
            } else {
                database.register(id, password);
                if (auth.authenticate(id, session)) {
                    send(player, "Registration successful.");
                    connectToBackend(player);
                }
            }
        }, id, session);
    }

    void login(Player player, String password) {
        UUID id = player.getUniqueId();
        UUID session = auth.session(id);
        if (!ProxyPasswordPolicy.valid(password) || !beginAttempt(player, session)) return;
        runDatabase(player, () -> {
            if (!auth.current(id, session)) return;
            if (!database.authenticate(id, password)) {
                send(player, "Invalid password.");
            } else {
                if (auth.authenticate(id, session)) {
                    send(player, "Login successful.");
                    connectToBackend(player);
                }
            }
        }, id, session);
    }

    void changePassword(Player player, String oldPassword, String newPassword) {
        UUID id = player.getUniqueId();
        UUID session = auth.session(id);
        if (!isAuthenticated(player) || !ProxyPasswordPolicy.valid(oldPassword) || !ProxyPasswordPolicy.valid(newPassword) || session == null || !auth.beginAttempt(id, session, System.currentTimeMillis(), AUTH_COOLDOWN_MILLIS, MAX_ATTEMPTS, true)) return;
        runDatabase(player, () -> send(player, database.changePassword(id, oldPassword, newPassword) ? "Password changed." : "Invalid current password."), id, session);
    }

    private boolean beginAttempt(Player player, UUID session) {
        if (session == null || auth.beginAttempt(player.getUniqueId(), session, System.currentTimeMillis(), AUTH_COOLDOWN_MILLIS, MAX_ATTEMPTS)) return session != null;
        send(player, "Please wait before trying again or reconnect.");
        return false;
    }

    private void runDatabase(Player player, DatabaseAction action, UUID id, UUID session) {
        CompletableFuture.runAsync(() -> {
            try {
                action.run();
            } catch (SQLException exception) {
                logger.error("Database operation failed for {}", player.getUsername(), exception);
                send(player, "Authentication storage is temporarily unavailable.");
            } finally {
                auth.finishAttempt(id, session);
            }
        });
    }

    private void send(Player player, String text) { player.sendMessage(message(text)); }
    private void prompt(Player player) { send(player, "Use /login <password> or /register <password> <password>."); }
    private Component message(String text) { return Component.text("[TrueAuth] " + text); }

    private void connectToBackend(Player player) {
        List<String> order = proxy.getConfiguration().getAttemptConnectionOrder();
        for (String name : order) {
            var server = proxy.getServer(name);
            if (server.isPresent()) {
                player.createConnectionRequest(server.get()).connect();
                return;
            }
        }
        proxy.getAllServers().stream().findFirst().ifPresent(server -> player.createConnectionRequest(server).connect());
    }

    @FunctionalInterface
    private interface DatabaseAction { void run() throws SQLException; }

    static final class AuthCommand implements SimpleCommand {
        enum Type { REGISTER, LOGIN, CHANGE_PASSWORD }
        private final VelocityTrueAuth plugin;
        private final Type type;
        AuthCommand(VelocityTrueAuth plugin, Type type) { this.plugin = plugin; this.type = type; }
        @Override public void execute(Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) { invocation.source().sendMessage(Component.text("Only players can use this command.")); return; }
            String[] args = invocation.arguments();
            if (type == Type.REGISTER && args.length == 2) plugin.register(player, args[0], args[1]);
            else if (type == Type.LOGIN && args.length == 1) plugin.login(player, args[0]);
            else if (type == Type.CHANGE_PASSWORD && args.length == 2) plugin.changePassword(player, args[0], args[1]);
            else player.sendMessage(Component.text("[TrueAuth] Invalid command usage."));
        }
        @Override public java.util.List<String> suggest(Invocation invocation) { return java.util.List.of(); }
    }
}
