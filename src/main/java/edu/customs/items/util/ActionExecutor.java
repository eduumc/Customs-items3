//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.customs.items.util;

import edu.customs.items.ItemActionsPlugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionExecutor {
    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap();

    public static boolean isOnCooldown(Player player, String itemKey, long cooldownSeconds) {
        long currentTime = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = (Map)cooldowns.getOrDefault(player.getUniqueId(), new HashMap());
        if (playerCooldowns.containsKey(itemKey)) {
            long expireTime = (Long)playerCooldowns.get(itemKey);
            if (currentTime < expireTime) {
                return true;
            }
        }

        playerCooldowns.put(itemKey, currentTime + cooldownSeconds * 1000L);
        cooldowns.put(player.getUniqueId(), playerCooldowns);
        return false;
    }

    public static void run(ItemActionsPlugin plugin, Player executor, Entity target, List<String> commands, List<String> messages) {
        String executorName = executor.getName();
        String targetName = target instanceof Player ? ((Player)target).getName() : (target != null ? target.getType().name() : "Ninguno");
        String prefix = plugin.getConfig().getString("Prefix", "&a[CustomItems] ");
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        if (commands != null) {
            for(String command : commands) {
                String parsed = command.replace("%executor%", executorName).replace("%target%", targetName);
                Bukkit.dispatchCommand(console, parsed);
            }
        }

        if (messages != null) {
            for(String message : messages) {
                String parsed = message.replace("%executor%", executorName).replace("%target%", targetName);
                executor.sendMessage(ColorUtil.format(prefix + parsed));
            }
        }

    }
}
