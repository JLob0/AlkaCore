package com.alkacode.core.economy;

import com.alkacode.core.api.EconomyBridge;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Ponte pro Vault via ServicesManager - funciona com qualquer plugin que registre
 * um provider Vault (hoje, o VaultHook do AlkaEconomy cobrindo CurrencyType.COINS,
 * id atual "gold"). O Core
 * nunca importa classes do AlkaEconomy, so fala com a API generica do Vault.
 */
public final class VaultEconomyBridge implements EconomyBridge {
    private Economy economy;

    public VaultEconomyBridge() {
        resolve();
    }

    private void resolve() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) this.economy = rsp.getProvider();
    }

    @Override
    public boolean isAvailable() {
        if (economy == null) resolve();
        return economy != null;
    }

    @Override
    public double getBalance(UUID uuid) {
        if (!isAvailable()) return 0;
        return economy.getBalance(offline(uuid));
    }

    @Override
    public boolean has(UUID uuid, double amount) {
        if (!isAvailable()) return false;
        return economy.has(offline(uuid), amount);
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        if (!isAvailable()) return false;
        return economy.withdrawPlayer(offline(uuid), amount).transactionSuccess();
    }

    @Override
    public void deposit(UUID uuid, double amount) {
        if (!isAvailable()) return;
        economy.depositPlayer(offline(uuid), amount);
    }

    @Override
    public String format(double amount) {
        if (!isAvailable()) return String.valueOf(amount);
        return economy.format(amount);
    }

    private OfflinePlayer offline(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }
}
