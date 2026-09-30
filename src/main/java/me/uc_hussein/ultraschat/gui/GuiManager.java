package me.uc_hussein.ultraschat.gui;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.config.Cfg;
import me.uc_hussein.ultraschat.logging.ChatLog;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Loads gui/*.yml, builds inventories and handles clicks. All methods run on the main thread. */
public final class GuiManager {
    private final ULTRASChatPlugin plugin;
    private volatile Map<String, GuiPage> pages = Map.of();
    private volatile long generation = 0;

    public GuiManager(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ loading

    public void reload(List<String> warnings) {
        Map<String, GuiPage> fresh = new HashMap<>();
        for (String file : List.of("settings", "confirm-reset", "admin-settings")) {
            Cfg c = Cfg.load(plugin, "gui/" + file + ".yml", warnings);
            fresh.put(file + "/main", parsePage(c, file, "main", ""));
        }
        Cfg ps = Cfg.load(plugin, "gui/player-settings.yml", warnings);
        for (String id : ps.keys("pages")) {
            fresh.put("player-settings/" + id, parsePage(ps, "player-settings", id, "pages." + id + "."));
        }
        generation++;
        pages = fresh;
        closeStale();
    }

    private GuiPage parsePage(Cfg c, String fileId, String pageId, String base) {
        int size = c.integer(base + "size");
        if (size < 9 || size > 54 || size % 9 != 0) {
            c.addWarning(base + "size must be a multiple of 9 between 9 and 54 - using 27");
            size = 27;
        }
        Material filler = Material.matchMaterial(c.string(base + "filler.material"));
        boolean fillerOn = c.bool(base + "filler.enabled", true) && filler != null;
        ToggleStyle fileStyle = new ToggleStyle(
                c.string("toggle-style.enabled.name"), c.stringList("toggle-style.enabled.lore"),
                c.string("toggle-style.disabled.name"), c.stringList("toggle-style.disabled.lore"));

        List<GuiItem> items = new ArrayList<>();
        Map<Integer, GuiItem> bySlot = new HashMap<>();
        for (String id : c.keys(base + "items")) {
            String p = base + "items." + id + ".";
            ItemType type;
            try {
                type = ItemType.valueOf(c.string(p + "type").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                c.addWarning(p + "type is invalid - item skipped");
                continue;
            }
            int slot = c.integer(p + "slot");
            if (slot < 0 || slot >= size) {
                c.addWarning(p + "slot is outside the inventory - item skipped");
                continue;
            }
            Material mat = Material.matchMaterial(c.string(p + "material"));
            if (mat == null || mat.isAir()) {
                c.addWarning(p + "material is invalid - using BARRIER");
                mat = Material.BARRIER;
            }
            PlayerSetting setting = null;
            if (type == ItemType.TOGGLE) {
                setting = PlayerSetting.fromKey(c.string(p + "setting"));
                if (setting == null) {
                    c.addWarning(p + "setting is unknown - item skipped");
                    continue;
                }
            }
            String label = c.has(p + "label") ? c.string(p + "label")
                    : (setting != null ? "gui.setting." + setting.key() + ".label" : "");
            String desc = c.has(p + "desc") ? c.string(p + "desc")
                    : (setting != null ? "gui.setting." + setting.key() + ".desc" : "");
            ToggleStyle style = fileStyle;
            if (c.has(p + "enabled.name") || c.has(p + "disabled.name")) {
                style = new ToggleStyle(
                        c.has(p + "enabled.name") ? c.string(p + "enabled.name") : fileStyle.enabledName(),
                        c.has(p + "enabled.lore") ? c.stringList(p + "enabled.lore") : fileStyle.enabledLore(),
                        c.has(p + "disabled.name") ? c.string(p + "disabled.name") : fileStyle.disabledName(),
                        c.has(p + "disabled.lore") ? c.stringList(p + "disabled.lore") : fileStyle.disabledLore());
            }
            String name = c.has(p + "name") ? c.string(p + "name") : "%label%";
            List<String> lore = c.has(p + "lore") ? c.stringList(p + "lore") : List.of();
            GuiItem item = new GuiItem(id, slot, mat, type, setting, c.string(p + "page"), label, desc, name, lore, style);
            items.add(item);
            bySlot.put(slot, item);
        }
        return new GuiPage(fileId, pageId, c.string(base + "title"), c.string(base + "title-key"), size,
                fillerOn, filler, items, bySlot);
    }

    private void closeStale() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof SettingsHolder h
                    && h.generation() != generation) {
                p.closeInventory();
            }
        }
    }

    public void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof SettingsHolder) {
                p.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------ opening

    public void openMain(Player viewer) {
        open(viewer, "settings", "main", viewer.getUniqueId(), viewer.getName(), false, null, null);
    }

    public void openConfirmReset(Player viewer) {
        open(viewer, "confirm-reset", "main", viewer.getUniqueId(), viewer.getName(), false, "settings", "main");
    }

    public void openAdmin(Player viewer, UUID target, String targetName) {
        open(viewer, "admin-settings", "main", target, targetName, true, null, null);
    }

    private void open(Player viewer, String fileId, String pageId, UUID target, String targetName,
                      boolean admin, String returnFile, String returnPage) {
        open(viewer, fileId, pageId, target, targetName, admin, returnFile, returnPage, true);
    }

    private void open(Player viewer, String fileId, String pageId, UUID target, String targetName,
                      boolean admin, String returnFile, String returnPage, boolean openSound) {
        GuiPage page = pages.get(fileId + "/" + pageId);
        if (page == null) {
            plugin.messages().error(viewer, "general.gui-disabled", null);
            return;
        }
        String lang = plugin.messages().lang(viewer);
        SettingsHolder holder = new SettingsHolder(viewer.getUniqueId(), target, targetName, admin, page,
                generation, returnFile, returnPage);
        String titleKey = page.titleKey();
        if (admin && fileId.equals("confirm-reset")) {
            titleKey = "gui.title.confirm-admin";
        }
        String title = page.titleFormat().replace("%title%", plugin.languages().text(lang, titleKey));
        Component titleComponent = plugin.messages().render(lang, title,
                Placeholders.create().text("target", targetName));
        Inventory inv = Bukkit.createInventory(holder, page.size(), titleComponent);
        holder.setInventory(inv);

        if (page.filler()) {
            ItemStack f = new ItemStack(page.fillerMaterial());
            ItemMeta fm = f.getItemMeta();
            fm.displayName(Component.text(" "));
            f.setItemMeta(fm);
            for (int i = 0; i < page.size(); i++) {
                inv.setItem(i, f);
            }
        }
        for (GuiItem item : page.items()) {
            inv.setItem(item.slot(), build(item, holder, lang));
        }
        viewer.openInventory(inv);
        if (openSound) {
            plugin.sounds().play(viewer, "gui-open", null);
        }
    }

    // ------------------------------------------------------------------ rendering

    private ItemStack build(GuiItem item, SettingsHolder h, String lang) {
        ItemStack stack = new ItemStack(item.material());
        ItemMeta meta = stack.getItemMeta();
        String name = item.name();
        List<String> lore = item.lore();
        boolean value = true;
        if (item.type() == ItemType.TOGGLE) {
            value = plugin.players().isEnabled(h.targetId(), item.setting());
            ToggleStyle st = item.style();
            name = value ? st.enabledName() : st.disabledName();
            lore = value ? st.enabledLore() : st.disabledLore();
        }
        var langs = plugin.languages();
        Map<String, String> strings = new HashMap<>();
        strings.put("%label%", item.label().isEmpty() ? "" : langs.text(lang, item.label()));
        strings.put("%desc%", item.desc().isEmpty() ? "" : langs.text(lang, item.desc()));
        strings.put("%state%", langs.text(lang, value ? "gui.state.enabled" : "gui.state.disabled"));
        strings.put("%hint%", langs.text(lang, "gui.hint." + item.type().name().toLowerCase(Locale.ROOT)));

        String targetLang = plugin.players().language(h.targetId());
        Placeholders ph = Placeholders.create()
                .text("target", h.targetName())
                .text("language", langs.displayName(targetLang));

        meta.displayName(line(lang, name, strings, ph));
        List<Component> lines = new ArrayList<>();
        for (String l : lore) {
            lines.add(line(lang, l, strings, ph));
        }
        meta.lore(lines);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (item.type() == ItemType.INFO && meta instanceof SkullMeta sm) {
            sm.setOwningPlayer(Bukkit.getOfflinePlayer(h.targetId()));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private Component line(String lang, String template, Map<String, String> strings, Placeholders ph) {
        String t = template;
        for (Map.Entry<String, String> e : strings.entrySet()) {
            t = t.replace(e.getKey(), e.getValue());
        }
        if (t.isEmpty()) {
            return Component.empty();
        }
        return plugin.messages().render(lang, t, ph).decoration(TextDecoration.ITALIC, false);
    }

    // ------------------------------------------------------------------ clicks

    public void handleClick(Player viewer, SettingsHolder h, int slot) {
        if (h.generation() != generation) {
            viewer.closeInventory();
            plugin.messages().error(viewer, "general.stale-gui", null);
            return;
        }
        boolean allowed = h.admin()
                ? viewer.hasPermission("ultraschat.admin.settings")
                : viewer.hasPermission("ultraschat.settings") && viewer.getUniqueId().equals(h.targetId());
        if (!allowed || !viewer.getUniqueId().equals(h.viewerId())) {
            viewer.closeInventory();
            plugin.messages().error(viewer, "general.no-permission", null);
            return;
        }
        GuiItem item = h.page().bySlot().get(slot);
        if (item == null) {
            return;
        }
        String file = h.page().fileId();
        switch (item.type()) {
            case TOGGLE -> {
                if (!h.admin() && !viewer.hasPermission(item.setting().permission())) {
                    plugin.messages().error(viewer, "general.no-permission", null);
                    return;
                }
                boolean nv = plugin.players().toggle(h.targetId(), item.setting());
                plugin.log().log(ChatLog.Category.SETTINGS, viewer.getName() + " set " + item.setting().key()
                        + "=" + nv + " for " + h.targetName());
                plugin.sounds().play(viewer, nv ? "toggle-on" : "toggle-off", null);
                h.getInventory().setItem(slot, build(item, h, plugin.messages().lang(viewer)));
            }
            case OPEN -> {
                plugin.sounds().play(viewer, "gui-click", null);
                open(viewer, "player-settings", item.page(), h.targetId(), h.targetName(), h.admin(), null, null);
            }
            case BACK -> {
                plugin.sounds().play(viewer, "gui-click", null);
                openMainFor(viewer, h);
            }
            case CLOSE -> viewer.closeInventory();
            case LANGUAGE -> {
                String next = plugin.languages().next(plugin.players().language(h.targetId()));
                plugin.players().setLanguage(h.targetId(), next);
                plugin.log().log(ChatLog.Category.LANGUAGE, viewer.getName() + " set language " + next + " for " + h.targetName());
                plugin.sounds().play(viewer, "language-changed", null);
                open(viewer, file, h.page().pageId(), h.targetId(), h.targetName(), h.admin(), h.returnFile(), h.returnPage(), false);
            }
            case RESET -> {
                plugin.sounds().play(viewer, "gui-click", null);
                open(viewer, "confirm-reset", "main", h.targetId(), h.targetName(), h.admin(), file, h.page().pageId());
            }
            case CANCEL -> {
                plugin.sounds().play(viewer, "gui-click", null);
                openMainFor(viewer, h);
            }
            case CONFIRM -> {
                plugin.players().reset(h.targetId());
                plugin.log().log(ChatLog.Category.SETTINGS, viewer.getName() + " reset settings of " + h.targetName());
                plugin.sounds().play(viewer, "reset", null);
                plugin.messages().feedback(viewer, h.admin() ? "reset.done-other" : "reset.done",
                        Placeholders.create().text("target", h.targetName()));
                openMainFor(viewer, h);
            }
            case INFO -> { }
        }
    }

    private void openMainFor(Player viewer, SettingsHolder h) {
        if (h.admin()) {
            openAdmin(viewer, h.targetId(), h.targetName());
        } else {
            openMain(viewer);
        }
    }
}
