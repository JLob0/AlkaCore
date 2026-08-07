package com.alkacode.core.message;

import com.alkacode.core.api.MessageProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public final class AlkaMessageProvider implements MessageProvider {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final FileConfiguration config;

    public AlkaMessageProvider(FileConfiguration config) {
        this.config = config;
    }

    private String prefix() {
        return config.getString("messages.prefix", "<gray>[Alka]</gray> ");
    }

    @Override
    public Component parse(String text) {
        return miniMessage.deserialize(text == null ? "" : text);
    }

    @Override
    public Component parsePrefixed(String text) {
        return parse(prefix() + (text == null ? "" : text));
    }

    @Override
    public void send(CommandSender target, String text) {
        target.sendMessage(parse(text));
    }

    @Override
    public void sendPrefixed(CommandSender target, String text) {
        target.sendMessage(parsePrefixed(text));
    }

    @Override
    public String legacyToMiniMessage(String legacyText) {
        if (legacyText == null) return "";
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(legacyText);
        return miniMessage.serialize(component);
    }
}
