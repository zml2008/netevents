package com.zachsthings.netevents;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command that provides status report
 */
public class StatusCommand implements CommandExecutor {
    private static String text(String... args) {
        StringBuilder build = new StringBuilder(ChatColor.BLUE.toString());
        for (String arg : args) {
            build.append(arg).append(ChatColor.BLUE);
        }
        return build.toString();
    }

    private static String hl(String text) {
        return ChatColor.YELLOW + text;
    }

    private final NetEventsPlugin plugin;

    public StatusCommand(NetEventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(text("NetEvents version ", hl(plugin.getDescription().getVersion())));
        sender.sendMessage(text("Remote listener bound to ", hl(plugin.getBoundAddress().toString())));
        sender.sendMessage(text("Connected servers:"));
        for (Forwarder f : plugin.getForwarders()) {
            if (f.isActive()) {
                sender.sendMessage(text("- ", hl(f.getRemoteAddress().toString())));
            } else if (f.getRemoteAddress() != null) {
                sender.sendMessage(text("- ", ChatColor.RED + f.getRemoteAddress().toString()));
            }
        }
        return true;
    }
}
