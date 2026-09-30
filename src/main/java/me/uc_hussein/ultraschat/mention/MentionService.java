package me.uc_hussein.ultraschat.mention;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.logging.ChatLog;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import me.uc_hussein.ultraschat.util.VanishUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects @name in chat and notifies the mentioned players (main thread only). */
public final class MentionService {
    public static final Pattern PATTERN = Pattern.compile("(?<![\\w])@(\\w{2,16})");

    private final ULTRASChatPlugin plugin;

    public MentionService(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    /** Whether {@code sender} may trigger mentions at all. */
    public boolean canSend(Player sender) {
        if (!plugin.cfg().bool("messages.mentions", true) || !plugin.cfg().bool("mentions.enabled", true)) {
            return false;
        }
        if (!sender.hasPermission("ultraschat.mention")) {
            return false;
        }
        return plugin.cfg().bool("mentions.from-everyone", true) || sender.hasPermission("ultraschat.mention.staff");
    }

    public void process(Player sender, String plainMessage) {
        if (!canSend(sender) || !plugin.antiSpam().mentionAllowed(sender.getUniqueId())) {
            return;
        }
        int max = Math.max(1, plugin.cfg().integer("mentions.max-per-message"));
        Set<String> names = new LinkedHashSet<>();
        Matcher m = PATTERN.matcher(plainMessage);
        while (m.find() && names.size() < max) {
            names.add(m.group(1).toLowerCase(Locale.ROOT));
        }
        for (String name : names) {
            Player target = Bukkit.getPlayerExact(name);
            if (target == null || target.equals(sender) || !VanishUtil.visibleTo(plugin, sender, target)) {
                continue;
            }
            if (!plugin.players().isEnabled(target, PlayerSetting.MENTIONS)
                    || (plugin.cfg().bool("ignore.enabled", true)
                    && plugin.players().isIgnoring(target.getUniqueId(), sender.getUniqueId())
                    && !sender.hasPermission("ultraschat.ignore.exempt"))) {
                continue;
            }
            if (plugin.cfg().bool("mentions.actionbar", true)
                    && plugin.players().isEnabled(target, PlayerSetting.MESSAGES)) {
                String lang = plugin.messages().lang(target);
                Component bar = plugin.messages().component(lang, "mention.actionbar",
                        Placeholders.create().put("sender", Component.text(sender.getName(), plugin.theme().color("mention"))));
                plugin.actionBars().show(target, bar);
            }
            if (plugin.cfg().bool("mentions.sound", true)) {
                plugin.sounds().play(target, "mention", PlayerSetting.MENTION_SOUNDS);
            }
            plugin.log().log(ChatLog.Category.MENTION, sender.getName() + " mentioned " + target.getName());
        }
    }
}
