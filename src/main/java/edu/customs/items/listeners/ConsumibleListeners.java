package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.*;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConsumibleListeners implements Listener {

    private final ItemActionsPlugin plugin;

    public ConsumibleListeners(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection == null) return;

        List<String> globalRegionBlacklist = new ArrayList<>(plugin.getGlobalRegionBlacklist());
        List<String> globalItemBlacklist = new ArrayList<>(plugin.getItemBlacklist());

        for (String key : itemsSection.getKeys(false)) {
            String basePath = "items." + key;
            String material = plugin.getConfig().getString(basePath + ".material");
            String configName = plugin.getConfig().getString(basePath + ".name");

            if (material == null || configName == null) continue;
            if (!item.getType().name().equalsIgnoreCase(material)) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;
            if (!ColorUtil.matchColorName(configName, meta.getDisplayName())) continue;

            // Bloqueo por región
            List<String> itemRegionBlock = plugin.getConfig().getStringList(basePath + ".region-block");
            boolean canUse = WGUtils.canUseItem(player, player.getLocation(),
                    globalRegionBlacklist, globalItemBlacklist, key, material, itemRegionBlock);
            if (!canUse) {
                player.sendMessage(ColorUtil.format(LangManager.get("blocked")));
                event.setCancelled(true);
                return;
            }

            // Cooldown
            long cooldown = plugin.getConfig().getLong(basePath + ".consume.cooldown", 0L);
            if (CooldownManager.hasCooldown(player, key)) {
                long remaining = CooldownManager.getRemaining(player, key);
                String prefix = plugin.getConfig().getString("Prefix", "&7[Items] ");
                player.sendMessage(ColorUtil.format(prefix + "&cTienes que esperar " + remaining + "s para volver a usar este ítem."));
                event.setCancelled(true);
                return;
            }

            // Ejecutar acciones
            List<String> commands = plugin.getConfig().getStringList(basePath + ".consume.commands");
            List<String> messages = plugin.getConfig().getStringList(basePath + ".consume.messages");

            ActionExecutor.run(plugin, player, player,
                    replaceVariables(commands, player.getName(), player.getName()),
                    replaceVariables(messages, player.getName(), player.getName()));

            if (cooldown > 0L) {
                CooldownManager.setCooldown(player, key, cooldown);
            }

            // Uso o reducción
            int maxUses = plugin.getConfig().getInt(basePath + ".consume.uses", 0);
            int reduceAmount = plugin.getConfig().getInt(basePath + ".consume.reduce-amount", 0);
            NamespacedKey usesKey = new NamespacedKey(plugin, "custom_uses");

            if (maxUses > 0) {
                int currentUses = meta.getPersistentDataContainer().getOrDefault(usesKey, PersistentDataType.INTEGER, maxUses);
                int newUses = currentUses - 1;

                if (newUses <= 0) {
                    player.getInventory().remove(item);
                    player.sendMessage(ColorUtil.format(LangManager.get("item-consumed")));
                } else {
                    meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, newUses);

                    List<String> updatedLore = meta.getLore();
                    if (updatedLore != null) {
                        updatedLore = updatedLore.stream()
                                .map(line -> line.replaceAll("%uses%", String.valueOf(newUses)))
                                .toList();
                        meta.setLore(updatedLore);
                    }

                    item.setItemMeta(meta);
                    player.getInventory().setItemInMainHand(item);
                    player.sendMessage(ColorUtil.format(LangManager.get("item-partially-consumed")));
                }
            } else if (reduceAmount > 0) {
                int newAmount = item.getAmount() - reduceAmount;
                if (newAmount <= 0) {
                    player.getInventory().removeItem(item);
                    player.sendMessage(ColorUtil.format(LangManager.get("item-consumed")));
                } else {
                    item.setAmount(newAmount);
                    player.getInventory().setItemInMainHand(item);
                    player.sendMessage(ColorUtil.format(LangManager.get("item-partially-consumed")));
                }
            }

            break;
        }
    }

    private List<String> replaceVariables(List<String> list, String executor, String target) {
        if (list == null) return Collections.emptyList();
        List<String> replaced = new ArrayList<>();
        for (String line : list) {
            replaced.add(ColorUtil.format(line.replace("%executor%", executor).replace("%target%", target)));
        }
        return replaced;
    }
}
