package com.alkacode.core.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGui gui)) return;
        if (event.getClickedInventory() != event.getInventory()) {
            event.setCancelled(true);
            return;
        }
        gui.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGui) event.setCancelled(true);
    }
}
