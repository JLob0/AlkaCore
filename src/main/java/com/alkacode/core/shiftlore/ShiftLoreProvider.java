package com.alkacode.core.shiftlore;

import com.alkacode.core.shiftlore.model.ShiftLoreEntry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Implementado por um plugin consumidor pra fornecer lore detalhado dinamico (ex:
 * AlkaFish gera a lore de um peixe a partir do peso/comprimento salvos no item).
 *
 * <p><b>Roda no caminho de envio do pacote (thread principal, sincrono)</b> - nao
 * pode bloquear (nada de I/O, banco, chamada de rede). Se o dado que voce precisa
 * vem de um cache em memoria (padrao ja usado em todo o ecossistema pra dado de
 * jogador online), isso e seguro; se vem so do banco, resolva pra um cache antes.
 */
public interface ShiftLoreProvider {

    /** Retorna o par de lore pra este item, ou null se este provider nao reconhece o item. */
    @Nullable ShiftLoreEntry getLore(Player viewer, ItemStack item);

    /** Maior = checado primeiro. Providers especificos devem vencer o loader vanilla (100). */
    default int getPriority() {
        return 100;
    }
}
