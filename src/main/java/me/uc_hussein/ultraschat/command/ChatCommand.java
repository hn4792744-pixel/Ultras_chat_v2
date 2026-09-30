package me.uc_hussein.ultraschat.command;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.language.LanguageManager;
import me.uc_hussein.ultraschat.logging.ChatLog;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import me.uc_hussein.ultraschat.util.VanishUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /chat with sub-commands: setting, toggle, language, reset, reload, msg, help. */
public final class ChatCommand implements CommandExecutor, TabCompleter {
    private final ULTRASChatPlugin plugin;

    public ChatCommand(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            if (args.length == 0) {
                help(sender);
                return true;
            }
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "setting", "settings" -> setting(sender, args);
                case "toggle" -> toggle(sender, args);
                case "language", "lang" -> language(sender, args);
                case "reset" -> reset(sender, args);
                case "reload" -> reload(sender);
                case "msg" -> msg(sender, args);
                case "ignore" -> ignore(sender, args);
                case "help" -> help(sender);
                default -> plugin.messages().error(sender, "general.unknown-command", null);
            }
        } catch (RuntimeException ex) {
            plugin.log().error("Command /chat failed", ex);
        }
        return true;
    }

    private boolean noPerm(CommandSender s, String perm) {
        if (s.hasPermission(perm)) {
            return false;
        }
        plugin.messages().error(s, "general.no-permission", null);
        return true;
    }

    private Player playerOnly(CommandSender s) {
        if (s instanceof Player p) {
            return p;
        }
        plugin.messages().error(s, "general.player-only", null);
        return null;
    }

    private void help(CommandSender s) {
        var m = plugin.messages();
        String lang = m.lang(s);
        s.sendMessage(m.component(lang, "help.header", null));
        for (String k : List.of("setting", "toggle", "language", "reset", "msg", "ignore")) {
            s.sendMessage(m.component(lang, "help." + k, null));
        }
        if (s.hasPermission("ultraschat.reload")) {
            s.sendMessage(m.component(lang, "help.reload", null));
        }
    }

    // ---------------------------------------------------------------- setting

    private void setting(CommandSender sender, String[] args) {
        Player p = playerOnly(sender);
        if (p == null) {
            return;
        }
        if (!plugin.cfg().bool("gui.enabled", true)) {
            plugin.messages().error(p, "general.gui-disabled", null);
            return;
        }
        if (args.length == 1) {
            if (!noPerm(p, "ultraschat.settings")) {
                plugin.gui().openMain(p);
            }
            return;
        }
        if (noPerm(p, "ultraschat.admin.settings")) {
            return;
        }
        String name = args[1];
        UUID target = null;
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            if (VanishUtil.visibleTo(plugin, p, online)) {
                target = online.getUniqueId();
                name = online.getName();
            }
        } else {
            target = plugin.players().findByName(name);
            if (target != null) {
                name = plugin.players().nameOf(target);
            } else {
                var cached = Bukkit.getOfflinePlayerIfCached(name);
                if (cached != null && cached.hasPlayedBefore()) {
                    target = cached.getUniqueId();
                    name = cached.getName() != null ? cached.getName() : name;
                }
            }
        }
        if (target == null) {
            plugin.messages().error(p, "general.player-not-found", null);
            return;
        }
        if (target.equals(p.getUniqueId())) {
            plugin.gui().openMain(p);
        } else {
            plugin.log().log(ChatLog.Category.SETTINGS, p.getName() + " opened settings of " + name);
            plugin.gui().openAdmin(p, target, name);
        }
    }

    // ---------------------------------------------------------------- toggle

    private boolean mayToggle(Player p, PlayerSetting s) {
        return p.hasPermission("ultraschat.use") && p.hasPermission(s.permission());
    }

    private void toggle(CommandSender sender, String[] args) {
        Player p = playerOnly(sender);
        if (p == null) {
            return;
        }
        var m = plugin.messages();
        if (args.length < 2) {
            List<String> keys = new ArrayList<>();
            for (PlayerSetting s : PlayerSetting.values()) {
                if (mayToggle(p, s)) {
                    keys.add(s.key());
                }
            }
            m.reply(p, "toggle.usage", null);
            m.reply(p, "toggle.list", Placeholders.create().text("list", String.join(", ", keys)));
            return;
        }
        PlayerSetting s = PlayerSetting.fromKey(args[1]);
        if (s == null) {
            m.error(p, "toggle.unknown", null);
            return;
        }
        if (!mayToggle(p, s)) {
            m.error(p, "general.no-permission", null);
            return;
        }
        Boolean explicit = null;
        if (args.length >= 3) {
            String v = args[2].toLowerCase(Locale.ROOT);
            if (v.equals("on") || v.equals("true")) {
                explicit = true;
            } else if (v.equals("off") || v.equals("false")) {
                explicit = false;
            } else {
                m.error(p, "toggle.usage", null);
                return;
            }
        }
        UUID id = p.getUniqueId();
        boolean nv = explicit != null ? explicit : !plugin.players().isEnabled(id, s);
        plugin.players().set(id, s, nv);
        plugin.log().log(ChatLog.Category.SETTINGS, p.getName() + " set " + s.key() + "=" + nv);
        // sent through feedback(): hidden when the player turned plugin messages off
        m.feedback(p, nv ? "toggle.enabled" : "toggle.disabled",
                Placeholders.create().put("setting", m.settingLabel(m.lang(p), s)));
        plugin.sounds().play(p, nv ? "toggle-on" : "toggle-off", null);
    }

    // ---------------------------------------------------------------- language / reset / reload / msg

    private void language(CommandSender sender, String[] args) {
        Player p = playerOnly(sender);
        if (p == null) {
            return;
        }
        var m = plugin.messages();
        UUID id = p.getUniqueId();
        String lang;
        if (args.length >= 2) {
            lang = LanguageManager.normalize(args[1]);
            if (lang == null) {
                m.error(p, "language.usage", null);
                return;
            }
        } else {
            lang = plugin.languages().next(plugin.players().language(id));
        }
        plugin.players().setLanguage(id, lang);
        plugin.log().log(ChatLog.Category.LANGUAGE, p.getName() + " set language " + lang);
        m.feedback(p, "language.changed",
                Placeholders.create().text("language", plugin.languages().displayName(lang)));
        plugin.sounds().play(p, "language-changed", null);
    }

    private void reset(CommandSender sender, String[] args) {
        Player p = playerOnly(sender);
        if (p == null || noPerm(p, "ultraschat.settings")) {
            return;
        }
        boolean confirm = args.length >= 2 && args[1].equalsIgnoreCase("confirm");
        if (confirm || !plugin.cfg().bool("gui.enabled", true)) {
            if (!confirm) {
                plugin.messages().reply(p, "reset.confirm-text", null);
                return;
            }
            plugin.players().reset(p.getUniqueId());
            plugin.log().log(ChatLog.Category.SETTINGS, p.getName() + " reset own settings");
            plugin.sounds().play(p, "reset", null);
            plugin.messages().feedback(p, "reset.done", null);
            return;
        }
        plugin.gui().openConfirmReset(p);
    }

    private void reload(CommandSender sender) {
        if (noPerm(sender, "ultraschat.reload")) {
            return;
        }
        plugin.reloadAll();
        var w = plugin.config().warnings();
        var m = plugin.messages();
        plugin.log().log(ChatLog.Category.RELOAD, sender.getName() + " reloaded (" + w.size() + " warnings)");
        if (w.isEmpty()) {
            m.reply(sender, "general.reloaded", null);
            if (sender instanceof Player p) {
                plugin.sounds().play(p, "success", null);
            }
            return;
        }
        m.error(sender, "general.reload-warnings", Placeholders.create().text("count", String.valueOf(w.size())));
        for (int i = 0; i < Math.min(5, w.size()); i++) {
            m.error(sender, "general.warning-line", Placeholders.create().text("text", w.get(i)));
        }
    }

    private void ignore(CommandSender sender, String[] args) {
        Player p = playerOnly(sender);
        if (p == null || noPerm(p, "ultraschat.ignore")) {
            return;
        }
        var m = plugin.messages();
        if (!plugin.cfg().bool("ignore.enabled", true)) {
            m.error(p, "ignore.disabled", null);
            return;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            var ids = plugin.players().ignoredBy(p.getUniqueId());
            if (ids.isEmpty()) {
                m.reply(p, "ignore.empty", null);
            } else {
                List<String> names = new ArrayList<>();
                for (UUID id : ids) {
                    names.add(plugin.players().nameOf(id));
                }
                m.reply(p, "ignore.list", Placeholders.create().text("list", String.join(", ", names)));
            }
            if (args.length < 2) {
                m.reply(p, "ignore.usage", null);
            }
            return;
        }
        UUID target = null;
        String name = args[1];
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && VanishUtil.visibleTo(plugin, p, online)) {
            target = online.getUniqueId();
            name = online.getName();
        } else if (online == null) {
            // allow removing someone who is offline but already on the list
            UUID stored = plugin.players().findByName(name);
            if (stored != null && plugin.players().ignoredBy(p.getUniqueId()).contains(stored)) {
                target = stored;
                name = plugin.players().nameOf(stored);
            }
        }
        if (target == null) {
            m.error(p, "general.player-not-found", null);
            return;
        }
        if (target.equals(p.getUniqueId())) {
            m.error(p, "ignore.self", null);
            return;
        }
        boolean nowIgnored = plugin.players().ignoredBy(p.getUniqueId()).contains(target);
        if (!nowIgnored && online != null && online.hasPermission("ultraschat.ignore.exempt")) {
            m.error(p, "ignore.exempt", null);
            return;
        }
        nowIgnored = plugin.players().toggleIgnore(p.getUniqueId(), target);
        plugin.log().log(ChatLog.Category.SETTINGS, p.getName() + (nowIgnored ? " ignores " : " stopped ignoring ") + name);
        m.feedback(p, nowIgnored ? "ignore.added" : "ignore.removed", Placeholders.create().text("target", name));
        plugin.sounds().play(p, nowIgnored ? "toggle-off" : "toggle-on", null);
    }

    private void msg(CommandSender sender, String[] args) {
        Player p = playerOnly(sender);
        if (p == null) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().error(p, "pm.usage", null);
            return;
        }
        plugin.privateMessages().send(p, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
    }

    // ---------------------------------------------------------------- tab completion

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("ultraschat.use")) {
            return out;
        }
        if (args.length == 1) {
            if (s instanceof Player && s.hasPermission("ultraschat.settings")) {
                out.add("setting");
                out.add("reset");
            }
            out.add("toggle");
            out.add("language");
            if (s.hasPermission("ultraschat.msg")) {
                out.add("msg");
            }
            if (s instanceof Player && s.hasPermission("ultraschat.ignore")) {
                out.add("ignore");
            }
            if (s.hasPermission("ultraschat.reload")) {
                out.add("reload");
            }
            out.add("help");
            return filter(out, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "setting", "settings" -> {
                    if (s.hasPermission("ultraschat.admin.settings")) {
                        out.addAll(visibleNames(s));
                    }
                }
                case "msg" -> {
                    if (s.hasPermission("ultraschat.msg")) {
                        out.addAll(visibleNames(s));
                    }
                }
                case "toggle" -> {
                    for (PlayerSetting ps : PlayerSetting.values()) {
                        if (s instanceof Player p && mayToggle(p, ps)) {
                            out.add(ps.key());
                        }
                    }
                }
                case "ignore" -> {
                    if (s.hasPermission("ultraschat.ignore")) {
                        out.add("list");
                        out.addAll(visibleNames(s));
                    }
                }
                case "language", "lang" -> out.addAll(LanguageManager.SUPPORTED);
                default -> { }
            }
            return filter(out, args[1]);
        }
        if (args.length == 3 && sub.equals("toggle")) {
            out.add("on");
            out.add("off");
            return filter(out, args[2]);
        }
        return out;
    }

    static List<String> visibleNames(CommandSender viewer) {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean visible = !(viewer instanceof Player v) || v.canSee(p);
            if (visible && (!VanishUtil.isVanished(p) || viewer.hasPermission("ultraschat.vanish.see"))) {
                names.add(p.getName());
            }
        }
        return names;
    }

    static List<String> filter(List<String> in, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s.toLowerCase(Locale.ROOT).startsWith(p) && !out.contains(s)) {
                out.add(s);
            }
        }
        return out;
    }
}
