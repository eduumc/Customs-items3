package edu.customs.items.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ActionExecutor {

    public static void run(Plugin plugin, Player player, List<String> commands, List<String> messages) {
        if (commands != null) {
            for (String command : commands) {
                command = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }

        if (messages != null) {
            for (String message : messages) {
                player.sendMessage(ColorUtil.format(message));
            }
        }
    }
}
