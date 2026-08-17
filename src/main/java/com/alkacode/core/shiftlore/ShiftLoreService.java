package com.alkacode.core.shiftlore;

import com.alkacode.core.shiftlore.model.ShiftLoreEntry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ponto de registro do Shift-Lore System - acessado via
 * {@code getAlkaAPI().getShiftLore()}, nunca um singleton estatico proprio (ver
 * padrao do resto do AlkaAPI). Cada provider fica associado ao {@link Plugin} que
 * registrou, pra {@link ShiftLoreManager} conseguir limpar automaticamente quando
 * esse plugin desabilitar (sem exigir que cada consumidor lembre de desregistrar
 * no proprio onDisable).
 */
public final class ShiftLoreService {

    private record Owned(Plugin owner, ShiftLoreProvider provider) {
    }

    private final List<Owned> providers = new CopyOnWriteArrayList<>();
    private volatile VanillaShiftLoreLoader vanillaLoader;

    void setVanillaLoader(VanillaShiftLoreLoader loader) {
        this.vanillaLoader = loader;
    }

    /** Chamado tipicamente no onPluginEnable do consumidor. */
    public void registerProvider(Plugin owner, ShiftLoreProvider provider) {
        providers.add(new Owned(owner, provider));
        providers.sort(Comparator.comparingInt((Owned o) -> o.provider().getPriority()).reversed());
    }

    public void unregisterProvider(ShiftLoreProvider provider) {
        providers.removeIf(o -> o.provider() == provider);
    }

    /** Chamado pelo {@link ShiftLoreManager} quando um plugin desabilita - nunca precisa ser chamado a mao. */
    void unregisterAllFrom(Plugin owner) {
        providers.removeIf(o -> o.owner() == owner);
    }

    /** Resolve o lore pra um item: providers de plugin primeiro (por prioridade), vanilla-lore.yml por ultimo. */
    @Nullable
    public ShiftLoreEntry resolve(Player viewer, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        for (Owned owned : providers) {
            try {
                ShiftLoreEntry entry = owned.provider().getLore(viewer, item);
                if (entry != null) {
                    return entry;
                }
            } catch (Throwable t) {
                // um provider quebrado nunca pode derrubar o pacote de todo mundo -
                // so pula pro proximo (vanilla loader ainda cobre o item).
            }
        }
        VanillaShiftLoreLoader loader = vanillaLoader;
        return loader != null ? loader.resolve(item) : null;
    }
}
