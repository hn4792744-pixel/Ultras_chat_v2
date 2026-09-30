package me.uc_hussein.ultraschat.notification;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import me.uc_hussein.ultraschat.util.SmallCaps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Renders templates from the language files and delivers them respecting per-player settings. */
public final class MessageService {
    private final ULTRASChatPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MessageService(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    public String lang(CommandSender s) {
        return s instanceof Player p ? plugin.players().language(p.getUniqueId()) : plugin.languages().defaultLanguage();
    }

    /** Renders a MiniMessage template. %key% placeholders become components (never parsed as markup). */
    public Component render(String lang, String template, Placeholders ph) {
        String t = template;
        if (plugin.cfg().bool("style.small-caps", true) && plugin.languages().smallCaps(lang)) {
            t = SmallCaps.convertTemplate(t);
        }
        Theme theme = plugin.theme();
        if (!theme.prefixEnabled()) {
            t = t.replace("<prefix> ", "").replace("<prefix>", "");
        }
        TagResolver.Builder rb = TagResolver.builder().resolver(theme.tags());
        if (theme.prefixEnabled()) {
            rb.resolver(Placeholder.component("prefix", theme.prefix()));
        }
        if (ph != null) {
            for (Map.Entry<String, Component> e : ph.map().entrySet()) {
                t = t.replace("%" + e.getKey() + "%", "<" + e.getKey() + ">");
                rb.resolver(Placeholder.component(e.getKey(), e.getValue()));
            }
        }
        return mm.deserialize(t, rb.build());
    }

    public Component component(String lang, String key, Placeholders ph) {
        return render(lang, plugin.languages().text(lang, key), ph);
    }

    /** Plain text from a language key (small caps applied when the language uses them). */
    public String plainText(String lang, String key) {
        String s = plugin.languages().text(lang, key);
        return plugin.cfg().bool("style.small-caps", true) && plugin.languages().smallCaps(lang) ? SmallCaps.convert(s) : s;
    }

    public Component settingLabel(String lang, PlayerSetting s) {
        return Component.text(plainText(lang, "gui.setting." + s.key() + ".label"));
    }

    /** Confirmation-style message: hidden when the player disabled plugin messages (or all messages). */
    public void feedback(CommandSender to, String key, Placeholders ph) {
        if (to instanceof Player p) {
            if (!plugin.players().isEnabled(p, PlayerSetting.MESSAGES)
                    || !plugin.players().isEnabled(p, PlayerSetting.PLUGIN_MESSAGES)) {
                return;
            }
        }
        to.sendMessage(component(lang(to), key, ph));
    }

    /** Direct answer to a command (usage, lists, admin results): always shown, no error sound. */
    public void reply(CommandSender to, String key, Placeholders ph) {
        to.sendMessage(component(lang(to), key, ph));
    }

    /** Errors and warnings are always shown. */
    public void error(CommandSender to, String key, Placeholders ph) {
        to.sendMessage(component(lang(to), key, ph));
        if (to instanceof Player p) {
            plugin.sounds().play(p, "error", null);
        }
    }

    /**
     * Sends an event message to every player who enabled {@code category} (and the master "messages" switch),
     * each in their own language, and to the console in the default language.
     */
    public void broadcast(PlayerSetting category, UUID exclude, Function<String, Component> maker) {
        Map<String, Component> cache = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(exclude)) {
                continue;
            }
            if (!plugin.players().isEnabled(p, PlayerSetting.MESSAGES) || !plugin.players().isEnabled(p, category)) {
                continue;
            }
            p.sendMessage(cache.computeIfAbsent(lang(p), maker));
        }
        Bukkit.getConsoleSender().sendMessage(cache.computeIfAbsent(plugin.languages().defaultLanguage(), maker));
    }
}
