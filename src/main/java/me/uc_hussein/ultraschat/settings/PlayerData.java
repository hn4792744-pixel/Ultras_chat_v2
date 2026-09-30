package me.uc_hussein.ultraschat.settings;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stored preferences of one player. Only explicitly changed settings are stored. */
public final class PlayerData {
    private final UUID id;
    private volatile String name;
    private volatile String language;
    private final Map<PlayerSetting, Boolean> overrides = new ConcurrentHashMap<>();
    private final Set<UUID> ignored = ConcurrentHashMap.newKeySet();

    public PlayerData(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String language() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean override(PlayerSetting s) {
        return overrides.get(s);
    }

    public void setOverride(PlayerSetting s, boolean v) {
        overrides.put(s, v);
    }

    public Map<PlayerSetting, Boolean> overrides() {
        return overrides;
    }

    /** Players this player does not want to see (kept when settings are reset). */
    public Set<UUID> ignored() {
        return ignored;
    }

    public void clearOverrides() {
        overrides.clear();
    }
}
