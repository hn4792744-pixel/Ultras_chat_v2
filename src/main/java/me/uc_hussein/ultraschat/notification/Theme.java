package me.uc_hussein.ultraschat.notification;

import me.uc_hussein.ultraschat.config.Cfg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Colors from config.yml, the ULTRAS prefix and the tag resolver that makes colors usable as tags. */
public final class Theme {
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern TAG_NAME = Pattern.compile("^[a-z0-9_]+$");
    /** Names used as placeholders; they must not also be color tags. */
    private static final Set<String> RESERVED = Set.of("killer", "victim", "player", "sender", "receiver",
            "target", "message", "achievement", "world", "language", "status", "setting", "count", "prefix", "list", "text", "usage");

    private final MiniMessage mm = MiniMessage.miniMessage();
    private volatile Map<String, TextColor> colors = Map.of();
    private volatile TagResolver tags = TagResolver.empty();
    private volatile Component prefix = Component.empty();
    private volatile boolean prefixEnabled = true;

    public void reload(Cfg cfg, List<String> warnings) {
        Map<String, TextColor> fresh = new HashMap<>();
        TagResolver.Builder tb = TagResolver.builder();
        for (String key : cfg.keys("colors")) {
            TextColor c = parseColor(cfg.string("colors." + key), null);
            if (c == null) {
                warnings.add("config.yml: colors." + key + " is not a valid color - using white");
                c = NamedTextColor.WHITE;
            }
            fresh.put(key, c);
            if (TAG_NAME.matcher(key).matches() && !RESERVED.contains(key)) {
                tb.resolver(TagResolver.resolver(key, Tag.styling(c)));
            }
        }
        colors = fresh;
        tags = tb.build();

        prefixEnabled = cfg.bool("prefix.enabled", true);
        String text = cfg.string("prefix.text");
        String sep = cfg.string("prefix.separator");
        String start = cfg.string("prefix.gradient.start");
        String end = cfg.string("prefix.gradient.end");
        if (!HEX.matcher(start).matches()) {
            warnings.add("config.yml: prefix.gradient.start must look like #rrggbb - using #ff3b30");
            start = "#ff3b30";
        }
        if (!HEX.matcher(end).matches()) {
            warnings.add("config.yml: prefix.gradient.end must look like #rrggbb - using #8b0000");
            end = "#8b0000";
        }
        // Regular (never small caps) text; the prefix is a finished component, so the converter never sees it.
        String grad = cfg.bool("prefix.gradient.enabled", true)
                ? "<gradient:" + start + ":" + end + ">%s</gradient>"
                : "<" + start + ">%s";
        String built = grad.formatted(text) + " " + grad.formatted(sep);
        prefix = mm.deserialize(built, tags);
    }

    private static TextColor parseColor(String s, TextColor fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        if (s.startsWith("#")) {
            TextColor c = TextColor.fromHexString(s);
            return c != null ? c : fallback;
        }
        NamedTextColor n = NamedTextColor.NAMES.value(s.toLowerCase().replace(' ', '_'));
        return n != null ? n : fallback;
    }

    public TextColor color(String name) {
        return colors.getOrDefault(name, NamedTextColor.WHITE);
    }

    public TagResolver tags() {
        return tags;
    }

    public Component prefix() {
        return prefix;
    }

    public boolean prefixEnabled() {
        return prefixEnabled;
    }
}
