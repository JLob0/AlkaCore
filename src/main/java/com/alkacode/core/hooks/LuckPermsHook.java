package com.alkacode.core.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.UUID;

public final class LuckPermsHook {
    private LuckPermsHook() {}

    public static boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    private static Optional<LuckPerms> api() {
        if (!isPresent()) return Optional.empty();
        try {
            return Optional.of(LuckPermsProvider.get());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    /** Nome do grupo primario do jogador, ou vazio se o LuckPerms nao estiver presente/carregado. */
    public static Optional<String> getPrimaryGroup(UUID uuid) {
        return api().map(lp -> lp.getUserManager().getUser(uuid))
            .map(user -> user != null ? user.getPrimaryGroup() : null);
    }
}
