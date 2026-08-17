package com.alkacode.core.shiftlore;

import com.alkacode.core.shiftlore.model.ShiftLoreEntry;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor do Shift-Lore System - so construido se o ProtocolLib estiver presente
 * (ver {@link com.alkacode.core.hooks.ProtocolLibHook}).
 *
 * <p>Ao contrario do esboco original, NAO interceptamos pacote nenhum pra
 * detectar Shift - {@link Player#isSneaking()} ja e a mesma informacao,
 * mantida pelo proprio Bukkit a partir do mesmo pacote do cliente, sem
 * reinventar isso via ProtocolLib.
 *
 * <p>O pulo do gato que faltava no esboco original: so trocar o lore no pacote
 * de SAIDA nao adianta nada se nenhum pacote novo for enviado no instante em
 * que o jogador aperta Shift (o inventario/hotbar so reenvia quando algo muda
 * por outro motivo). Por isso {@link #onToggleSneak} forca um
 * {@code player.updateInventory()} a cada toggle - e esse reenvio forcado que
 * de fato passa pelos listeners abaixo e troca a lore visivel.
 */
public final class ShiftLoreManager implements Listener {

    private final ShiftLoreService service;

    public ShiftLoreManager(JavaPlugin plugin, ShiftLoreService service) {
        this.service = service;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerPacketListeners(plugin);
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        // dispara nos dois sentidos: entrar em Shift mostra o lore detalhado,
        // sair volta pro curto - os dois exigem forcar o reenvio.
        event.getPlayer().updateInventory();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        service.unregisterAllFrom(event.getPlugin());
    }

    private void registerPacketListeners(JavaPlugin plugin) {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();

        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (!player.isSneaking()) {
                    return;
                }
                List<ItemStack> items = event.getPacket().getItemListModifier().read(0);
                if (items == null) {
                    return;
                }
                List<ItemStack> modified = new ArrayList<>(items.size());
                boolean changed = false;
                for (ItemStack original : items) {
                    ItemStack processed = applyDetailedLore(player, original);
                    modified.add(processed);
                    changed |= processed != original;
                }
                if (changed) {
                    event.getPacket().getItemListModifier().write(0, modified);
                }
            }
        });

        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (!player.isSneaking()) {
                    return;
                }
                ItemStack item = event.getPacket().getItemModifier().read(0);
                if (item == null) {
                    return;
                }
                ItemStack processed = applyDetailedLore(player, item);
                if (processed != item) {
                    event.getPacket().getItemModifier().write(0, processed);
                }
            }
        });
    }

    /** Nunca lanca - qualquer falha aqui e no meio do pipeline de envio de pacote real. */
    private ItemStack applyDetailedLore(Player viewer, ItemStack original) {
        try {
            if (original == null || original.getType().isAir()) {
                return original;
            }
            ShiftLoreEntry entry = service.resolve(viewer, original);
            if (entry == null || !entry.hasDetailedLore()) {
                return original;
            }
            ItemStack modified = original.clone();
            ItemMeta meta = modified.getItemMeta();
            if (meta == null) {
                return original;
            }
            meta.lore(entry.detailedLore());
            modified.setItemMeta(meta);
            return modified;
        } catch (Throwable t) {
            return original;
        }
    }
}
