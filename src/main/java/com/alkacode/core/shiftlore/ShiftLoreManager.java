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
 * <p>NAO interceptamos pacote nenhum pra detectar Shift - {@link Player#isSneaking()}
 * ja e a mesma informacao, mantida pelo proprio Bukkit a partir do mesmo pacote do
 * cliente, sem reinventar isso via ProtocolLib.
 *
 * <p><b>Como a troca de lore aparece de verdade:</b> a lore detalhada so existe na
 * camada de pacote (o item real nunca muda). {@link #onToggleSneak} chama
 * {@link Player#updateInventory()} a cada toggle pra reenviar o inventario, e os
 * listeners de pacote reescrevem a lore quando {@link Player#isSneaking()} e true.
 *
 * <p><b>Limitacao conhecida:</b> ao soltar o shift, o {@code broadcastChanges()} do
 * Paper pode pular slots cujo conteudo real nao mudou (a lore vive so no pacote),
 * deixando a lore expandida travada ate o item ser movido. Por isso a lore orienta o
 * jogador a <b>mover o item</b> (nao soltar o shift de novo) pra lore fechar.
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
        // Dispara nos dois sentidos: entrar em Shift mostra a lore detalhada, sair
        // volta pro padrao - os dois exigem forcar o reenvio do inventario.
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
                if (!shouldRewrite(player)) {
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
                if (!shouldRewrite(player)) {
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

    /** Reescreve sempre que o jogador estiver segurando shift - em qualquer tela. */
    private boolean shouldRewrite(Player player) {
        return player.isSneaking();
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
