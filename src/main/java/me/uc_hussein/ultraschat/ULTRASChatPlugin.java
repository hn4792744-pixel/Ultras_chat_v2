package me.uc_hussein.ultraschat;

import me.uc_hussein.ultraschat.chat.AntiSpamService;
import me.uc_hussein.ultraschat.command.ChatCommand;
import me.uc_hussein.ultraschat.command.MsgCommand;
import me.uc_hussein.ultraschat.config.Cfg;
import me.uc_hussein.ultraschat.config.ConfigService;
import me.uc_hussein.ultraschat.gui.GuiManager;
import me.uc_hussein.ultraschat.language.LanguageManager;
import me.uc_hussein.ultraschat.listener.AchievementListener;
import me.uc_hussein.ultraschat.listener.ChatListener;
import me.uc_hussein.ultraschat.listener.DeathListener;
import me.uc_hussein.ultraschat.listener.GuiListener;
import me.uc_hussein.ultraschat.listener.JoinQuitListener;
import me.uc_hussein.ultraschat.logging.ChatLog;
import me.uc_hussein.ultraschat.mention.MentionIndex;
import me.uc_hussein.ultraschat.mention.MentionService;
import me.uc_hussein.ultraschat.notification.ActionBarService;
import me.uc_hussein.ultraschat.notification.MessageService;
import me.uc_hussein.ultraschat.notification.Theme;
import me.uc_hussein.ultraschat.private_message.PrivateMessageService;
import me.uc_hussein.ultraschat.settings.PlayerDataService;
import me.uc_hussein.ultraschat.sound.SoundService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/** ULTRAS_Chat_v1 by UC_Hussein. Wires services together; listeners and commands are registered exactly once. */
public final class ULTRASChatPlugin extends JavaPlugin {
    private ConfigService configService;
    private ChatLog chatLog;
    private LanguageManager languages;
    private Theme theme;
    private PlayerDataService players;
    private MessageService messages;
    private SoundService sounds;
    private ActionBarService actionBars;
    private AntiSpamService antiSpam;
    private MentionIndex mentionIndex;
    private MentionService mentions;
    private PrivateMessageService privateMessages;
    private GuiManager gui;
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        try {
            getDataFolder().mkdirs();
            configService = new ConfigService(this);
            chatLog = new ChatLog(this);
            languages = new LanguageManager(this);
            theme = new Theme();
            players = new PlayerDataService(this);
            messages = new MessageService(this);
            sounds = new SoundService(this);
            actionBars = new ActionBarService(this);
            antiSpam = new AntiSpamService(this);
            mentionIndex = new MentionIndex(this);
            mentions = new MentionService(this);
            privateMessages = new PrivateMessageService(this);
            gui = new GuiManager(this);

            reloadAll();
            players.load();
            registerListeners();
            registerCommands();
            mentionIndex.start();
            Bukkit.getScheduler().runTask(this, () -> {
                // runs after every plugin has been enabled
                chatListener.refreshFormatMode();
                checkCommandOwnership();
            });
            chatLog.log(ChatLog.Category.RELOAD, "ULTRAS_Chat enabled");
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "ULTRAS_Chat failed to enable", ex);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (gui != null) {
            gui.closeAll();
        }
        if (actionBars != null) {
            actionBars.shutdown();
        }
        if (mentionIndex != null) {
            mentionIndex.stop();
        }
        if (players != null) {
            players.shutdown();
        }
        if (chatLog != null) {
            chatLog.close();
        }
    }

    /**
     * Reloads config, language files, colors, sounds and GUI files.
     * Player data, listeners, commands and tasks are deliberately NOT touched.
     */
    public synchronized void reloadAll() {
        configService.reload();
        List<String> w = configService.warnings();
        Cfg cfg = configService.cfg();
        languages.reload(w);
        theme.reload(cfg, w);
        sounds.reload(cfg, w);
        gui.reload(w);
        chatLog.prune();
        if (chatListener != null) {
            chatListener.refreshFormatMode();
        }
        mentionIndex.refresh();
        for (String s : w) {
            getLogger().warning(s);
            chatLog.log(ChatLog.Category.CONFIG, s);
        }
    }

    private void registerListeners() {
        chatListener = new ChatListener(this);
        var pm = getServer().getPluginManager();
        pm.registerEvents(chatListener, this);
        pm.registerEvents(new DeathListener(this), this);
        pm.registerEvents(new JoinQuitListener(this), this);
        pm.registerEvents(new AchievementListener(this), this);
        pm.registerEvents(new GuiListener(this), this);
    }

    private void registerCommands() {
        ChatCommand chat = new ChatCommand(this);
        bind("chat", chat, chat);
        MsgCommand msg = new MsgCommand(this, false);
        bind("msg", msg, msg);
        MsgCommand reply = new MsgCommand(this, true);
        bind("reply", reply, reply);
    }

    private <T extends CommandExecutor & TabCompleter> void bind(String name, T handler, TabCompleter tab) {
        PluginCommand c = getCommand(name);
        if (c == null) {
            getLogger().severe("Command '" + name + "' is missing from plugin.yml");
            return;
        }
        c.setExecutor(handler);
        c.setTabCompleter(tab);
    }

    /** Warns (instead of hacking) when another plugin owns /msg, /tell, /w ... */
    private void checkCommandOwnership() {
        for (String name : List.of("chat", "msg", "reply")) {
            PluginCommand mine = getCommand(name);
            if (mine == null) {
                continue;
            }
            List<String> labels = new java.util.ArrayList<>(mine.getAliases());
            labels.add(0, name);
            for (String label : labels) {
                Command actual = Bukkit.getCommandMap().getCommand(label);
                if (actual != null && actual != mine) {
                    String msg = "/" + label + " is handled by another command (" + actual.getClass().getSimpleName()
                            + "). ULTRAS_Chat did not override it. Use /ultras_chat:" + label + " or see the README (Command conflicts).";
                    getLogger().warning(msg);
                    chatLog.log(ChatLog.Category.CONFIG, msg);
                }
            }
        }
    }

    public ConfigService config() { return configService; }
    public Cfg cfg() { return configService == null ? null : configService.cfg(); }
    public ChatLog log() { return chatLog; }
    public LanguageManager languages() { return languages; }
    public Theme theme() { return theme; }
    public PlayerDataService players() { return players; }
    public MessageService messages() { return messages; }
    public SoundService sounds() { return sounds; }
    public ActionBarService actionBars() { return actionBars; }
    public AntiSpamService antiSpam() { return antiSpam; }
    public MentionIndex mentionIndex() { return mentionIndex; }
    public MentionService mentions() { return mentions; }
    public PrivateMessageService privateMessages() { return privateMessages; }
    public GuiManager gui() { return gui; }
}
