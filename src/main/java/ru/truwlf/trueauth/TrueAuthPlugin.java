package ru.truwlf.trueauth;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.GameRule;
import org.bukkit.generator.ChunkGenerator;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Random;

public final class TrueAuthPlugin extends JavaPlugin {
    private Database database;
    private LocaleManager locale;
    private AuthManager auth;
    private World limboWorld;

    @Override public void onEnable() {
        saveDefaultConfig();
        validateConfig();
        saveResource("lang/ru_RU.yml", false);
        saveResource("lang/en_US.yml", false);
        saveResource("lang/de_DE.yml", false);
        saveResource("lang/fr_FR.yml", false);
        saveResource("lang/it_IT.yml", false);
        saveResource("lang/es_ES.yml", false);
        saveResource("lang/pt_BR.yml", false);
        locale = new LocaleManager(getDataFolder(), getConfig().getString("lang", "en_US"));
        try {
            database = new Database(getConfig(), getDataFolder());
        } catch (SQLException exception) {
            getLogger().severe("Could not initialize database: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        auth = new AuthManager(this);
        limboWorld = createLimboWorld();
        auth.startPrompts();
        getServer().getPluginManager().registerEvents(new LimboListener(this), this);
        AuthCommands authCommands = new AuthCommands(this);
        Objects.requireNonNull(getCommand("register")).setExecutor(authCommands);
        Objects.requireNonNull(getCommand("login")).setExecutor(authCommands);
        Objects.requireNonNull(getCommand("changepassword")).setExecutor(authCommands);
        TrueAuthCommand adminCommand = new TrueAuthCommand(this);
        Objects.requireNonNull(getCommand("trueauth")).setExecutor(adminCommand);
        Objects.requireNonNull(getCommand("trueauth")).setTabCompleter(adminCommand);
    }
    @Override public void onDisable() {
        if (auth != null) auth.saveAuthenticatedLocations();
        if (auth != null) auth.restoreAll();
        if (database != null) database.close();
    }
    Database database() { return database; }
    LocaleManager locale() { return locale; }
    AuthManager auth() { return auth; }
    World limboWorld() { return limboWorld; }
    void reloadPluginConfig() {
        reloadConfig();
        validateConfig();
        locale = new LocaleManager(getDataFolder(), getConfig().getString("lang", "en_US"));
    }

    private void validateConfig() {
        int min = Math.max(1, getConfig().getInt("auth.min-password-length", 6));
        int max = Math.max(min, getConfig().getInt("auth.max-password-length", 32));
        boolean changed = getConfig().getInt("auth.min-password-length", 6) != min || getConfig().getInt("auth.max-password-length", 32) != max;
        getConfig().set("auth.min-password-length", min);
        getConfig().set("auth.max-password-length", max);
        if (getConfig().getLong("auth.command-cooldown-millis", 1000) < 0) {
            getConfig().set("auth.command-cooldown-millis", 0);
            changed = true;
        }
        int timeout = getConfig().getInt("auth.login-timeout-seconds", 60);
        int safeTimeout = Math.min(Math.max(0, timeout), 86400);
        if (timeout != safeTimeout) {
            getConfig().set("auth.login-timeout-seconds", safeTimeout);
            changed = true;
        }
        if (getConfig().getInt("spawns.random-respawn.radius", 300) < 1) {
            getConfig().set("spawns.random-respawn.radius", 1);
            changed = true;
        }
        if (changed) saveConfig();
    }

    private World createLimboWorld() {
        String name = getConfig().getString("limbo.world", "trueauth_limbo");
        World world = getServer().getWorld(name);
        if (world != null && getServer().getWorlds().get(0).equals(world)) {
            throw new IllegalStateException("The limbo world must not be the server's primary world");
        }
        if (world == null) world = new WorldCreator(name).generator(new VoidGenerator()).generateStructures(false).createWorld();
        if (world == null) throw new IllegalStateException("Could not create limbo world");
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setStorm(false);
        world.setThundering(false);
        world.setTime(18000L);
        return world;
    }

    private static final class VoidGenerator extends ChunkGenerator {
        @Override public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
            return createChunkData(world);
        }
    }
}
