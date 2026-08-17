package com.alkacode.core.shiftlore;

import com.alkacode.core.shiftlore.model.ShiftLoreEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Le shift-lore.yml (itens vanilla, sem precisar de plugin nenhum registrar provider). */
final class VanillaShiftLoreLoader {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private record VanillaEntry(@Nullable String matchName, int matchCmd, ShiftLoreEntry entry) {
    }

    private final Map<Material, VanillaEntry> entries = new EnumMap<>(Material.class);
    private final Set<Material> blacklist = new HashSet<>();

    VanillaShiftLoreLoader(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "shift-lore.yml");
        if (!file.exists()) {
            try (var in = plugin.getResource("shift-lore.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar shift-lore.yml: " + e.getMessage());
            }
        }
        load(file, plugin);
    }

    private void load(File file, JavaPlugin plugin) {
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        for (String mat : config.getStringList("blacklist")) {
            try {
                blacklist.add(Material.valueOf(mat.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("shift-lore.yml: material invalido na blacklist: " + mat);
            }
        }

        ConfigurationSection items = config.getConfigurationSection("vanilla-items");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection itemSec = items.getConfigurationSection(key);
            if (itemSec == null) {
                continue;
            }
            ConfigurationSection match = itemSec.getConfigurationSection("match");
            if (match == null) {
                continue;
            }
            String matName = match.getString("material");
            if (matName == null) {
                continue;
            }
            Material material;
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("shift-lore.yml: material invalido em '" + key + "': " + matName);
                continue;
            }

            List<Component> shortLore = parseLore(itemSec.getStringList("lore-short"));
            List<Component> detailedLore = parseLore(itemSec.getStringList("lore-detailed"));
            String matchName = match.getString("name");
            int matchCmd = match.getInt("custom-model-data", -1);

            entries.put(material, new VanillaEntry(matchName, matchCmd,
                    new ShiftLoreEntry(shortLore, detailedLore)));
        }
    }

    private List<Component> parseLore(List<String> lines) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) {
            result.add(MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        return result;
    }

    @Nullable
    ShiftLoreEntry resolve(ItemStack item) {
        if (item == null || blacklist.contains(item.getType())) {
            return null;
        }
        VanillaEntry entry = entries.get(item.getType());
        if (entry == null) {
            return null;
        }

        if (entry.matchName() != null || entry.matchCmd() >= 0) {
            if (!item.hasItemMeta()) {
                return null;
            }
            ItemMeta meta = item.getItemMeta();
            if (entry.matchName() != null) {
                if (!meta.hasDisplayName()) {
                    return null;
                }
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());
                String expected = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(MM.deserialize(entry.matchName()));
                if (!plain.equals(expected)) {
                    return null;
                }
            }
            if (entry.matchCmd() >= 0 && (!meta.hasCustomModelData() || meta.getCustomModelData() != entry.matchCmd())) {
                return null;
            }
        }

        return entry.entry();
    }
}
