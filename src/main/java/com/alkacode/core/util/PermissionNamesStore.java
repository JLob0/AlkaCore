package com.alkacode.core.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Guarda nomes amigaveis de permissoes num arquivo YAML proprio do plugin (ex:
 * "permission-names.yml"), editavel pelo admin. Quando uma permissao ainda nao tem nome
 * cadastrado, registerUnknown grava a propria chave crua como valor padrao (visivel/
 * editavel no arquivo, nunca mostrado direto pro jogador - ver PermissionLoreUtil, que
 * pula a linha ate o nome existir de verdade). Cada plugin (AlkaVips, AlkaRankUp, etc)
 * usa a sua propria instancia/arquivo - nao ha nome global compartilhado entre plugins,
 * cada um pode nomear a mesma permissao diferente conforme o contexto do seu proprio menu.
 */
public final class PermissionNamesStore {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public PermissionNamesStore(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Nao foi possivel criar " + file.getName(), e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    /** Nome amigavel ja cadastrado pra essa chave, ou null se ainda nao existir. */
    public String lookup(String lookupKey) {
        return config.getString(lookupKey);
    }

    /** Grava a propria chave como valor padrao (editavel) se ainda nao existir - idempotente. */
    public void registerUnknown(String lookupKey) {
        if (config.isString(lookupKey)) {
            return;
        }
        config.set(lookupKey, lookupKey);
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Nao foi possivel salvar " + file.getName(), e);
        }
    }
}
