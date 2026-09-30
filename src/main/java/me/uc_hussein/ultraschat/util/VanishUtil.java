package me.uc_hussein.ultraschat.util;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

/** Vanish handling based on the common "vanished" metadata convention and Bukkit's canSee. */
public final class VanishUtil {
    private VanishUtil() {
    }

    public static boolean isVanished(Player p) {
        for (MetadataValue v : p.getMetadata("vanished")) {
            if (v.asBoolean()) {
                return true;
            }
        }
        return false;
    }

    /** True if {@code viewer} is allowed to know about / interact with {@code target}. */
    public static boolean visibleTo(ULTRASChatPlugin plugin, CommandSender viewer, Player target) {
        if (!plugin.cfg().bool("vanish.respect", true)) {
            return true;
        }
        if (viewer instanceof Player v && !v.canSee(target)) {
            return false;
        }
        if (isVanished(target)) {
            return viewer.hasPermission("ultraschat.vanish.see");
        }
        return true;
    }

    /** True if ULTRAS must stay silent about this player's join/quit/death/achievement (vanished staff). */
    public static boolean hidesEvents(ULTRASChatPlugin plugin, Player p) {
        return plugin.cfg().bool("vanish.respect", true)
                && plugin.cfg().bool("vanish.hide-events", true)
                && isVanished(p);
    }
}
