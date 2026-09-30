package me.uc_hussein.ultraschat.settings;

import java.util.Locale;

/** Every per-player toggle. The key is used in commands, storage, GUI and config defaults. */
public enum PlayerSetting {
    CHAT("chat", "ultraschat.chat"),
    PLUGIN_MESSAGES("plugin-messages", "ultraschat.use"),
    MESSAGES("messages", "ultraschat.use"),
    SOUNDS("sounds", "ultraschat.use"),
    DEATH("death", "ultraschat.use"),
    DEATH_SOUNDS("death-sounds", "ultraschat.use"),
    JOIN_QUIT("join-quit", "ultraschat.use"),
    JOIN_QUIT_SOUNDS("join-quit-sounds", "ultraschat.use"),
    ACHIEVEMENTS("achievements", "ultraschat.use"),
    ACHIEVEMENT_SOUNDS("achievement-sounds", "ultraschat.use"),
    PRIVATE_MESSAGES("msg", "ultraschat.msg"),
    PRIVATE_SOUNDS("msg-sounds", "ultraschat.msg"),
    MENTIONS("mentions", "ultraschat.mention"),
    MENTION_SOUNDS("mention-sounds", "ultraschat.mention");

    private final String key;
    private final String permission;

    PlayerSetting(String key, String permission) {
        this.key = key;
        this.permission = permission;
    }

    public String key() {
        return key;
    }

    public String permission() {
        return permission;
    }

    /** Accepts the canonical key and a few friendly aliases. */
    public static PlayerSetting fromKey(String raw) {
        if (raw == null) {
            return null;
        }
        String k = raw.toLowerCase(Locale.ROOT);
        switch (k) {
            case "death-messages" -> k = "death";
            case "join", "quit" -> k = "join-quit";
            case "join-sounds", "quit-sounds" -> k = "join-quit-sounds";
            case "pm", "private", "private-messages" -> k = "msg";
            case "pm-sounds", "private-sounds" -> k = "msg-sounds";
            case "achievement", "advancements" -> k = "achievements";
            case "plugin", "feedback" -> k = "plugin-messages";
            default -> { }
        }
        for (PlayerSetting s : values()) {
            if (s.key.equals(k)) {
                return s;
            }
        }
        return null;
    }
}
