package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.*;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class AttackListener implements Listener {

    private final ItemActionsPlugin plugin;

    public AttackListener(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        Entity target = event.getEntity();

        ItemStack item = VersionUtils.getItemInHand(player);
        if (item == null || !item.hasItemMeta()) return;

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            String basePath = "items." + key;
            String material = plugin.getConfig().getString(basePath + ".material");
            String configName = plugin.getConfig().getString(basePath + ".name");

            if (material == null || configName == null) continue;

            if (!item.getType().name().equalsIgnoreCase(material)) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;

            if (!ColorUtil.matchColorName(configName, meta.getDisplayName())) continue;

            // Check WorldGuard regions or blacklists
            List<String> itemRegionBlock = plugin.getConfig().getStringList(basePath + ".region-block");
            boolean canUse = WGUtils.canUseItem(player, player.getLocation(),
                    plugin.getGlobalRegionBlacklist().stream().toList(),
                    plugin.getItemBlacklist().stream().toList(),
                    key, material, itemRegionBlock);

            if (!canUse) {
                player.sendMessage(ColorUtil.format(LangManager.get("blocked")));
                event.setCancelled(true);
                return;
            }

            // Cooldown
            long cooldown = plugin.getConfig().getLong(basePath + ".attack.cooldown", 0L);
            if (CooldownManager.hasCooldown(player, key)) {
                long remaining = CooldownManager.getRemaining(player, key);
                String prefix = plugin.getConfig().getString("Prefix", "&7[Items] ");
                player.sendMessage(ColorUtil.format(prefix + "&cTienes que esperar " + remaining + "s para volver a usar este ítem."));
                event.setCancelled(true);
                return;
            }

            // Ejecutar acciones
            List<String> commands = plugin.getConfig().getStringList(basePath + ".attack.commands");
            List<String> messages = plugin.getConfig().getStringList(basePath + ".attack.messages");

            ActionExecutor.run(plugin, player, target,
                    replaceVars(commands, player.getName(), target.getName()),
                    replaceVars(messages, player.getName(), target.getName()));

            if (cooldown > 0L) {
                CooldownManager.setCooldown(player, key, cooldown);
            }

            // Uso o reducción de cantidad
            int maxUses = plugin.getConfig().getInt(basePath + ".attack.uses", 0);
            int reduceAmount = plugin.getConfig().getInt(basePath + ".attack.reduce-amount", 0);
            NamespacedKey usesKey = new NamespacedKey(plugin, "custom_uses");

            if (maxUses > 0) {
                int currentUses = meta.getPersistentDataContainer().getOrDefault(usesKey, PersistentDataType.INTEGER, maxUses);
                int newUses = currentUses - 1;

                if (newUses <= 0) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, newUses);
                    item.setItemMeta(meta);
                    player.getInventory().setItemInMainHand(item);
                }

            } else if (reduceAmount > 0) {
                int newAmount = item.getAmount() - reduceAmount;
                if (newAmount <= 0) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    item.setAmount(newAmount);
                    player.getInventory().setItemInMainHand(item);
                }
            }

            break; // Salimos del bucle porque ya se encontró el ítem
        }
    }

    private List<String> replaceVars(List<String> list, String executor, String target) {
        return list.stream()
                .map(str -> str.replace("%executor%", executor).replace("%target%", target))
                .toList();
    }
}
