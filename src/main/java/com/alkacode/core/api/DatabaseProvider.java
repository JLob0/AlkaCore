package com.alkacode.core.api;

import java.sql.Connection;
import java.sql.SQLException;

/** Conexao unica compartilhada por todos os plugins Alka* que dependem do AlkaCore. */
public interface DatabaseProvider {
    Connection getConnection() throws SQLException;

    boolean isSQLite();

    boolean isMySQL();

    void close();
}
