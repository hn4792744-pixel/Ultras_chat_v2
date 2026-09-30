package me.uc_hussein.ultraschat.settings;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.storage.PlayerDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** In-memory player preferences (thread-safe reads) with debounced, crash-safe saving. */
public final class PlayerDataService {
    private final ULTRASChatPlugin plugin;
    private final Map<UUID, PlayerData> data = new ConcurrentHashMap<>();
    private final AtomicBoolean saveQueued = new AtomicBoolean();
    private PlayerDataStorage storage;

    public PlayerDataService(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        storage = new PlayerDataStorage(plugin);
        data.putAll(storage.read());
    }

    public void shutdown() {
        if (storage != null) {
            storage.writeAsync(storage.serialize(data.values()));
            storage.close();
        }
    }

    public boolean defaultOf(PlayerSetting s) {
        return plugin.cfg().bool("player-defaults." + s.key(), true);
    }

    public boolean isEnabled(UUID id, PlayerSetting s) {
        PlayerData d = data.get(id);
        Boolean v = d == null ? null : d.override(s);
        return v != null ? v : defaultOf(s);
    }

    public boolean isEnabled(Player p, PlayerSetting s) {
        return isEnabled(p.getUniqueId(), s);
    }

    private PlayerData dataFor(UUID id) {
        return data.computeIfAbsent(id, k -> {
            PlayerData d = new PlayerData(k);
            Player online = Bukkit.getPlayer(k);
            d.setName(online != null ? online.getName() : Bukkit.getOfflinePlayer(k).getName());
            return d;
        });
    }

    public void set(UUID id, PlayerSetting s, boolean value) {
        dataFor(id).setOverride(s, value);
        markDirty();
    }

    /** Flips the setting and returns the new value. */
    public boolean toggle(UUID id, PlayerSetting s) {
        boolean nv = !isEnabled(id, s);
        set(id, s, nv);
        return nv;
    }

    public String language(UUID id) {
        PlayerData d = data.get(id);
        String l = d == null ? null : d.language();
        return l != null ? l : plugin.languages().defaultLanguage();
    }

    public void setLanguage(UUID id, String lang) {
        dataFor(id).setLanguage(lang);
        markDirty();
    }

    /** Resets toggles and language to the defaults; the stored name is kept. */
    public void reset(UUID id) {
        PlayerData d = data.get(id);
        if (d != null) {
            d.clearOverrides();
            d.setLanguage(null);
            markDirty();
        }
    }

    /** True if {@code viewer} chose to ignore {@code source}. Safe to call from any thread. */
    public boolean isIgnoring(UUID viewer, UUID source) {
        PlayerData d = data.get(viewer);
        return d != null && d.ignored().contains(source);
    }

    /** Adds/removes {@code target} from the ignore list; returns true if now ignored. */
    public boolean toggleIgnore(UUID viewer, UUID target) {
        PlayerData d = dataFor(viewer);
        boolean now = d.ignored().add(target);
        if (!now) {
            d.ignored().remove(target);
        }
        markDirty();
        return now;
    }

    public java.util.Set<UUID> ignoredBy(UUID viewer) {
        PlayerData d = data.get(viewer);
        return d == null ? java.util.Set.of() : java.util.Set.copyOf(d.ignored());
    }

    /** Keeps the stored name current for players that already have data. */
    public void touch(Player p) {
        PlayerData d = data.get(p.getUniqueId());
        if (d != null && !p.getName().equals(d.name())) {
            d.setName(p.getName());
            markDirty();
        }
    }

    /** Finds a stored player by name (used by /chat setting for offline players). */
    public UUID findByName(String name) {
        for (PlayerData d : data.values()) {
            if (d.name() != null && d.name().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) {
                return d.id();
            }
        }
        return null;
    }

    public String nameOf(UUID id) {
        PlayerData d = data.get(id);
        if (d != null && d.name() != null) {
            return d.name();
        }
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n != null ? n : id.toString();
    }

    private void markDirty() {
        if (!plugin.isEnabled() || storage == null || !saveQueued.compareAndSet(false, true)) {
            return;
        }
        long delay = Math.max(1, plugin.cfg().integer("storage.save-delay-ticks"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveQueued.set(false);
            storage.writeAsync(storage.serialize(data.values()));
        }, delay);
    }
}
