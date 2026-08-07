package com.alkacode.core.hooks;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/** Presenca + parsing de placeholders via PlaceholderAPI. Registrar expansoes continua sendo tarefa de cada plugin. */
public final class PapiHook {
    private PapiHook() {}

    public static boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static String parse(OfflinePlayer player, String text) {
        if (!isPresent()) return text;
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }
}
