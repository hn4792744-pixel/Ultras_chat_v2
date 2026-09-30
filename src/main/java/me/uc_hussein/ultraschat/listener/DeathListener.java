package me.uc_hussein.ultraschat.listener;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.config.Cfg;
import me.uc_hussein.ultraschat.death.DeathInfo;
import me.uc_hussein.ultraschat.death.DeathResolver;
import me.uc_hussein.ultraschat.logging.ChatLog;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import me.uc_hussein.ultraschat.util.VanishUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Replaces the vanilla death message with the ULTRAS one (per viewer language / settings). */
public final class DeathListener implements Listener {
    private final ULTRASChatPlugin plugin;
    private final DeathResolver resolver = new DeathResolver();

    public DeathListener(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        try {
            handle(e);
        } catch (RuntimeException ex) {
            plugin.log().error("Death handling failed", ex);
        }
    }

    private void handle(PlayerDeathEvent e) {
        Cfg cfg = plugin.cfg();
        Player victim = e.getEntity();
        Component original = e.deathMessage();

        if (!cfg.bool("messages.death", true)) {
            if (!cfg.bool("fallback.show-vanilla-message-when-disabled")) {
                e.deathMessage(null);
            }
            return;
        }
        DeathInfo info = resolver.resolve(victim);
        String key = info.type().key();
        if (VanishUtil.hidesEvents(plugin, victim)) {
            e.deathMessage(null);           // vanished players never announce anything
            return;
        }
        if (!cfg.bool("death.types." + key + ".enabled", true)) {
            // disabled type: no ULTRAS message; vanilla only if death.disabled-type-shows-vanilla is true
            if (!cfg.bool("death.disabled-type-shows-vanilla", false)) {
                e.deathMessage(null);
            }
            return;
        }
        if (original == null && cfg.bool("death.respect-suppressed-message", true)) {
            return;                          // another plugin already suppressed it
        }
        e.deathMessage(null);

        boolean random = cfg.bool("death.types." + key + ".random", true);
        int roll = ThreadLocalRandom.current().nextInt(1000);
        Component victimName = Component.text(victim.getName(), plugin.theme().color("victim"));
        Component killerName = killerName(info.killer());

        plugin.messages().broadcast(PlayerSetting.DEATH, null, lang -> {
            List<String> options = plugin.languages().list(lang, "death." + key);
            if (options.isEmpty()) {
                options = plugin.languages().list("en", "death.other");
            }
            String template = options.isEmpty() ? "<prefix> %victim%"
                    : options.get(random ? roll % options.size() : 0);
            return plugin.messages().render(lang, template,
                    Placeholders.create().put("victim", victimName).put("killer", killerName));
        });

        if (cfg.bool("death.sound.enabled", true)) {
            if (cfg.bool("death.sound.victim", true)) {
                plugin.sounds().play(victim, "death-victim", PlayerSetting.DEATH_SOUNDS);
            }
            if (cfg.bool("death.sound.broadcast", true)) {
                boolean worldOnly = "world".equalsIgnoreCase(cfg.string("death.sound.scope"));
                for (Player o : Bukkit.getOnlinePlayers()) {
                    if (o.equals(victim) || (worldOnly && !o.getWorld().equals(victim.getWorld()))) {
                        continue;
                    }
                    plugin.sounds().play(o, "death-broadcast", PlayerSetting.DEATH_SOUNDS);
                }
            }
        }
        sendLocation(victim);
        plugin.log().log(ChatLog.Category.DEATH, victim.getName() + " died (" + key + ")");
    }

    /** Private hint for the victim only: where they died. */
    private void sendLocation(Player victim) {
        if (!plugin.cfg().bool("death.show-location", true)
                || !plugin.players().isEnabled(victim, PlayerSetting.MESSAGES)
                || !plugin.players().isEnabled(victim, PlayerSetting.DEATH)) {
            return;
        }
        var loc = victim.getLocation();
        String coords = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
        victim.sendMessage(plugin.messages().component(plugin.messages().lang(victim), "death.location",
                Placeholders.create().text("location", coords).text("world", victim.getWorld().getName())));
    }

    private Component killerName(Entity killer) {
        if (killer == null) {
            return Component.text("?");
        }
        if (killer instanceof Player p) {
            return Component.text(p.getName(), plugin.theme().color("killer"));
        }
        return killer.name().colorIfAbsent(plugin.theme().color("killer"));
    }
}
