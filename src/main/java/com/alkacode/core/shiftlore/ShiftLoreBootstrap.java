package com.alkacode.core.shiftlore;

import com.alkacode.core.hooks.ProtocolLibHook;
import org.bukkit.plugin.java.JavaPlugin;

/** Unico ponto de entrada publico do pacote - mantem {@link VanillaShiftLoreLoader} package-private. */
public final class ShiftLoreBootstrap {

    private ShiftLoreBootstrap() {
    }

    public static ShiftLoreService createService() {
        return new ShiftLoreService();
    }

    /**
     * Carrega o shift-lore.yml e, se o ProtocolLib estiver presente, liga o motor
     * de pacotes. Chame 1 tick depois do onEnable (softdepend nao garante ordem -
     * ver AlkaCorePlugin), nunca sincrono ali.
     */
    public static void enable(JavaPlugin corePlugin, ShiftLoreService service) {
        service.setVanillaLoader(new VanillaShiftLoreLoader(corePlugin));
        if (ProtocolLibHook.isPresent()) {
            new ShiftLoreManager(corePlugin, service);
            corePlugin.getLogger().info("Shift-Lore System ativo (ProtocolLib detectado).");
        } else {
            corePlugin.getLogger().info("ProtocolLib nao encontrado - Shift-Lore System fica so com o registro de providers, sem efeito visual.");
        }
    }
}
