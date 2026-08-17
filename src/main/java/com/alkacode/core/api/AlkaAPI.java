package com.alkacode.core.api;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.core.shiftlore.ShiftLoreService;

/**
 * Acessor estatico da API do AlkaCore. Inicializado uma unica vez por
 * {@code AlkaCorePlugin.onEnable()} - plugins consumidores so leem, nunca criam
 * sua propria instancia (diferente de um lazy-singleton "primeiro que chamar
 * ganha": isso dependeria de ordem de load e do config.yml de quem chegou
 * primeiro, o que e fragil). A ordem certa e garantida pelo `depend: [AlkaCore]`
 * no plugin.yml de cada consumidor.
 */
public final class AlkaAPI {
    private static AlkaAPI instance;

    private final DatabaseProvider database;
    private final CurrencyAPI currency;
    private final EconomyBridge economy;
    private final MessageProvider messages;
    private final AlkaScheduler scheduler;
    private final ShiftLoreService shiftLore;

    public AlkaAPI(DatabaseProvider database, CurrencyAPI currency, EconomyBridge economy, MessageProvider messages,
                    AlkaScheduler scheduler, ShiftLoreService shiftLore) {
        this.database = database;
        this.currency = currency;
        this.economy = economy;
        this.messages = messages;
        this.scheduler = scheduler;
        this.shiftLore = shiftLore;
    }

    /** Chamado uma unica vez, em AlkaCorePlugin#onEnable. */
    public static void register(AlkaAPI api) {
        instance = api;
    }

    public static AlkaAPI get() {
        if (instance == null) {
            throw new IllegalStateException(
                "AlkaAPI ainda nao foi inicializada - o plugin que chamou isso precisa de "
                    + "\"depend: [AlkaCore]\" no plugin.yml pra garantir ordem de load.");
        }
        return instance;
    }

    /** Chamado em AlkaCorePlugin#onDisable, pra nao deixar uma referencia morta viva depois de um /reload. */
    public static void unregister() {
        instance = null;
    }

    public DatabaseProvider getDatabase() { return database; }
    public CurrencyAPI getCurrency() { return currency; }
    public EconomyBridge getEconomy() { return economy; }
    public MessageProvider getMessages() { return messages; }
    public AlkaScheduler getScheduler() { return scheduler; }
    /** Nunca null - registrar provider aqui e sempre seguro, mesmo sem ProtocolLib instalado (so fica sem efeito visual). */
    public ShiftLoreService getShiftLore() { return shiftLore; }
}
