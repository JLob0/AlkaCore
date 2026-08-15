package com.alkacode.core.gui;

import com.alkacode.core.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Sistema de GUI base pra todo plugin Alka* que dependa do AlkaCore - sem logica de jogo, so o motor. */
public abstract class BaseGui implements InventoryHolder {
    protected final JavaPlugin plugin;
    protected final Inventory inventory;
    protected final Player player;
    protected final String id;
    protected final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected BaseGui(JavaPlugin plugin, Player player, String title, int rows, String id) {
        this.plugin = plugin;
        this.player = player;
        this.id = id;
        this.inventory = Bukkit.createInventory(this, rows * 9, MiniMessage.miniMessage().deserialize(title));
    }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }

    public String getId() { return id; }

    public void open() {
        refresh();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2.0f);
    }

    /** Re-renderiza sem reabrir o inventario (paginacao, pos-acao) - limpa itens e clicks
     * velhos primeiro, senao um slot que some entre uma pagina e outra fica "fantasma"
     * (item e click antigo continuam la, so nao aparecem em nenhum render novo). */
    protected void refresh() {
        inventory.clear();
        actions.clear();
        render();
    }

    public abstract void render();

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        if (action != null) actions.put(slot, action);
    }

    protected void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    protected void fillBorder(ItemStack glass) {
        int rows = inventory.getSize() / 9;
        for (int i = 0; i < 9; i++) {
            setItem(i, glass);
            setItem((rows - 1) * 9 + i, glass);
        }
        for (int i = 1; i < rows - 1; i++) {
            setItem(i * 9, glass);
            setItem(i * 9 + 8, glass);
        }
    }

    protected void fill(ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) setItem(i, item);
        }
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + name));

        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(MiniMessage.miniMessage().deserialize("<!i><gray>" + line));
        }
        meta.lore(loreList);

        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "gui_item"), PersistentDataType.STRING, "true");

        item.setItemMeta(meta);
        return item;
    }

    /** Layout por char-map (linhas de 9 caracteres, um por slot) - espaco ou '_' pula o slot,
     * qualquer outro char presente em icons vira setItem(slot, icons.get(c), clicks.get(c)).
     * Generaliza fillBorder/fill: a borda e so mais uma entrada no mapa de icons. */
    protected void layout(String[] rows, Map<Character, ItemStack> icons, Map<Character, Consumer<InventoryClickEvent>> clicks) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length() && col < 9; col++) {
                char c = line.charAt(col);
                if (c == ' ' || c == '_') continue;
                ItemStack icon = icons.get(c);
                if (icon == null) continue;
                setItem(row * 9 + col, icon, clicks == null ? null : clicks.get(c));
            }
        }
    }

    /** PLAYER_HEAD com textura Base64 custom (cabeca cosmetica/mob-head) - ver ItemBuilder#skullFromTexture. */
    protected ItemStack headTexture(String base64Texture, String displayName, String... lore) {
        ItemStack skull = ItemBuilder.skullFromTexture(base64Texture);
        ItemMeta meta = skull.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + displayName));

        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(MiniMessage.miniMessage().deserialize("<!i><gray>" + line));
        }
        meta.lore(loreList);

        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "gui_item"), PersistentDataType.STRING, "true");
        skull.setItemMeta(meta);
        return skull;
    }

    /** Aplica glow (enchant fake + HIDE_ENCHANTS) num item ja pronto - mesmo padrao de ItemBuilder#glow(boolean). */
    protected ItemStack glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    protected ItemStack head(String playerName, String displayName, String... lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = skull.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + displayName));

        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(MiniMessage.miniMessage().deserialize("<!i><gray>" + line));
        }
        meta.lore(loreList);

        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        }

        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "gui_item"), PersistentDataType.STRING, "true");
        skull.setItemMeta(meta);
        return skull;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Consumer<InventoryClickEvent> action = actions.get(event.getSlot());
        if (action != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
            action.accept(event);
        }
    }
}
