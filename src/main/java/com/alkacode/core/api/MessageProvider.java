package com.alkacode.core.api;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/** MiniMessage + prefixo padrao configuravel (messages.prefix no config.yml do AlkaCore). */
public interface MessageProvider {
    Component parse(String miniMessage);

    Component parsePrefixed(String miniMessage);

    void send(CommandSender target, String miniMessage);

    void sendPrefixed(CommandSender target, String miniMessage);

    String legacyToMiniMessage(String legacyText);
}
