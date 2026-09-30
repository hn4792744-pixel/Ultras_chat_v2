package me.uc_hussein.ultraschat.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.chat.ChatFilter;
import me.uc_hussein.ultraschat.mention.MentionService;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Locale;
import java.util.UUID;

/**
 * Public chat. Only the chat event itself is touched:
 * <ul>
 *   <li>viewers are filtered per player (chat off, ignore list) - repeated at MONITOR so viewers added by
 *       other plugins are removed too, and also on the legacy recipients list;</li>
 *   <li>the format is applied according to chat.format-mode (AUTO / ALWAYS / NEVER);</li>
 *   <li>mentions are dispatched after the event finished.</li>
 * </ul>
 * Other plugins' own broadcasts are never intercepted.
 */
public final class ChatListener implements Listener {
    private final ULTRASChatPlugin plugin;
    private final ChatFilter filter;
    private volatile boolean formatActive = true;

    public ChatListener(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
        this.filter = new ChatFilter(plugin);
    }

    /** Decides whether the ULTRAS chat format is applied. Called after all plugins loaded and on reload. */
    public void refreshFormatMode() {
        String mode = plugin.cfg().string("chat.format-mode").toUpperCase(Locale.ROOT);
        boolean active;
        switch (mode) {
            case "NEVER" -> active = false;
            case "ALWAYS" -> active = true;
            case "AUTO" -> {
                active = true;
                for (String name : plugin.cfg().stringList("chat.known-chat-plugins")) {
                    if (Bukkit.getPluginManager().isPluginEnabled(name)) {
                        plugin.getLogger().info("Chat plugin '" + name + "' detected - ULTRAS chat format disabled (chat.format-mode: AUTO).");
                        active = false;
                    }
                }
            }
            default -> {
                plugin.config().warnings().add("config.yml: chat.format-mode must be AUTO, ALWAYS or NEVER - using AUTO");
                active = true;
            }
        }
        formatActive = active;
    }

    /** True if {@code viewer} must not see a public message written by {@code source}. */
    private boolean hidden(Player source, Audience viewer) {
        if (!(viewer instanceof Player v)) {
            return false;                                   // console etc. always receives chat
        }
        UUID vid = v.getUniqueId();
        if (vid.equals(source.getUniqueId())) {
            return !plugin.cfg().bool("chat.show-own-messages", true)
                    && plugin.cfg().bool("chat.player-toggle", true)
                    && !plugin.players().isEnabled(v, PlayerSetting.CHAT);
        }
        if (plugin.cfg().bool("chat.player-toggle", true) && !plugin.players().isEnabled(v, PlayerSetting.CHAT)) {
            return true;                                    // chat off: no player messages at all
        }
        return plugin.cfg().bool("ignore.enabled", true)
                && plugin.players().isIgnoring(vid, source.getUniqueId())
                && !source.hasPermission("ultraschat.ignore.exempt");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(e.message());
        boolean ultras = plugin.cfg().bool("messages.chat", true);

        if (ultras) {
            if (filter.blocks(plain)) {
                e.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.messages().error(p, "chat.blocked", null));
                return;
            }
            if (!p.hasPermission("ultraschat.bypass.antispam") && !plugin.antiSpam().chatAllowed(p.getUniqueId())) {
                e.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.messages().error(p, "general.spam", null));
                return;
            }
        }
        e.viewers().removeIf(a -> hidden(p, a));
        if (ultras && formatActive) {
            e.renderer((source, displayName, message, viewer) -> render(source, displayName, message, viewer));
        }
    }

    /** Second pass: removes viewers that plugins running after us added back. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChatFinal(AsyncChatEvent e) {
        Player p = e.getPlayer();
        e.viewers().removeIf(a -> hidden(p, a));

        if (plugin.cfg().bool("messages.mentions", true)) {
            String plain = PlainTextComponentSerializer.plainText().serialize(e.message());
            if (plain.indexOf('@') >= 0) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (p.isOnline()) {
                        plugin.mentions().process(p, plain);
                    }
                });
            }
        }
    }

    /** Plugins that still work with the legacy recipients list: apply the same filtering. */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLegacyChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        e.getRecipients().removeIf(r -> hidden(p, r));
    }

    private Component render(Player source, Component displayName, Component message, Audience viewer) {
        String lang = viewer instanceof Player v ? plugin.players().language(v.getUniqueId())
                : plugin.languages().defaultLanguage();
        Component name = displayName.colorIfAbsent(plugin.theme().color("player"));
        Component body = highlight(source, message).colorIfAbsent(plugin.theme().color("chatmsg"));
        return plugin.messages().render(lang, plugin.languages().text(lang, "chat.format"),
                Placeholders.create().put("player", name).put("message", body));
    }

    private Component highlight(Player source, Component message) {
        if (!plugin.mentions().canSend(source)) {
            return message;
        }
        var color = plugin.theme().color("mention");
        return message.replaceText(TextReplacementConfig.builder()
                .match(MentionService.PATTERN)
                .replacement((match, builder) ->
                        plugin.mentionIndex().contains(match.group(1).toLowerCase(Locale.ROOT)) ? builder.color(color) : builder)
                .build());
    }
}
