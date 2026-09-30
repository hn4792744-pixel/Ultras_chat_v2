package me.uc_hussein.ultraschat.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A YAML file on disk backed by the bundled default of the same name.
 * Missing or wrongly typed values silently fall back to the bundled default
 * (wrong types also add a warning). A broken file never throws.
 */
public final class Cfg {
    private final String name;
    private final YamlConfiguration disk;
    private final YamlConfiguration defaults;
    private final List<String> warnings;

    private Cfg(String name, YamlConfiguration disk, YamlConfiguration defaults, List<String> warnings) {
        this.name = name;
        this.disk = disk;
        this.defaults = defaults;
        this.warnings = warnings;
    }

    /** Loads {@code resource} from the data folder (copying the default first if absent). */
    public static Cfg load(JavaPlugin plugin, String resource, List<String> warnings) {
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists() && plugin.getResource(resource) != null) {
            plugin.saveResource(resource, false);
        }
        YamlConfiguration disk = new YamlConfiguration();
        if (file.exists()) {
            try {
                disk.load(file);
            } catch (IOException | InvalidConfigurationException ex) {
                disk = new YamlConfiguration();
                String first = String.valueOf(ex.getMessage()).split("\n")[0];
                String msg = resource + ": could not be read (" + first + ") - using built-in defaults";
                warnings.add(msg);
                plugin.getLogger().severe(msg);
            }
        }
        YamlConfiguration defaults = new YamlConfiguration();
        try (InputStream in = plugin.getResource(resource)) {
            if (in != null) {
                defaults.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe("Bundled resource " + resource + " is invalid: " + ex.getMessage());
        }
        // Files from an older plugin version are backed up and replaced, so changed defaults reach the server.
        int bundledVersion = defaults.getInt("file-version", 0);
        if (file.exists() && bundledVersion > disk.getInt("file-version", 0)) {
            File backup = new File(file.getParentFile(), file.getName() + ".old-" + System.currentTimeMillis());
            try {
                java.nio.file.Files.move(file.toPath(), backup.toPath());
                plugin.saveResource(resource, false);
                disk = new YamlConfiguration();
                disk.load(file);
                String msg = resource + " was updated to version " + bundledVersion + " (old file saved as " + backup.getName() + ")";
                warnings.add(msg);
                plugin.getLogger().warning(msg);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe("Could not update " + resource + ": " + ex.getMessage());
            }
        }
        return new Cfg(resource, disk, defaults, warnings);
    }

    private void warn(String path, String problem) {
        String m = name + ": '" + path + "' " + problem + " - using default";
        if (!warnings.contains(m)) {
            warnings.add(m);
        }
    }

    public void addWarning(String text) {
        String m = name + ": " + text;
        if (!warnings.contains(m)) {
            warnings.add(m);
        }
    }

    public boolean has(String path) {
        return disk.contains(path) || defaults.contains(path);
    }

    /** Raw value (disk first, then bundled default). */
    public Object raw(String path) {
        Object v = disk.get(path);
        return v != null ? v : defaults.get(path);
    }

    public String string(String path) {
        Object v = disk.get(path);
        if (v instanceof String || v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        if (v != null && !(v instanceof ConfigurationSection)) {
            warn(path, "expected text");
        }
        Object d = defaults.get(path);
        return d == null ? "" : String.valueOf(d);
    }

    public boolean bool(String path) {
        return bool(path, false);
    }

    public boolean bool(String path, boolean fallback) {
        Object v = disk.get(path);
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof String s && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"))) {
            return Boolean.parseBoolean(s);
        }
        if (v != null) {
            warn(path, "expected true/false");
        }
        Object d = defaults.get(path);
        return d instanceof Boolean b ? b : fallback;
    }

    public int integer(String path) {
        Object v = disk.get(path);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        if (v != null) {
            warn(path, "expected a whole number");
        }
        Object d = defaults.get(path);
        return d instanceof Number n ? n.intValue() : 0;
    }

    public double dbl(String path) {
        Object v = disk.get(path);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        if (v != null) {
            warn(path, "expected a number");
        }
        Object d = defaults.get(path);
        return d instanceof Number n ? n.doubleValue() : 0D;
    }

    public List<String> stringList(String path) {
        Object v = disk.get(path);
        if (!(v instanceof List<?>)) {
            if (v != null) {
                warn(path, "expected a list");
            }
            v = defaults.get(path);
        }
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> l) {
            for (Object o : l) {
                out.add(String.valueOf(o));
            }
        }
        return out;
    }

    /** Child keys of a section: from disk if the section exists there, else from the defaults. */
    public Set<String> keys(String path) {
        ConfigurationSection s = path.isEmpty() ? disk : disk.getConfigurationSection(path);
        if (s == null || s.getKeys(false).isEmpty()) {
            s = path.isEmpty() ? defaults : defaults.getConfigurationSection(path);
        }
        return s == null ? new LinkedHashSet<>() : new LinkedHashSet<>(s.getKeys(false));
    }

    /** All non-section keys from disk and bundled default. */
    public Set<String> leafKeys() {
        Set<String> out = new LinkedHashSet<>();
        for (YamlConfiguration y : new YamlConfiguration[]{defaults, disk}) {
            for (String k : y.getKeys(true)) {
                if (!(y.get(k) instanceof ConfigurationSection)) {
                    out.add(k);
                }
            }
        }
        return out;
    }
}
