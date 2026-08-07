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

        StringBuilder sb = new StringBuilder("INSERT INTO ").append(table)
            .append(" (").append(columnList).append(") VALUES (").append(placeholders).append(")");

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

    @FunctionalInterface
    public interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
