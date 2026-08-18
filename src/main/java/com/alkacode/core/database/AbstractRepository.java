package com.alkacode.core.database;

import com.alkacode.core.api.DatabaseProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Base para repositorios de plugins Alka* que usam o AlkaCore - resolve upsert MySQL vs SQLite. */
public abstract class AbstractRepository {
    protected final DatabaseProvider db;

    protected AbstractRepository(DatabaseProvider db) {
        this.db = db;
    }

    /**
     * Monta um `INSERT ... ON CONFLICT/ON DUPLICATE KEY UPDATE` generico. `columns` deve
     * incluir as colunas de chave unica tambem (elas vao pro VALUES normalmente); so
     * `uniqueKeys` e usado pra decidir o que casar no SQLite.
     */
    protected String upsert(String table, String[] columns, String[] uniqueKeys) {
        String columnList = String.join(", ", columns);
        String placeholders = String.join(", ", java.util.Collections.nCopies(columns.length, "?"));

        // Se TODAS as colunas fazem parte da chave única (ex.: tabela de junção
        // player_uuid+perk_id sem colunas extra), não sobra nada pra por no SET/UPDATE -
        // "...DO UPDATE SET " ou "...ON DUPLICATE KEY UPDATE " vazio é SQL inválido em
        // qualquer um dos dois bancos. Nesse caso é só ignorar duplicata mesmo.
        boolean hasNonKeyColumn = false;
        for (String col : columns) {
            if (!contains(uniqueKeys, col)) {
                hasNonKeyColumn = true;
                break;
            }
        }

        StringBuilder sb = new StringBuilder(!hasNonKeyColumn && db.isSQLite() ? "INSERT OR IGNORE INTO " : "INSERT INTO ")
            .append(table).append(" (").append(columnList).append(") VALUES (").append(placeholders).append(")");

        if (!hasNonKeyColumn) {
            if (!db.isSQLite()) {
                // MySQL não tem "INSERT IGNORE" combinável com essa montagem - atualiza a
                // própria chave (no-op) só pra manter a sintaxe do ON DUPLICATE KEY válida.
                sb.append(" ON DUPLICATE KEY UPDATE ").append(uniqueKeys[0]).append(" = ").append(uniqueKeys[0]);
            }
            return sb.toString();
        }

        if (db.isSQLite()) {
            sb.append(" ON CONFLICT(").append(String.join(", ", uniqueKeys)).append(") DO UPDATE SET ");
            appendAssignments(sb, columns, uniqueKeys, "excluded.");
        } else {
            sb.append(" ON DUPLICATE KEY UPDATE ");
            appendAssignments(sb, columns, uniqueKeys, "VALUES(", ")");
        }
        return sb.toString();
    }

    private void appendAssignments(StringBuilder sb, String[] columns, String[] uniqueKeys, String valuePrefix) {
        appendAssignments(sb, columns, uniqueKeys, valuePrefix, "");
    }

    private void appendAssignments(StringBuilder sb, String[] columns, String[] uniqueKeys, String valuePrefix, String valueSuffix) {
        boolean first = true;
        for (String col : columns) {
            if (contains(uniqueKeys, col)) continue;
            if (!first) sb.append(", ");
            sb.append(col).append(" = ").append(valuePrefix).append(col).append(valueSuffix);
            first = false;
        }
    }

    private boolean contains(String[] array, String value) {
        for (String s : array) if (s.equals(value)) return true;
        return false;
    }

    protected void execute(String sql, SQLConsumer<PreparedStatement> binder) throws SQLException {
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.accept(ps);
            ps.executeUpdate();
        }
    }

    /**
     * Roda {@code callback} dentro de uma transacao na mesma conexao: abre, executa,
     * commita. Em qualquer excecao da rollback e relanca, pro chamador decidir o que
     * fazer. Usar quando uma operacao envolve varias escritas que precisam ser
     * atomicas (ex: transferencia que debita e credita - nunca deixar um lado so).
     */
    protected <T> T inTransaction(TransactionCallback<T> callback) throws SQLException {
        try (Connection conn = db.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                T result = callback.accept(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    @FunctionalInterface
    public interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T accept(Connection conn) throws SQLException;
    }
}
