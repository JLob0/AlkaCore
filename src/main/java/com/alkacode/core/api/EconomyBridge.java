package com.alkacode.core.api;

import java.util.UUID;

/**
 * Abstracao sobre a economia principal da network (Vault - hoje o unico provider
 * registrado e o VaultHook do AlkaEconomy, cobrindo CurrencyType.COINS). O Core
 * fala com Vault, nao com AlkaEconomy diretamente, pra nao violar a regra de
 * "Core nao depende de nenhum plugin Alka*".
 */
public interface EconomyBridge {
    boolean isAvailable();

    double getBalance(UUID uuid);

    boolean has(UUID uuid, double amount);

    boolean withdraw(UUID uuid, double amount);

    void deposit(UUID uuid, double amount);

    String format(double amount);
}
