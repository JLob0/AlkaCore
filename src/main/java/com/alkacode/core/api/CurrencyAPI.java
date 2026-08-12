package com.alkacode.core.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Moedas secundarias genericas (po, fragmentos, tokens, etc), numa tabela unica
 * `alkacore_currencies` compartilhada por quem usar o AlkaCore, identificadas por
 * um currencyId livre por plugin (ex: "alkaclasses_dust", "alkamines_fragments").
 *
 * NAO e substituto do AlkaEconomy (COINS/DRAKONIO/NACAR/ESCARION/SOULS) - aquilo
 * continua sendo a economia principal da network, acessada via {@link EconomyBridge}.
 * Isso aqui e so pra parar de cada plugin reinventar sua propria tabela tipo
 * `player_dust` pra saldo secundario que so aquele plugin usa.
 */
public interface CurrencyAPI {
    int getBalance(UUID uuid, String currencyId);

    void setBalance(UUID uuid, String currencyId, int amount);

    void add(UUID uuid, String currencyId, int amount);

    boolean remove(UUID uuid, String currencyId, int amount);

    /** Versao assincrona de {@link #getBalance(UUID, String)} - nunca toca a main thread. */
    CompletableFuture<Integer> getBalanceAsync(UUID uuid, String currencyId);

    /** Versao assincrona de {@link #add(UUID, String, int)}. */
    CompletableFuture<Void> addAsync(UUID uuid, String currencyId, int amount);

    /** Versao assincrona de {@link #remove(UUID, String, int)} - completa com true se removeu. */
    CompletableFuture<Boolean> removeAsync(UUID uuid, String currencyId, int amount);
}
