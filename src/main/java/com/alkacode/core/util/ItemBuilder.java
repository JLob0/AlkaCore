package com.alkacode.core.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Builder de ItemStack em MiniMessage - equivalente generico ao antigo ItemBuilder baseado em ChatColor/'&'. */
public class ItemBuilder {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ItemStack item;
    private final ItemMeta meta;
    private final List<Component> lore = new ArrayList<>();

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    /** PLAYER_HEAD com textura Base64 custom (skin de mob/cabeca cosmetica) - sem nome/lore, encadeie com name()/lore(). */
    public static ItemStack skullFromTexture(String base64Texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        PlayerProfile profile = (PlayerProfile) Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", base64Texture));
        meta.setPlayerProfile(profile);
        skull.setItemMeta(meta);
        return skull;
    }

    public static ItemBuilder of(String materialName, Material fallback) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = fallback;
        }
        return new ItemBuilder(material);
    }

    public ItemBuilder name(String miniMessage) {
        if (meta != null) meta.displayName(MM.deserialize("<!i>" + miniMessage));
        return this;
    }

    public ItemBuilder lore(String miniMessageLine) {
        lore.add(MM.deserialize("<!i><gray>" + miniMessageLine));
        return this;
    }

    public ItemBuilder loreIfPresent(boolean condition, String miniMessageLine) {
        if (condition) lore(miniMessageLine);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (meta != null && glow) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
