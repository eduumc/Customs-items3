package edu.customs.items.expiration;

import org.bukkit.inventory.ItemStack;

public class TrackedItem {
    private final ItemStack item;
    private final ExpirationSettings settings;
    private int secondsRemaining;

    public TrackedItem(ItemStack item, ExpirationSettings settings) {
        this.item = item;
        this.settings = settings;
        this.secondsRemaining = settings.getTotalSeconds();
    }

    public void decrement() {
        secondsRemaining--;
    }

    public boolean isExpired() {
        return secondsRemaining <= 0;
    }

    public ItemStack getItem() {
        return item;
    }

    public ExpirationSettings getSettings() {
        return settings;
    }
}
