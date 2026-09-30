package me.uc_hussein.ultraschat.gui;

import me.uc_hussein.ultraschat.settings.PlayerSetting;
import org.bukkit.Material;

import java.util.List;

/** One configured button. label/desc are language keys. */
public record GuiItem(String id, int slot, Material material, ItemType type, PlayerSetting setting, String page,
                      String label, String desc, String name, List<String> lore, ToggleStyle style) {
}
