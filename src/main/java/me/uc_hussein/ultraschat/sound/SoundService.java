package me.uc_hussein.ultraschat.sound;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.config.Cfg;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Plays configured sounds, honoring the global switch, the event switch and the player's own toggles. */
public final class SoundService {
    private record Spec(boolean enabled, Key key, float volume, float pitch) {
    }

    private final ULTRASChatPlugin plugin;
    private volatile Map<String, Spec> specs = Map.of();
    private volatile boolean globalEnabled = true;

    public SoundService(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload(Cfg cfg, List<String> warnings) {
        Map<String, Spec> fresh = new HashMap<>();
        for (String id : cfg.keys("sounds.events")) {
            String p = "sounds.events." + id + ".";
            boolean enabled = cfg.bool(p + "enabled", true);
            String raw = cfg.string(p + "sound");
            Key key = null;
            try {
                key = Key.key(raw);
            } catch (RuntimeException ex) {
                warnings.add("config.yml: sounds.events." + id + ".sound '" + raw + "' is not a valid sound key - disabled");
                enabled = false;
            }
            float volume = (float) Math.max(0D, Math.min(10D, cfg.dbl(p + "volume")));
            float pitch = (float) Math.max(0.5D, Math.min(2D, cfg.dbl(p + "pitch")));
            fresh.put(id, new Spec(enabled, key, volume, pitch));
        }
        specs = fresh;
        globalEnabled = cfg.bool("sounds.enabled", true);
    }

    /**
     * @param category extra per-player toggle that must be on (may be null)
     */
    public void play(Player p, String eventId, PlayerSetting category) {
        if (!globalEnabled || !p.isOnline()) {
            return;
        }
        Spec s = specs.get(eventId);
        if (s == null || !s.enabled() || s.key() == null) {
            return;
        }
        if (!plugin.players().isEnabled(p, PlayerSetting.SOUNDS)) {
            return;
        }
        if (category != null && !plugin.players().isEnabled(p, category)) {
            return;
        }
        p.playSound(Sound.sound(s.key(), Sound.Source.MASTER, s.volume(), s.pitch()));
    }
}
