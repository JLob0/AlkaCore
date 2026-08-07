package com.alkacode.core.database;

import com.alkacode.core.api.DatabaseProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Escolhe MySQL (com fallback pra SQLite se a conexao falhar) ou SQLite direto, a partir do config.yml do AlkaCore. */
public final class DatabaseFactory {
    private DatabaseFactory() {}

    public static DatabaseProvider create(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean wantsMysql = config.getBoolean("mysql.enabled", false);
        if (wantsMysql) {
            MySQLProvider provider = MySQLProvider.tryConnect(config.getConfigurationSection("mysql"), plugin.getLogger());
            if (provider != null) return provider;
            plugin.getLogger().warning("[AlkaCore] MySQL indisponivel (servidor nao respondeu) - usando SQLite como fallback.");
        }
        SQLiteProvider provider = new SQLiteProvider(plugin.getDataFolder(), config.getString("sqlite.file", "database.db"));
        plugin.getLogger().info("[AlkaCore] SQLite inicializado!");
        return provider;
    }
}
