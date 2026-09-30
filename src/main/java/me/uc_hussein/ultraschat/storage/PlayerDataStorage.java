package me.uc_hussein.ultraschat.storage;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.language.LanguageManager;
import me.uc_hussein.ultraschat.settings.PlayerData;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * player-data.yml persistence. Writes go through one ordered background thread and are
 * atomic (temp file + move), so a crash cannot leave a half-written file.
 * YAML was chosen over SQLite: the data is tiny, human-readable and needs no driver.
 */
public final class PlayerDataStorage {
    private final ULTRASChatPlugin plugin;
    private final File file;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ULTRAS-Chat-IO");
        t.setDaemon(true);
        return t;
    });

    public PlayerDataStorage(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-data.yml");
    }

    public Map<UUID, PlayerData> read() {
        Map<UUID, PlayerData> out = new HashMap<>();
        if (!file.exists()) {
            return out;
        }
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.load(file);
        } catch (IOException | InvalidConfigurationException ex) {
            File backup = new File(plugin.getDataFolder(), "player-data.corrupt-" + System.currentTimeMillis() + ".yml");
            plugin.log().error("player-data.yml is unreadable; moved to " + backup.getName(), ex);
            try {
                Files.move(file.toPath(), backup.toPath());
            } catch (IOException moveEx) {
                plugin.log().error("Could not back up the corrupt player-data.yml", moveEx);
            }
            return out;
        }
        ConfigurationSection players = y.getConfigurationSection("players");
        if (players == null) {
            return out;
        }
        for (String key : players.getKeys(false)) {
            ConfigurationSection s = players.getConfigurationSection(key);
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (s == null) {
                continue;
            }
            PlayerData d = new PlayerData(id);
            d.setName(s.getString("name"));
            d.setLanguage(LanguageManager.normalize(s.getString("language")));
            for (PlayerSetting ps : PlayerSetting.values()) {
                String p = "settings." + ps.key();
                if (s.isBoolean(p)) {
                    d.setOverride(ps, s.getBoolean(p));
                }
            }
            for (String raw : s.getStringList("ignored")) {
                try {
                    d.ignored().add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignoredEx) {
                    // skip broken entry
                }
            }
            out.put(id, d);
        }
        return out;
    }

    /** Builds the YAML text. Call on the main thread. */
    public String serialize(Collection<PlayerData> all) {
        YamlConfiguration y = new YamlConfiguration();
        for (PlayerData d : all) {
            String base = "players." + d.id() + ".";
            if (d.name() != null) {
                y.set(base + "name", d.name());
            }
            if (d.language() != null) {
                y.set(base + "language", d.language());
            }
            for (Map.Entry<PlayerSetting, Boolean> e : d.overrides().entrySet()) {
                y.set(base + "settings." + e.getKey().key(), e.getValue());
            }
            if (!d.ignored().isEmpty()) {
                java.util.List<String> ids = new java.util.ArrayList<>();
                for (UUID u : d.ignored()) {
                    ids.add(u.toString());
                }
                y.set(base + "ignored", ids);
            }
        }
        return y.saveToString();
    }

    public void writeAsync(String yaml) {
        try {
            writer.execute(() -> write(yaml));
        } catch (RejectedExecutionException ex) {
            write(yaml);
        }
    }

    private void write(String yaml) {
        Path target = file.toPath();
        Path tmp = new File(plugin.getDataFolder(), "player-data.yml.tmp").toPath();
        try {
            Files.writeString(tmp, yaml, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.log().error("Could not save player-data.yml", ex);
        }
    }

    public void close() {
        writer.shutdown();
        try {
            writer.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
