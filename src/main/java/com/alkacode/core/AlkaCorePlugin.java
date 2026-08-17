package com.alkacode.core;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.CurrencyRepository;
import com.alkacode.core.database.DatabaseFactory;
import com.alkacode.core.economy.VaultEconomyBridge;
import com.alkacode.core.gui.GuiListener;
import com.alkacode.core.message.AlkaMessageProvider;
import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.core.shiftlore.ShiftLoreBootstrap;
import com.alkacode.core.shiftlore.ShiftLoreService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Infraestrutura compartilhada da network Alka* - sem logica de jogo. Inicializa
 * a conexao unica de banco, a economia (Vault), mensagens (MiniMessage) e o
 * listener de GUI uma unica vez, e publica tudo via {@link AlkaAPI#get()} pra
 * qualquer plugin que estenda {@link com.alkacode.core.plugin.AlkaPlugin}.
 */
public final class AlkaCorePlugin extends JavaPlugin {
    private DatabaseProvider database;
    private AlkaScheduler scheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.scheduler = new AlkaScheduler(this);
        this.database = DatabaseFactory.create(this);
        CurrencyRepository currency = new CurrencyRepository(database, scheduler);
        VaultEconomyBridge economy = new VaultEconomyBridge();
        AlkaMessageProvider messages = new AlkaMessageProvider(getConfig());
        ShiftLoreService shiftLore = ShiftLoreBootstrap.createService();

        AlkaAPI.register(new AlkaAPI(database, currency, economy, messages, scheduler, shiftLore));

        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        // ProtocolLib e softdepend - sem ordem garantida de onEnable (mesma licao
        // ja aplicada no AlkaDrop). Nunca resolver isso sincrono aqui.
        Bukkit.getScheduler().runTask(this, () -> ShiftLoreBootstrap.enable(this, shiftLore));

        getLogger().info("AlkaCore habilitado! v" + getPluginMeta().getVersion()
            + " (banco: " + (database.isMySQL() ? "MySQL" : "SQLite")
            + ", economia: " + (economy.isAvailable() ? "Vault conectado" : "Vault indisponivel") + ")");
    }

    @Override
    public void onDisable() {
        // Cancela tasks assincronas (autosave etc) ANTES de fechar o pool, senao
        // uma escrita pendente pode estourar depois do banco ja fechado.
        if (scheduler != null) scheduler.cancelAll();
        AlkaAPI.unregister();
        if (database != null) database.close();
    }
}
