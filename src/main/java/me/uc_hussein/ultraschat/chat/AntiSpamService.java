package me.uc_hussein.ultraschat.chat;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Small per-player delays. Bypass with ultraschat.bypass.antispam (chat only). */
public final class AntiSpamService {
    private final ULTRASChatPlugin plugin;
    private final Map<UUID, Long> chat = new ConcurrentHashMap<>();
    private final Map<UUID, Long> mention = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pm = new ConcurrentHashMap<>();

    public AntiSpamService(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean check(Map<UUID, Long> map, UUID id, String configKey) {
        if (!plugin.cfg().bool("anti-spam.enabled", true)) {
            return true;
        }
        long delayNanos = Math.max(0, plugin.cfg().integer("anti-spam." + configKey)) * 1_000_000L;
        long now = System.nanoTime();
        Long last = map.get(id);
        if (last != null && now - last < delayNanos) {
            return false;
        }
        map.put(id, now);
        return true;
    }

    public boolean chatAllowed(UUID id) {
        return check(chat, id, "chat-delay-ms");
    }

    public boolean mentionAllowed(UUID id) {
        return check(mention, id, "mention-delay-ms");
    }

    public boolean pmAllowed(UUID id) {
        return check(pm, id, "pm-delay-ms");
    }

    public void clear(UUID id) {
        chat.remove(id);
        mention.remove(id);
        pm.remove(id);
    }
}
