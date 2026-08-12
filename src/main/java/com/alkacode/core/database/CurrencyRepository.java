package com.alkacode.core.database;

import com.alkacode.core.api.CurrencyAPI;
import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.scheduler.AlkaScheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class CurrencyRepository extends AbstractRepository implements CurrencyAPI {
    private static final Logger LOGGER = Logger.getLogger("AlkaCore");

    private final AlkaScheduler scheduler;

    public CurrencyRepository(DatabaseProvider db, AlkaScheduler scheduler) {
        super(db);
        this.scheduler = scheduler;
        SchemaMigrator migrator = new SchemaMigrator(db, LOGGER);
        migrator.register("currencies-table", 1, conn -> {
            try (var stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS alkacore_currencies (" +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "currency_id VARCHAR(64) NOT NULL," +
                    "balance INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (player_uuid, currency_id))");
            }
        });
    }

    @Override
    public int getBalance(UUID uuid, String currencyId) {
        String sql = "SELECT balance FROM alkacore_currencies WHERE player_uuid = ? AND currency_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, currencyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("balance");
            }
        } catch (SQLException e) {
            LOGGER.severe("Erro ao ler saldo (" + currencyId + "): " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void setBalance(UUID uuid, String currencyId, int amount) {
        String sql = upsert("alkacode_currencies",
            new String[]{"player_uuid", "currency_id", "balance"},
            new String[]{"player_uuid", "currency_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, currencyId);
                ps.setInt(3, Math.max(0, amount));
            });
        } catch (SQLException e) {
            LOGGER.severe("Erro ao salvar saldo (" + currencyId + "): " + e.getMessage());
        }
    }

    @Override
    public void add(UUID uuid, String currencyId, int amount) {
        if (amount <= 0) return;
        setBalance(uuid, currencyId, getBalance(uuid, currencyId) + amount);
    }

    @Override
    public boolean remove(UUID uuid, String currencyId, int amount) {
        if (amount <= 0) return true;
        int current = getBalance(uuid, currencyId);
        if (current < amount) return false;
        setBalance(uuid, currencyId, current - amount);
        return true;
    }

    @Override
    public CompletableFuture<Integer> getBalanceAsync(UUID uuid, String currencyId) {
        return CompletableFuture.supplyAsync(() -> getBalance(uuid, currencyId), asyncExecutor());
    }

    @Override
    public CompletableFuture<Void> addAsync(UUID uuid, String currencyId, int amount) {
        return CompletableFuture.runAsync(() -> add(uuid, currencyId, amount), asyncExecutor());
    }

    @Override
    public CompletableFuture<Boolean> removeAsync(UUID uuid, String currencyId, int amount) {
        return CompletableFuture.supplyAsync(() -> remove(uuid, currencyId, amount), asyncExecutor());
    }

    private java.util.concurrent.Executor asyncExecutor() {
        return task -> scheduler.runAsync(task);
    }
}
