package edu.customs.items.listeners;

import edu.customs.items.expiration.ExpirationCondition;
import edu.customs.items.expiration.ExpirationHandler;
import edu.customs.items.expiration.ExpirationSettings;
import edu.customs.items.expiration.TrackedItem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExpirationListeners implements Listener {

    // Simula una base temporal (en ExpirationHandler real debería estar público)
    private final List<TrackedItem> getTrackedItems(Player player) {
        try {
            var field = ExpirationHandler.class.getDeclaredField("trackedItems");
            field.setAccessible(true);
            var map = (Map<UUID, List<TrackedItem>>) field.get(null);
            return map.getOrDefault(player.getUniqueId(), List.of());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) return;

        Player player = (Player) damager;
        ItemStack item = player.getInventory().getItemInMainHand();

        for (TrackedItem tracked : getTrackedItems(player)) {
            if (!tracked.getItem().isSimilar(item)) continue;

            ExpirationSettings settings = tracked.getSettings();
            if (settings.getConditions().contains(ExpirationCondition.ON_HIT)) {
                tracked.decrement();
            }
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        for (TrackedItem tracked : getTrackedItems(player)) {
            ExpirationSettings settings = tracked.getSettings();
            if (settings.getConditions().contains(ExpirationCondition.ON_DAMAGE_RECEIVED)) {
                tracked.decrement();
            }
        }
    }
}
