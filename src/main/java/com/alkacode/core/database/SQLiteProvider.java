package com.alkacode.core.database;

import com.alkacode.core.api.DatabaseProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class SQLiteProvider implements DatabaseProvider {
    private final HikariDataSource dataSource;

    public SQLiteProvider(File dataFolder, String fileName) {
        dataFolder.mkdirs();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, fileName).getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        // SQLite so aceita um writer por vez - pool > 1 so causa "database is locked".
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000);
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public boolean isSQLite() { return true; }

    @Override
    public boolean isMySQL() { return false; }

    @Override
    public void close() {
        if (!dataSource.isClosed()) dataSource.close();
    }
}
