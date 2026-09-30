package me.uc_hussein.ultraschat.listener;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.gui.SettingsHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Locks down ULTRAS menus: nothing can be moved, dragged or taken. */
public final class GuiListener implements Listener {
    private final ULTRASChatPlugin plugin;

    public GuiListener(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof SettingsHolder holder)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != top) {
            return;
        }
        ClickType t = e.getClick();
        if (t != ClickType.LEFT && t != ClickType.RIGHT && t != ClickType.SHIFT_LEFT && t != ClickType.SHIFT_RIGHT) {
            return;
        }
        plugin.gui().handleClick(p, holder, e.getSlot());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof SettingsHolder) {
            e.setCancelled(true);
        }
    }
}
