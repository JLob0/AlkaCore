package com.alkacode.core.shiftlore.model;

import net.kyori.adventure.text.Component;

import java.util.List;

/** Par de lore: curto (padrao) e detalhado (jogador segurando Shift). */
public record ShiftLoreEntry(List<Component> shortLore, List<Component> detailedLore) {

    public boolean hasDetailedLore() {
        return detailedLore != null && !detailedLore.isEmpty();
    }
}
