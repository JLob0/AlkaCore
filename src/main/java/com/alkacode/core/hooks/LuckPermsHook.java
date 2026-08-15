package com.alkacode.core.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /** Chaves de todos os nos de permissao (nao negados) direto num grupo do LuckPerms,
     * ordenadas - usada pra montar lore "permissoes deste rank/vip" (ver PermissionLoreUtil).
     * Lista vazia se o LuckPerms nao estiver presente ou o grupo nao existir. */
    public static List<String> getGroupPermissionKeys(String groupName) {
        return api().map(lp -> {
            Group group = lp.getGroupManager().getGroup(groupName);
            if (group == null) {
                return List.<String>of();
            }
            return group.getNodes().stream()
                .filter(node -> !node.isNegated())
                .map(Node::getKey)
                .sorted()
                .collect(Collectors.toList());
        }).orElse(List.of());
    }
}
