package me.uc_hussein.ultraschat.gui;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/** One parsed menu page. */
public record GuiPage(String fileId, String pageId, String titleFormat, String titleKey, int size,
                      boolean filler, Material fillerMaterial, List<GuiItem> items, Map<Integer, GuiItem> bySlot) {
}
