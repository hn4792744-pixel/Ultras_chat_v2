package me.uc_hussein.ultraschat.config;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.language.LanguageManager;

import java.util.ArrayList;
import java.util.List;

/** Owns config.yml and the shared list of configuration warnings. */
public final class ConfigService {
    private final ULTRASChatPlugin plugin;
    private final List<String> warnings = new ArrayList<>();
    private volatile Cfg cfg;

    public ConfigService(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        warnings.clear();
        cfg = Cfg.load(plugin, "config.yml", warnings);
        if (LanguageManager.normalize(cfg.string("language")) == null) {
            warnings.add("config.yml: 'language' must be en or ar - using en");
        }
    }

    public Cfg cfg() {
        return cfg;
    }

    public List<String> warnings() {
        return warnings;
    }

    public String defaultLanguage() {
        Cfg c = cfg;
        String l = c == null ? null : LanguageManager.normalize(c.string("language"));
        return l == null ? "en" : l;
    }
}
