package edu.customs.items.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public class CooldownManager {

    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public static boolean hasCooldown(Player player, String itemId) {
        if (!cooldowns.containsKey(player.getUniqueId())) return false;
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (!playerCooldowns.containsKey(itemId)) return false;

        long expireTime = playerCooldowns.get(itemId);
        return System.currentTimeMillis() < expireTime;
    }

    public static long getRemaining(Player player, String itemId) {
        if (!hasCooldown(player, itemId)) return 0;
        return (cooldowns.get(player.getUniqueId()).get(itemId) - System.currentTimeMillis()) / 1000;
    }

    public static void setCooldown(Player player, String itemId, long seconds) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(itemId, System.currentTimeMillis() + (seconds * 1000));
    }
}
