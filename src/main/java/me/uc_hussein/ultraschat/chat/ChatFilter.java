package me.uc_hussein.ultraschat.chat;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;

import java.util.List;
import java.util.Locale;

/** Optional word filter. Disabled by default (chat-filter.enabled: false). */
public final class ChatFilter {
    private final ULTRASChatPlugin plugin;

    public ChatFilter(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean blocks(String plain) {
        if (!plugin.cfg().bool("chat-filter.enabled", false)) {
            return false;
        }
        List<String> words = plugin.cfg().stringList("chat-filter.words");
        String lower = plain.toLowerCase(Locale.ROOT);
        for (String w : words) {
            if (!w.isBlank() && lower.contains(w.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
