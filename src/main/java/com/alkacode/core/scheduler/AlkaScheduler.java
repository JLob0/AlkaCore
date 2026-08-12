package com.alkacode.core.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Abstracao de agendamento assincrono pro AlkaCore. Usa o AsyncScheduler do
 * Paper ({@link Bukkit#getAsyncScheduler()}), que existe tanto no Paper quanto
 * no Folia, entao a mesma chamada funciona com ou sem Folia - basta o jar rodar
 * numa build Paper 1.20.2+ (o Core ja exige 1.21.8).
 *
 * <p>Regra de ouro: <b>nenhum trabalho de banco deve rodar na main thread</b>.
 * Repositorios (veja {@link com.alkacode.core.database.CurrencyRepository})
 * devem usar {@link #runAsync(Runnable)} pras operacoes que o chamador nao espera
 * de volta e os variantes *Async (CompletableFuture) pras que precisam do valor.
 *
 * <p>Tasks registradas sao rastreadas aqui e canceladas em
 * {@link #cancelAll()}, chamado pelo AlkaCorePlugin.onDisable() ANTES de fechar
 * o pool de conexoes, pra nao sobrar escrita pendente depois do banco fechado.
 */
public final class AlkaScheduler {
    private final JavaPlugin plugin;
    private final AsyncScheduler scheduler;
    private final List<ScheduledTask> tasks = new ArrayList<>();

    public AlkaScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getAsyncScheduler();
    }

    /** Roda uma unica vez em thread assincrona. Nunca joga excecao pra fora - loga e engole. */
    public void runAsync(Runnable runnable) {
        scheduler.runNow(plugin, task -> guard(runnable));
    }

    /**
     * Roda repetidamente em thread assincrona (primitivo pra autosave de qualquer
     * plugin Alka*). A task retornada pelo scheduler e guardada pra ser cancelada
     * no shutdown via {@link #cancelAll()}.
     */
    public void runAsyncRepeating(Runnable runnable, long delayTicks, long periodTicks) {
        // O TimeUnit do Paper nao tem TICKS; converte explicitamente pra millis
        // (1 tick = 50ms) pra nao depender de como o API interpreta a unidade.
        ScheduledTask task = scheduler.runAtFixedRate(
            plugin, t -> guard(runnable), delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
        synchronized (tasks) {
            tasks.add(task);
        }
    }

    private void guard(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            plugin.getLogger().severe("[AlkaCore] Erro em task assincrona: " + e);
        }
    }

    /** Cancela todas as tasks registradas (inclusive as repetidas) - chamar antes de fechar o banco. */
    public void cancelAll() {
        synchronized (tasks) {
            for (ScheduledTask task : tasks) task.cancel();
            tasks.clear();
        }
    }
}
