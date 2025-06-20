package edu.customs.items.expiration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

public class ExpirationSettings {
    private final int totalSeconds;
    private final List<ExpirationCondition> conditions;

    public ExpirationSettings(ConfigurationSection section) {
        int h = section.getInt("hours", 0);
        int m = section.getInt("minutes", 0);
        int s = section.getInt("seconds", 0);
        this.totalSeconds = h * 3600 + m * 60 + s;

        this.conditions = section.getStringList("conditions").stream()
                .map(str -> {
                    try {
                        return ExpirationCondition.valueOf(str.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(c -> c != null)
                .toList();
    }

    public boolean shouldCount(Player player, ItemStack item) {
        PlayerInventory inv = player.getInventory();
        for (ExpirationCondition cond : conditions) {
            switch (cond) {
                case SLOT_ARMOR:
                    if (!isInArmorSlot(inv, item)) return false;
                    break;
                case SLOT_MAINHAND:
                    if (!item.equals(inv.getItemInMainHand())) return false;
                    break;
                case SLOT_OFFHAND:
                    if (!item.equals(inv.getItemInOffHand())) return false;
                    break;
                case SLOT_INVENTORY:
                    if (!inv.contains(item)) return false;
                    break;
                case ALWAYS:
                    return true;
                // ON_HIT y ON_DAMAGE_RECEIVED se manejan por eventos
            }
        }
        return true;
    }

    private boolean isInArmorSlot(PlayerInventory inv, ItemStack item) {
        return item.equals(inv.getHelmet()) || item.equals(inv.getChestplate())
                || item.equals(inv.getLeggings()) || item.equals(inv.getBoots());
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public List<ExpirationCondition> getConditions() {
        return conditions;
    }
}
