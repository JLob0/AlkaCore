package com.alkacode.core.api;

import java.util.UUID;

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
}
