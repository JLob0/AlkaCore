package com.alkacode.core.database;

import com.alkacode.core.api.DatabaseProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Versionamento de schema do banco. Mantem uma tabela `alkacode_schema` com a
 * versao atual e aplica migracoes incrementais em transacao (uma unica vez cada,
 * registradas na ordem). Necessario pra rede crescer sem precisar apagar o banco
 * quando uma tabela mudar entre versoes do Core.
 *
 * <p>Migracoes sao registradas no onEnable do AlkaCore (e de repositorios via
 * {@link #register(String, int, Migration)}); cada uma roda so se a versao atual
 * for menor que a versao alvo.
 */
public final class SchemaMigrator {
    private static final String META_TABLE =
        "CREATE TABLE IF NOT EXISTS alkacode_schema (" +
        "id INTEGER PRIMARY KEY CHECK (id = 1), " +
        "version INTEGER NOT NULL DEFAULT 0)";

    private final DatabaseProvider db;
    private final Logger logger;

    public SchemaMigrator(DatabaseProvider db, Logger logger) {
        this.db = db;
        this.logger = logger;
        ensureMetaTable();
    }

    /** Versao atual do schema (0 se nunca migrado). Nunca joga excecao pra fora. */
    public int getVersion() {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT version FROM alkacode_schema WHERE id = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("version") : 0;
            }
        } catch (SQLException e) {
            logger.severe("[AlkaCore] Falha ao ler versao do schema: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Aplica {@code migration} em transacao se a versao atual for menor que
     * {@code targetVersion}, e grava a nova versao. Falha e logada (nao estoura),
     * pra um plugin sozinho nao derrubar o servidor se uma migracao der errado.
     */
    public void register(String migrationName, int targetVersion, Migration migration) {
        if (getVersion() >= targetVersion) return;
        logger.info("[AlkaCore] Aplicando migracao de schema '" + migrationName
            + "' (v" + getVersion() + " -> v" + targetVersion + ")");
        try (Connection conn = db.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                migration.apply(conn);
                upsertVersion(conn, targetVersion);
                conn.commit();
                logger.info("[AlkaCore] Migracao '" + migrationName + "' aplicada.");
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            logger.severe("[AlkaCore] Falha na migracao '" + migrationName + "': " + e.getMessage());
        }
    }

    private void ensureMetaTable() {
        try (Connection conn = db.getConnection(); Statement st = conn.createStatement()) {
            st.execute(META_TABLE);
        } catch (SQLException e) {
            logger.severe("[AlkaCore] Falha ao criar tabela alkacode_schema: " + e.getMessage());
        }
    }

    private void upsertVersion(Connection conn, int version) throws SQLException {
        String sql = db.isSQLite()
            ? "INSERT INTO alkacode_schema (id, version) VALUES (1, ?) " +
              "ON CONFLICT(id) DO UPDATE SET version = excluded.version"
            : "INSERT INTO alkacode_schema (id, version) VALUES (1, ?) " +
              "ON DUPLICATE KEY UPDATE version = VALUES(version)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, version);
            ps.executeUpdate();
        }
    }

    @FunctionalInterface
    public interface Migration {
        void apply(Connection conn) throws SQLException;
    }
}
