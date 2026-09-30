package me.uc_hussein.ultraschat.gui;

import java.util.List;

/** Name/lore templates for the ON and OFF state of a toggle button. */
public record ToggleStyle(String enabledName, List<String> enabledLore, String disabledName, List<String> disabledLore) {
}
