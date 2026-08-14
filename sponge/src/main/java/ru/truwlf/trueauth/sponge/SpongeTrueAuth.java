package ru.truwlf.trueauth.sponge;

import com.google.inject.Inject;
import org.mindrot.jbcrypt.BCrypt;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import org.spongepowered.plugin.PluginContainer;

import net.kyori.adventure.text.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin("trueauth")
public final class SpongeTrueAuth {
    private static final Component AUTH_REQUIRED = Component.text("You must log in before playing.");
    private static final Component REGISTER_PROMPT = Component.text("Use /register <password> to create an account.");
    private static final Component LOGIN_PROMPT = Component.text("Use /login <password> to authenticate.");

    private final PluginContainer plugin;
    private final Path configDirectory;
    private final Map<UUID, ScheduledTask> timeouts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> authenticated = new ConcurrentHashMap<>();
    private Connection database;

    @Inject
    public SpongeTrueAuth(PluginContainer plugin, @ConfigDir(sharedRoot = false) Path configDirectory) {
        this.plugin = plugin;
        this.configDirectory = configDirectory;
    }

    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Command.Parameterized> event) {
        Parameter.Value<String> password = Parameter.remainingJoinedStrings().key("password").build();
        event.register(plugin, command("register", password, this::register), "register", "reg");
        event.register(plugin, command("login", password, this::login), "login", "l");
    }

    @Listener
    public void onJoin(ServerSideConnectionEvent.Join event) {
        ServerPlayer player = event.player();
        UUID id = player.uniqueId();
        authenticated.put(id, false);
        boolean accountExists = accountExists(id);
        player.sendMessage(accountExists ? LOGIN_PROMPT : REGISTER_PROMPT);
        long timeoutSeconds = 60;
        ScheduledTask timeout = Sponge.server().scheduler().submit(Task.builder()
                .plugin(plugin)
                .delay(Duration.ofSeconds(timeoutSeconds))
                .execute(task -> {
                    if (!isAuthenticated(id)) player.kick(Component.text("Authentication timed out."));
                })
                .build());
        timeouts.put(id, timeout);
    }

    @Listener
    public void onDisconnect(ServerSideConnectionEvent.Disconnect event) {
        UUID id = event.player().uniqueId();
        authenticated.remove(id);
        cancelTimeout(id);
    }

    @Listener
    public void onChat(PlayerChatEvent event) {
        player(event).ifPresent(player -> {
            if (!isAuthenticated(player.uniqueId())) event.setCancelled(true);
        });
    }

    @Listener
    public void onCommand(ExecuteCommandEvent.Pre event) {
        event.commandCause().first(ServerPlayer.class).ifPresent(player -> {
            if (isAuthenticated(player.uniqueId())) return;
            String command = event.command().toLowerCase();
            if (!command.equals("login") && !command.equals("l") && !command.equals("register") && !command.equals("reg")) {
                event.setCancelled(true);
                player.sendMessage(AUTH_REQUIRED);
            }
        });
    }

    @Listener
    public void onMove(MoveEntityEvent event) {
        if (event.entity() instanceof ServerPlayer player && !isAuthenticated(player.uniqueId())) event.setCancelled(true);
    }

    @Listener
    public void onDamage(DamageEntityEvent event) {
        if (event.entity() instanceof ServerPlayer player && !isAuthenticated(player.uniqueId())) event.setCancelled(true);
    }

    @Listener
    public void onInteract(InteractEntityEvent event) {
        player(event).ifPresent(player -> {
            if (!isAuthenticated(player.uniqueId())) event.setCancelled(true);
        });
    }

    @Listener
    public void onContainerClick(ClickContainerEvent event) {
        player(event).ifPresent(player -> {
            if (!isAuthenticated(player.uniqueId())) event.setCancelled(true);
        });
    }

    private Command.Parameterized command(String name, Parameter.Value<String> password, CommandExecutor executor) {
        return Command.builder()
                .addParameter(password)
                .shortDescription(Component.text("TrueAuth " + name))
                .executor(context -> executor.execute(context, context.requireOne(password.key())))
                .build();
    }

    private CommandResult register(CommandContext context, String password) {
        return player(context, "Only players can register.").map(player -> {
            UUID id = player.uniqueId();
            if (isAuthenticated(id)) return error("You are already authenticated.");
            if (password.length() < 6 || password.length() > 72) return error("Password length must be 6-72 characters.");
            if (accountExists(id)) return error("An account already exists. Use /login.");
            saveAccount(id, BCrypt.hashpw(password, BCrypt.gensalt(12)));
            authenticate(player);
            return success(player, "Registration complete.");
        }).orElseGet(() -> error("Only players can register."));
    }

    private CommandResult login(CommandContext context, String password) {
        return player(context, "Only players can log in.").map(player -> {
            UUID id = player.uniqueId();
            if (isAuthenticated(id)) return error("You are already authenticated.");
            String hash = accountHash(id);
            if (hash == null || !BCrypt.checkpw(password, hash)) return error("Invalid password.");
            authenticate(player);
            return success(player, "Login successful.");
        }).orElseGet(() -> error("Only players can log in."));
    }

    private void authenticate(ServerPlayer player) {
        UUID id = player.uniqueId();
        authenticated.put(id, true);
        cancelTimeout(id);
        player.sendMessage(Component.text("Authentication successful."));
    }

    private boolean isAuthenticated(UUID id) { return authenticated.getOrDefault(id, false); }

    private void cancelTimeout(UUID id) {
        ScheduledTask task = timeouts.remove(id);
        if (task != null) task.cancel();
    }

    private boolean accountExists(UUID id) {
        return accountHash(id) != null;
    }

    private String accountHash(UUID id) {
        try (PreparedStatement statement = database().prepareStatement("SELECT password FROM accounts WHERE uuid = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getString(1) : null; }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read TrueAuth database", exception);
        }
    }

    private void saveAccount(UUID id, String password) {
        try (PreparedStatement statement = database().prepareStatement("INSERT INTO accounts(uuid, password) VALUES(?, ?)")) {
            statement.setString(1, id.toString());
            statement.setString(2, password);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save TrueAuth account", exception);
        }
    }

    private Connection database() {
        if (database != null) return database;
        try {
            Files.createDirectories(configDirectory);
            database = DriverManager.getConnection("jdbc:sqlite:" + configDirectory.resolve("trueauth.db"));
            try (var statement = database.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (uuid TEXT PRIMARY KEY, password TEXT NOT NULL)");
            }
            return database;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not open TrueAuth database", exception);
        }
    }

    private static CommandResult success(ServerPlayer player, String message) {
        player.sendMessage(Component.text(message));
        return CommandResult.success();
    }
    private static CommandResult error(String message) { return CommandResult.error(Component.text(message)); }

    private static java.util.Optional<ServerPlayer> player(CommandContext context, String ignored) {
        return context.cause().first(ServerPlayer.class);
    }

    private static java.util.Optional<ServerPlayer> player(CommandContext context) {
        return context.cause().first(ServerPlayer.class);
    }

    private static java.util.Optional<ServerPlayer> player(org.spongepowered.api.event.Event event) {
        return event.cause().first(ServerPlayer.class);
    }

    private interface CommandExecutor {
        CommandResult execute(CommandContext context, String password);
    }
}
