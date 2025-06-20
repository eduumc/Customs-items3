package edu.customs.items.expiration;

import edu.customs.items.ItemActionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ExpirationHandler {

    private static final Map<UUID, List<TrackedItem>> trackedItems = new HashMap<>();

    public static void trackItem(Player player, ItemStack item, ExpirationSettings settings) {
        trackedItems.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new TrackedItem(item, settings));
    }

    public static void startTask(ItemActionsPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : trackedItems.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;
                    List<TrackedItem> toRemove = new ArrayList<>();
                    for (TrackedItem tracked : trackedItems.get(uuid)) {
                        if (tracked.getSettings().shouldCount(player, tracked.getItem())) {
                            tracked.decrement();
                            if (tracked.isExpired()) {
                                player.getInventory().remove(tracked.getItem());
                                toRemove.add(tracked);
                            }
                        }
                    }
                    trackedItems.get(uuid).removeAll(toRemove);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // cada segundo
    }
}
