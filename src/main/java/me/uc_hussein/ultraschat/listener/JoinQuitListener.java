package me.uc_hussein.ultraschat.listener;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.config.Cfg;
import me.uc_hussein.ultraschat.logging.ChatLog;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import me.uc_hussein.ultraschat.util.VanishUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/** Join / quit messages, sounds and per-player state cleanup. */
public final class JoinQuitListener implements Listener {
    private final ULTRASChatPlugin plugin;

    public JoinQuitListener(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        try {
            plugin.players().touch(p);
            plugin.mentionIndex().refresh();
            warnAdmin(p);

            Cfg cfg = plugin.cfg();
            Component original = e.joinMessage();
            if (!cfg.bool("messages.join", true)) {
                if (!cfg.bool("fallback.show-vanilla-message-when-disabled")) {
                    e.joinMessage(null);
                }
                return;
            }
            if (VanishUtil.hidesEvents(plugin, p)) {
                e.joinMessage(null);
                return;
            }
            if (original == null && cfg.bool("join.respect-suppressed-message", true)) {
                return;
            }
            e.joinMessage(null);
            Component name = Component.text(p.getName(), plugin.theme().color("join"));
            String formatKey = !p.hasPlayedBefore() && cfg.bool("join.first-join.enabled", true)
                    ? "join.first-format" : "join.format";
            plugin.messages().broadcast(PlayerSetting.JOIN_QUIT, null, lang ->
                    plugin.messages().component(lang, formatKey, Placeholders.create().put("player", name)));
            if (cfg.bool("join.sound.enabled", true)) {
                boolean self = cfg.bool("join.sound.play-to-self", false);
                for (Player o : Bukkit.getOnlinePlayers()) {
                    if (o.equals(p) && !self) {
                        continue;
                    }
                    plugin.sounds().play(o, "join", PlayerSetting.JOIN_QUIT_SOUNDS);
                }
            }
            plugin.log().log(ChatLog.Category.JOIN_QUIT, "join " + p.getName());
        } catch (RuntimeException ex) {
            plugin.log().error("Join handling failed", ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        try {
            Cfg cfg = plugin.cfg();
            Component original = e.quitMessage();
            if (!cfg.bool("messages.quit", true)) {
                if (!cfg.bool("fallback.show-vanilla-message-when-disabled")) {
                    e.quitMessage(null);
                }
            } else if (VanishUtil.hidesEvents(plugin, p)) {
                e.quitMessage(null);
            } else if (original != null || !cfg.bool("quit.respect-suppressed-message", true)) {
                e.quitMessage(null);
                Component name = Component.text(p.getName(), plugin.theme().color("quit"));
                plugin.messages().broadcast(PlayerSetting.JOIN_QUIT, id, lang ->
                        plugin.messages().component(lang, "quit.format", Placeholders.create().put("player", name)));
                if (cfg.bool("quit.sound.enabled", true)) {
                    for (Player o : Bukkit.getOnlinePlayers()) {
                        if (!o.equals(p)) {
                            plugin.sounds().play(o, "quit", PlayerSetting.JOIN_QUIT_SOUNDS);
                        }
                    }
                }
                plugin.log().log(ChatLog.Category.JOIN_QUIT, "quit " + p.getName());
            }
        } catch (RuntimeException ex) {
            plugin.log().error("Quit handling failed", ex);
        }
        plugin.antiSpam().clear(id);
        plugin.actionBars().clear(id);
        plugin.privateMessages().forget(id);
        plugin.mentionIndex().refreshExcluding(id);
    }

    private void warnAdmin(Player p) {
        if (plugin.config().warnings().isEmpty() || !p.hasPermission("ultraschat.admin")) {
            return;
        }
        int count = plugin.config().warnings().size();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                plugin.messages().error(p, "general.config-warnings-join",
                        Placeholders.create().text("count", String.valueOf(count)));
            }
        }, 60L);
    }
}
