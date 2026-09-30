package me.uc_hussein.ultraschat.notification;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Short HUD notifications. Longer durations re-send the bar; tasks are cleaned up on quit/disable. */
public final class ActionBarService {
    private final ULTRASChatPlugin plugin;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public ActionBarService(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void show(Player p, Component c) {
        UUID id = p.getUniqueId();
        clear(id);
        p.sendActionBar(c);
        int total = Math.max(20, plugin.cfg().integer("actionbar.duration-ticks"));
        if (total <= 40) {
            return;
        }
        final int[] remaining = {total - 40};
        final BukkitTask[] self = new BukkitTask[1];
        self[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null || remaining[0] <= 0) {
                self[0].cancel();
                tasks.remove(id, self[0]);
                return;
            }
            online.sendActionBar(c);
            remaining[0] -= 40;
        }, 40L, 40L);
        tasks.put(id, self[0]);
    }

    public void clear(UUID id) {
        BukkitTask t = tasks.remove(id);
        if (t != null) {
            t.cancel();
        }
    }

    public void shutdown() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }
}
