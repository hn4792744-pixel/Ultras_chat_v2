package me.uc_hussein.ultraschat.private_message;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.logging.ChatLog;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import me.uc_hussein.ultraschat.util.VanishUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** /msg and /reply. Only online players can be messaged (no mail system). */
public final class PrivateMessageService {
    private final ULTRASChatPlugin plugin;
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();

    public PrivateMessageService(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(Player sender, String targetName, String message) {
        var msgs = plugin.messages();
        if (!plugin.cfg().bool("messages.private-messages", true)) {
            msgs.error(sender, "general.feature-disabled", null);
            return;
        }
        if (!sender.hasPermission("ultraschat.msg")) {
            msgs.error(sender, "general.no-permission", null);
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !VanishUtil.visibleTo(plugin, sender, target)) {
            msgs.error(sender, "pm.not-found", null);
            return;
        }
        deliver(sender, target, message);
    }

    public void reply(Player sender, String message) {
        UUID partner = lastPartner.get(sender.getUniqueId());
        Player target = partner == null ? null : Bukkit.getPlayer(partner);
        if (target == null || !VanishUtil.visibleTo(plugin, sender, target)) {
            plugin.messages().error(sender, "pm.no-reply", null);
            return;
        }
        send(sender, target.getName(), message);
    }

    /**
     * A private message is refused if the receiver turned private messages off, turned public chat off
     * (chat.hide-private-messages) or ignores the sender. ultraschat.msg.bypass overrides all three.
     * The sender always gets the same neutral "not receiving messages" answer.
     */
    private boolean blocked(Player sender, Player target) {
        if (sender.hasPermission("ultraschat.msg.bypass")) {
            return false;
        }
        if (!plugin.players().isEnabled(target, PlayerSetting.PRIVATE_MESSAGES)) {
            return true;
        }
        if (plugin.cfg().bool("chat.hide-private-messages", true)
                && plugin.cfg().bool("chat.player-toggle", true)
                && !plugin.players().isEnabled(target, PlayerSetting.CHAT)) {
            return true;
        }
        return plugin.cfg().bool("ignore.enabled", true)
                && plugin.players().isIgnoring(target.getUniqueId(), sender.getUniqueId());
    }

    private void deliver(Player sender, Player target, String message) {
        var msgs = plugin.messages();
        if (sender.equals(target)) {
            msgs.error(sender, "pm.self", null);
            return;
        }
        if (blocked(sender, target)) {
            msgs.error(sender, "pm.disabled",
                    Placeholders.create().put("receiver", Component.text(target.getName(), plugin.theme().color("receiver"))));
            return;
        }
        if (!plugin.antiSpam().pmAllowed(sender.getUniqueId())) {
            msgs.error(sender, "general.spam", null);
            return;
        }
        Component body = Component.text(message, plugin.theme().color("chatmsg"));
        Component senderName = Component.text(sender.getName(), plugin.theme().color("sender"));
        Component receiverName = Component.text(target.getName(), plugin.theme().color("receiver"));

        sender.sendMessage(msgs.component(msgs.lang(sender), "pm.to",
                Placeholders.create().put("receiver", receiverName).put("message", body)));
        String targetLang = msgs.lang(target);
        target.sendMessage(msgs.component(targetLang, "pm.from",
                Placeholders.create().put("sender", senderName).put("message", body)));

        lastPartner.put(sender.getUniqueId(), target.getUniqueId());
        lastPartner.put(target.getUniqueId(), sender.getUniqueId());

        if (plugin.cfg().bool("private.actionbar", true) && plugin.players().isEnabled(target, PlayerSetting.MESSAGES)) {
            plugin.actionBars().show(target, msgs.component(targetLang, "pm.actionbar",
                    Placeholders.create().put("sender", senderName)));
        }
        plugin.sounds().play(target, "private-message", PlayerSetting.PRIVATE_SOUNDS);
        // content is intentionally never logged
        plugin.log().log(ChatLog.Category.PM, sender.getName() + " -> " + target.getName());
    }

    public void forget(UUID id) {
        lastPartner.remove(id);
    }
}
