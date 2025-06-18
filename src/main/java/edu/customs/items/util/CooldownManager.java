//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.customs.items.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class CooldownManager {
    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap();

    public static boolean hasCooldown(Player player, String itemId) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return false;
        } else {
            Map<String, Long> playerCooldowns = (Map)cooldowns.get(player.getUniqueId());
            if (!playerCooldowns.containsKey(itemId)) {
                return false;
            } else {
                long expireTime = (Long)playerCooldowns.get(itemId);
                return System.currentTimeMillis() < expireTime;
            }
        }
    }

    public static long getRemaining(Player player, String itemId) {
        return !hasCooldown(player, itemId) ? 0L : ((Long)((Map)cooldowns.get(player.getUniqueId())).get(itemId) - System.currentTimeMillis()) / 1000L;
    }

    public static void setCooldown(Player player, String itemId, long seconds) {
        ((Map)cooldowns.computeIfAbsent(player.getUniqueId(), (k) -> new HashMap())).put(itemId, System.currentTimeMillis() + seconds * 1000L);
    }
}
