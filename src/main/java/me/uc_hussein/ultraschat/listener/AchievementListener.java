package me.uc_hussein.ultraschat.listener;

import io.papermc.paper.advancement.AdvancementDisplay;
import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.config.Cfg;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import me.uc_hussein.ultraschat.util.VanishUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.Locale;

/** Replaces advancement announcements. */
public final class AchievementListener implements Listener {
    private final ULTRASChatPlugin plugin;

    public AchievementListener(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        try {
            handle(e);
        } catch (RuntimeException ex) {
            plugin.log().error("Achievement handling failed", ex);
        }
    }

    private void handle(PlayerAdvancementDoneEvent e) {
        Cfg cfg = plugin.cfg();
        AdvancementDisplay display = e.getAdvancement().getDisplay();
        if (e.message() == null || display == null) {
            return;                          // not announced (recipes, hidden advancements, other plugins)
        }
        if (!cfg.bool("messages.achievements", true)) {
            if (!cfg.bool("fallback.show-vanilla-message-when-disabled")) {
                e.message(null);
            }
            return;
        }
        if (VanishUtil.hidesEvents(plugin, e.getPlayer())) {
            e.message(null);
            return;
        }
        String type = display.frame().name().toLowerCase(Locale.ROOT);
        if (!cfg.bool("achievement.types." + type + ".enabled", true)) {
            e.message(null);
            return;
        }
        e.message(null);
        Player p = e.getPlayer();
        Component name = Component.text(p.getName(), plugin.theme().color("player"));
        Component title = display.title().colorIfAbsent(plugin.theme().color("achievement"));
        String key = "achievement." + type;
        plugin.messages().broadcast(PlayerSetting.ACHIEVEMENTS, null, lang ->
                plugin.messages().component(lang, key,
                        Placeholders.create().put("player", name).put("achievement", title)));

        if (cfg.bool("achievement.sound.enabled", true)) {
            if ("all".equalsIgnoreCase(cfg.string("achievement.sound.scope"))) {
                for (Player o : Bukkit.getOnlinePlayers()) {
                    plugin.sounds().play(o, "achievement", PlayerSetting.ACHIEVEMENT_SOUNDS);
                }
            } else {
                plugin.sounds().play(p, "achievement", PlayerSetting.ACHIEVEMENT_SOUNDS);
            }
        }
    }
}
