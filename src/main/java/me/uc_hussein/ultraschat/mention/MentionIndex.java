package me.uc_hussein.ultraschat.mention;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.util.VanishUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Thread-safe snapshot of mentionable player names (online, not vanished) so the async chat
 * thread never has to touch Bukkit player APIs.
 */
public final class MentionIndex {
    private final ULTRASChatPlugin plugin;
    private volatile Set<String> names = Set.of();
    private BukkitTask task;

    public MentionIndex(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        refresh();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 40L, 40L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void refresh() {
        refreshExcluding(null);
    }

    public void refreshExcluding(UUID exclude) {
        boolean respect = plugin.cfg() == null || plugin.cfg().bool("vanish.respect", true);
        Set<String> fresh = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(exclude) || (respect && VanishUtil.isVanished(p))) {
                continue;
            }
            fresh.add(p.getName().toLowerCase(Locale.ROOT));
        }
        names = fresh;
    }

    public boolean contains(String lowerName) {
        return names.contains(lowerName);
    }
}
