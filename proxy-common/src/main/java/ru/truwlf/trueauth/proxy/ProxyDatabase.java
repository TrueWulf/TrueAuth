package ru.truwlf.trueauth.proxy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public final class ProxyDatabase implements AutoCloseable {
    private final HikariDataSource source;
    private final boolean mysql;

    public ProxyDatabase(Path dataDirectory) throws IOException, SQLException {
        Files.createDirectories(dataDirectory);
        Path configFile = dataDirectory.resolve("config.properties");
        Properties config = new Properties();
        if (Files.notExists(configFile)) {
            config.setProperty("database.type", "SQLITE");
            config.setProperty("database.mysql.host", "127.0.0.1");
            config.setProperty("database.mysql.port", "3306");
            config.setProperty("database.mysql.database", "trueauth");
            config.setProperty("database.mysql.username", "trueauth");
            config.setProperty("database.mysql.password", "change-me");
            try (var writer = Files.newBufferedWriter(configFile)) {
                config.store(writer, "TrueAuth proxy configuration");
            }
        } else {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                config.load(reader);
            }
        }

        mysql = "MYSQL".equalsIgnoreCase(config.getProperty("database.type", "SQLITE"));
        HikariConfig hikari = new HikariConfig();
        if (mysql) {
            String host = config.getProperty("database.mysql.host", "127.0.0.1");
            String port = config.getProperty("database.mysql.port", "3306");
            String database = config.getProperty("database.mysql.database", "trueauth");
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=false&serverTimezone=UTC");
            hikari.setUsername(config.getProperty("database.mysql.username", "trueauth"));
            hikari.setPassword(config.getProperty("database.mysql.password", "change-me"));
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikari.setMaximumPoolSize(8);
        } else {
            hikari.setJdbcUrl("jdbc:sqlite:" + dataDirectory.resolve("trueauth.db").toAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setMaximumPoolSize(1);
        }
        hikari.setPoolName("TrueAuth-Proxy-Database");
        source = new HikariDataSource(hikari);
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS trueauth_users (uuid VARCHAR(36) PRIMARY KEY, password_hash VARCHAR(100) NOT NULL)")) {
            statement.executeUpdate();
        }
    }

    public boolean registered(UUID uuid) throws SQLException {
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM trueauth_users WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean authenticate(UUID uuid, String password) throws SQLException {
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT password_hash FROM trueauth_users WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && BCrypt.checkpw(password, result.getString(1));
            }
        }
    }

    public void register(UUID uuid, String password) throws SQLException {
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO trueauth_users (uuid, password_hash) VALUES (?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, BCrypt.hashpw(password, BCrypt.gensalt(12)));
            statement.executeUpdate();
        }
    }

    public boolean changePassword(UUID uuid, String oldPassword, String newPassword) throws SQLException {
        if (!authenticate(uuid, oldPassword)) return false;
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE trueauth_users SET password_hash = ? WHERE uuid = ?")) {
            statement.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
            return true;
        }
    }

    @Override
    public void close() {
        source.close();
    }
}
