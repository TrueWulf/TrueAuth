package ru.truwlf.trueauth;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

final class Database implements AutoCloseable {
    private final HikariDataSource source;
    private final boolean mysql;

    Database(FileConfiguration config, File dataFolder) throws SQLException {
        HikariConfig hikari = new HikariConfig();
        mysql = config.getString("database.type", "SQLITE").equalsIgnoreCase("MYSQL");
        if (mysql) {
            String sslMode = config.getBoolean("database.mysql.use-ssl", true) ? "VERIFY_IDENTITY" : "DISABLED";
            hikari.setJdbcUrl("jdbc:mysql://" + config.getString("database.mysql.host") + ":" + config.getInt("database.mysql.port") + "/" + config.getString("database.mysql.database") + "?sslMode=" + sslMode + "&allowPublicKeyRetrieval=false&serverTimezone=UTC");
            hikari.setUsername(config.getString("database.mysql.username"));
            hikari.setPassword(config.getString("database.mysql.password"));
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikari.setMaximumPoolSize(8);
        } else {
            hikari.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "trueauth.db").getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setMaximumPoolSize(1);
        }
        hikari.setPoolName("TrueAuth-Database");
        source = new HikariDataSource(hikari);
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS trueauth_users (uuid VARCHAR(36) PRIMARY KEY, password_hash VARCHAR(100) NOT NULL)")) {
            statement.execute();
        }
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS trueauth_locations (uuid VARCHAR(36) PRIMARY KEY, world VARCHAR(255) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL)")) {
            statement.execute();
        }
    }

    Optional<String> passwordHash(UUID uuid) throws SQLException {
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT password_hash FROM trueauth_users WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(result.getString(1)) : Optional.empty();
            }
        }
    }

    void create(UUID uuid, String hash) throws SQLException {
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO trueauth_users (uuid, password_hash) VALUES (?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, hash);
            statement.executeUpdate();
        }
    }

    void update(UUID uuid, String hash) throws SQLException {
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE trueauth_users SET password_hash = ? WHERE uuid = ?")) {
            statement.setString(1, hash);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }

    void delete(UUID uuid) throws SQLException {
        try (Connection connection = source.getConnection(); PreparedStatement user = connection.prepareStatement("DELETE FROM trueauth_users WHERE uuid = ?"); PreparedStatement location = connection.prepareStatement("DELETE FROM trueauth_locations WHERE uuid = ?")) {
            connection.setAutoCommit(false);
            try {
                user.setString(1, uuid.toString());
                location.setString(1, uuid.toString());
                user.executeUpdate();
                location.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    Optional<SavedLocation> lastLocation(UUID uuid) throws SQLException {
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT world, x, y, z, yaw, pitch FROM trueauth_locations WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new SavedLocation(result.getString("world"), result.getDouble("x"), result.getDouble("y"), result.getDouble("z"), result.getFloat("yaw"), result.getFloat("pitch")));
            }
        }
    }

    void saveLastLocation(UUID uuid, SavedLocation location) throws SQLException {
        String query = mysql
                ? "INSERT INTO trueauth_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)"
                : "INSERT INTO trueauth_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch";
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, location.world());
            statement.setDouble(3, location.x());
            statement.setDouble(4, location.y());
            statement.setDouble(5, location.z());
            statement.setFloat(6, location.yaw());
            statement.setFloat(7, location.pitch());
            statement.executeUpdate();
        }
    }

    record SavedLocation(String world, double x, double y, double z, float yaw, float pitch) { }

    @Override public void close() { source.close(); }
}
