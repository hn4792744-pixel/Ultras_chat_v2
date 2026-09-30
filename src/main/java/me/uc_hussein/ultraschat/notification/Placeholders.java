package me.uc_hussein.ultraschat.notification;

import net.kyori.adventure.text.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Values for %placeholders% in message templates. Values are components, so user text is never parsed as markup. */
public final class Placeholders {
    private final Map<String, Component> values = new LinkedHashMap<>();

    public static Placeholders create() {
        return new Placeholders();
    }

    public Placeholders put(String key, Component value) {
        values.put(key, value);
        return this;
    }

    public Placeholders text(String key, String value) {
        values.put(key, Component.text(value));
        return this;
    }

    Map<String, Component> map() {
        return values;
    }
}
