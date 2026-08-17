package com.alkacode.core.hooks;

import org.bukkit.Bukkit;

/**
 * So a checagem de presenca - o Core nao mexe com packets diretamente (isso e
 * logica de jogo especifica de cada plugin, ex: GlowPacketManager do
 * AlkaEffects). Existe aqui pra centralizar o `getPlugin("ProtocolLib")`
 * que hoje se repete em varios onEnable().
 */
public final class ProtocolLibHook {
    private ProtocolLibHook() {}

    public static boolean isPresent() {
        return Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
    }
}
