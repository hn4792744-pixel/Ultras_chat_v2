package me.uc_hussein.ultraschat.command;

import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** /msg (aliases tell, w, whisper) and /reply (alias r). */
public final class MsgCommand implements CommandExecutor, TabCompleter {
    private final ULTRASChatPlugin plugin;
    private final boolean reply;

    public MsgCommand(ULTRASChatPlugin plugin, boolean reply) {
        this.plugin = plugin;
        this.reply = reply;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.messages().error(sender, "general.player-only", null);
            return true;
        }
        try {
            if (reply) {
                if (args.length < 1) {
                    plugin.messages().error(p, "pm.reply-usage", null);
                } else {
                    plugin.privateMessages().reply(p, String.join(" ", args));
                }
            } else if (args.length < 2) {
                plugin.messages().error(p, "pm.usage", null);
            } else {
                plugin.privateMessages().send(p, args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            }
        } catch (RuntimeException ex) {
            plugin.log().error("Command /" + label + " failed", ex);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        if (reply || args.length != 1 || !s.hasPermission("ultraschat.msg")) {
            return List.of();
        }
        return ChatCommand.filter(ChatCommand.visibleNames(s), args[0]);
    }
}
