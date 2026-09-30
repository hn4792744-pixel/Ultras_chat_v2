package me.uc_hussein.ultraschat.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Marks inventories created by this plugin and carries the menu state. */
public final class SettingsHolder implements InventoryHolder {
    private final UUID viewerId;
    private final UUID targetId;
    private final String targetName;
    private final boolean admin;
    private final GuiPage page;
    private final long generation;
    private final String returnFile;
    private final String returnPage;
    private Inventory inventory;

    public SettingsHolder(UUID viewerId, UUID targetId, String targetName, boolean admin, GuiPage page,
                          long generation, String returnFile, String returnPage) {
        this.viewerId = viewerId;
        this.targetId = targetId;
        this.targetName = targetName;
        this.admin = admin;
        this.page = page;
        this.generation = generation;
        this.returnFile = returnFile;
        this.returnPage = returnPage;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID viewerId() { return viewerId; }
    public UUID targetId() { return targetId; }
    public String targetName() { return targetName; }
    public boolean admin() { return admin; }
    public GuiPage page() { return page; }
    public long generation() { return generation; }
    public String returnFile() { return returnFile; }
    public String returnPage() { return returnPage; }
}
