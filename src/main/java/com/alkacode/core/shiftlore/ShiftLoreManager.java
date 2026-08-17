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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor do Shift-Lore System - so construido se o ProtocolLib estiver presente
 * (ver {@link com.alkacode.core.hooks.ProtocolLibHook}).
 *
 * <p>NAO interceptamos pacote nenhum pra detectar Shift - {@link Player#isSneaking()}
 * ja e a mesma informacao, mantida pelo proprio Bukkit a partir do mesmo pacote do
 * cliente, sem reinventar isso via ProtocolLib.
 *
 * <p><b>Como a troca de lore aparece de verdade:</b> a lore detalhada so existe na
 * camada de pacote (o item real nunca muda). Logo, so adianta reescrever os pacotes
 * de saida se um pacote novo for gerado no instante em que o Shift muda - e o
 * inventario/hotbar nao reenvia sozinho quando nada muda por outro motivo. O primeiro
 * problema: {@code player.updateInventory()} NAO resolve isso, porque
 * {@code Container.broadcastChanges()} so reenvia slots cujo conteudo mudou e, como a
 * lore vive so no pacote, o container acha que nada mudou e nao envia nada.
 *
 * <p>A correcao: {@link #forceResend} zera (via reflexao) o cache de deteccao de mudanca
 * {@code lastSlots} do container aberto do jogador e so entao chama
 * {@code updateInventory()}, forcando o broadcast a enviar todos os slots. O Core
 * compila so contra a paper-api, entao o acesso ao NMS e por reflexao e totalmente
 * defensivo: se o nome de campo/versao mudar, cai no fallback (o velho updateInventory,
 * que no pior caso e um no-op e nao quebra nada).
 *
 * <p><b>Inventario fechado = vanilla:</b> os listeners so reescrevem quando o jogador
 * esta com shift E com um inventario aberto (rastreado via
 * {@code InventoryOpenEvent}/{@code InventoryCloseEvent}). Com o inventario fechado,
 * nenhum pacote e alterado - a hotbar fica exatamente como o vanilla.
 */
public final class ShiftLoreManager implements Listener {

    private final ShiftLoreService service;
    private final Set<UUID> openInventories = ConcurrentHashMap.newKeySet();

    public ShiftLoreManager(JavaPlugin plugin, ShiftLoreService service) {
        this.service = service;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerPacketListeners(plugin);
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        // Soh tem lore pra trocar se o jogador estiver olhando um inventario.
        // Com o inventario fechado (hotbar), Shift fica 100% vanilla.
        if (openInventories.contains(event.getPlayer().getUniqueId())) {
            forceResend(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openInventories.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openInventories.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
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

    /** Soh reescreve quando o jogador segura shift COM um inventario aberto. */
    private boolean shouldRewrite(Player player) {
        return player.isSneaking() && openInventories.contains(player.getUniqueId());
    }

    /**
     * Forca um reenvio real de todos os slots do container aberto. Zera o cache de
     * deteccao de mudanca do NMS e chama {@code updateInventory()}, que entao envia
     * SET_SLOT de todos os slots (e esses passam pelos listeners acima, trocando a
     * lore conforme o estado do Shift). Nunca lanca - qualquer falha de reflexao cai
     * no fallback (updateInventory simples, que no pior caso nao reenvia nada).
     */
    private void forceResend(Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(player);
            for (String menuField : new String[]{"containerMenu", "inventoryMenu"}) {
                Field field;
                try {
                    field = serverPlayer.getClass().getField(menuField);
                } catch (NoSuchFieldException e) {
                    continue;
                }
                Object menu = field.get(serverPlayer);
                if (menu != null) {
                    resetLastSlots(menu);
                }
            }
        } catch (Throwable t) {
            // reflexao falhou - segue pro fallback abaixo.
        }
        player.updateInventory();
    }

    /**
     * Marca todos os slots do menu como "mudados" colocando um item vazio (NMS
     * ItemStack.EMPTY) no cache lastSlots, pra {@code broadcastChanges()} reenviar
     * todos os slots em vez de so os realmente alterados.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void resetLastSlots(Object menu) {
        try {
            Field lastSlots = findField(menu.getClass(), "lastSlots");
            if (lastSlots == null) {
                return;
            }
            lastSlots.setAccessible(true);
            Object cache = lastSlots.get(menu);
            if (!(cache instanceof List)) {
                return;
            }
            Object empty = nmsEmptyItem();
            if (empty == null) {
                return;
            }
            List list = (List) cache;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, empty);
            }
        } catch (Throwable t) {
            // nome de campo/versao diferente - nao quebra, so nao force o reenvio.
        }
    }

    private Field findField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // sobe na hierarquia.
            }
        }
        return null;
    }

    private Object nmsEmptyItem() {
        try {
            return Class.forName("net.minecraft.world.item.ItemStack").getField("EMPTY").get(null);
        } catch (Throwable t) {
            return null;
        }
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
