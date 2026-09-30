package me.uc_hussein.ultraschat.language;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.config.Cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Loads the split language files (messages/*_en.yml, *_ar.yml) into one lookup per language. */
public final class LanguageManager {
    public static final List<String> SUPPORTED = List.of("en", "ar");
    private static final List<String> FILES =
            List.of("messages", "death", "join", "chat", "private", "achievement", "settings");

    private final ULTRASChatPlugin plugin;
    private volatile Map<String, Map<String, Object>> bundles = Map.of();
    private final Set<String> missingLogged = ConcurrentHashMap.newKeySet();

    public LanguageManager(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    /** Returns "en"/"ar" for accepted spellings, otherwise null. */
    public static String normalize(String s) {
        if (s == null) {
            return null;
        }
        String l = s.trim().toLowerCase(Locale.ROOT);
        return switch (l) {
            case "en", "english", "en_us", "en-us" -> "en";
            case "ar", "arabic", "ar_sa", "ar-sa", "عربي", "العربية" -> "ar";
            default -> null;
        };
    }

    public void reload(List<String> warnings) {
        Map<String, Map<String, Object>> fresh = new HashMap<>();
        for (String lang : SUPPORTED) {
            Map<String, Object> flat = new HashMap<>();
            for (String file : FILES) {
                Cfg c = Cfg.load(plugin, "messages/" + file + "_" + lang + ".yml", warnings);
                for (String key : c.leafKeys()) {
                    Object v = c.raw(key);
                    if (v != null) {
                        flat.put(key, v);
                    }
                }
            }
            fresh.put(lang, flat);
        }
        bundles = fresh;
        missingLogged.clear();
    }

    private Object lookup(String lang, String key) {
        Map<String, Object> b = bundles.get(lang);
        Object v = b == null ? null : b.get(key);
        if (v == null) {
            b = bundles.get("en");
            v = b == null ? null : b.get(key);
        }
        return v;
    }

    public String text(String lang, String key) {
        Object v = lookup(lang, key);
        if (v instanceof List<?> l) {
            return l.isEmpty() ? key : String.valueOf(l.get(0));
        }
        if (v != null) {
            return String.valueOf(v);
        }
        if (missingLogged.add(key)) {
            plugin.getLogger().warning("Missing language key: " + key);
        }
        return key;
    }

    public List<String> list(String lang, String key) {
        Object v = lookup(lang, key);
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> l) {
            for (Object o : l) {
                out.add(String.valueOf(o));
            }
        } else if (v != null) {
            out.add(String.valueOf(v));
        }
        return out;
    }

    public boolean smallCaps(String lang) {
        Map<String, Object> b = bundles.get(lang);
        Object v = b == null ? null : b.get("meta.small-caps");
        return v instanceof Boolean bool && bool;
    }

    public String displayName(String lang) {
        Map<String, Object> b = bundles.get(lang);
        Object v = b == null ? null : b.get("meta.name");
        return v == null ? lang : String.valueOf(v);
    }

    public String next(String lang) {
        int i = SUPPORTED.indexOf(lang);
        return SUPPORTED.get((i + 1) % SUPPORTED.size());
    }

    public String defaultLanguage() {
        return plugin.config().defaultLanguage();
    }
}
