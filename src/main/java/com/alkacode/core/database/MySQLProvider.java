package com.alkacode.core.database;

import com.alkacode.core.api.DatabaseProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLProvider implements DatabaseProvider {
    private final HikariDataSource dataSource;

    private MySQLProvider(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Tenta conectar com timeout curto - retorna null (em vez de lancar) se o MySQL nao responder, pra quem chamou cair pro SQLite. */
    public static MySQLProvider tryConnect(ConfigurationSection mysqlSection, java.util.logging.Logger logger) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8",
            mysqlSection.getString("host"),
            mysqlSection.getInt("port"),
            mysqlSection.getString("database")));
        config.setUsername(mysqlSection.getString("username"));
        config.setPassword(mysqlSection.getString("password"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(mysqlSection.getInt("pool-size", 10));
        config.setConnectionTimeout(3000);
        config.setInitializationFailTimeout(3000);
        try {
            HikariDataSource testSource = new HikariDataSource(config);
            try (Connection conn = testSource.getConnection()) {
                if (!conn.isValid(2)) {
                    testSource.close();
                    return null;
                }
            }
            logger.info("[AlkaCore] MySQL conectado com sucesso!");
            return new MySQLProvider(testSource);
        } catch (Exception e) {
            logger.warning("[AlkaCore] Falha ao conectar no MySQL: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public boolean isSQLite() { return false; }

    @Override
    public boolean isMySQL() { return true; }

    @Override
    public void close() {
        if (!dataSource.isClosed()) dataSource.close();
    }
}
