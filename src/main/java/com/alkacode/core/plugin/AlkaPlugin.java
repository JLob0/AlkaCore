package com.alkacode.core.plugin;

import com.alkacode.core.api.AlkaAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classe base pros plugins Alka* que dependem do AlkaCore. Precisa de
 * `depend: [AlkaCore]` no plugin.yml pra garantir que {@link AlkaAPI#get()} ja
 * esteja registrada quando {@link #onPluginEnable()} rodar.
 *
 * Nao registra {@link com.alkacode.core.gui.GuiListener} - isso e feito uma
 * unica vez pelo proprio AlkaCore (registrar de novo em cada plugin filho faria
 * cada clique em qualquer BaseGui, de qualquer plugin, disparar N vezes).
 */
public abstract class AlkaPlugin extends JavaPlugin {
    private AlkaAPI api;

    @Override
    public final void onEnable() {
        saveDefaultConfig();
        this.api = AlkaAPI.get();
        onPluginEnable();
    }

    @Override
    public final void onDisable() {
        onPluginDisable();
    }

    public AlkaAPI getAlkaAPI() { return api; }

    protected abstract void onPluginEnable();

    protected abstract void onPluginDisable();
}
