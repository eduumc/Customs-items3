package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.*;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class RightClickListener implements Listener {

    private final ItemActionsPlugin plugin;

    public RightClickListener(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = VersionUtils.getItemInHand(player);
        if (item == null || !item.hasItemMeta()) return;

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            String basePath = "items." + key;
            String material = plugin.getConfig().getString(basePath + ".material");
            String nameConfig = plugin.getConfig().getString(basePath + ".name");

            if (material == null || nameConfig == null) continue;
            if (!item.getType().name().equalsIgnoreCase(material)) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;
            if (!ColorUtil.matchColorName(nameConfig, meta.getDisplayName())) continue;

            // Verificar regiones bloqueadas
            List<String> itemRegionBlock = plugin.getCustomItemRegionBlocks().getOrDefault(key.toLowerCase(), Collections.emptyList());
            List<String> regionBlacklist = new ArrayList<>(plugin.getGlobalRegionBlacklist());
            List<String> itemBlacklist = new ArrayList<>(plugin.getItemBlacklist());

            boolean canUse = WGUtils.canUseItem(player, player.getLocation(), regionBlacklist, itemBlacklist, key, material, itemRegionBlock);

            if (!canUse) {
                player.sendMessage(ColorUtil.format(LangManager.get("blocked")));
                event.setCancelled(true);
                return;
            }

            // Cooldown
            long cooldown = plugin.getConfig().getLong(basePath + ".right-click.cooldown", 0L);
            if (CooldownManager.hasCooldown(player, key)) {
                long remaining = CooldownManager.getRemaining(player, key);
                String prefix = plugin.getConfig().getString("Prefix", "&7[Items] ");
                player.sendMessage(ColorUtil.format(prefix + "&cTienes que esperar " + remaining + "s para volver a usar este ítem."));
                event.setCancelled(true);
                return;
            }

            // Ejecutar acciones
            List<String> commands = replaceVariables(plugin.getConfig().getStringList(basePath + ".right-click.commands"), player.getName(), "Ninguno");
            List<String> messages = replaceVariables(plugin.getConfig().getStringList(basePath + ".right-click.messages"), player.getName(), "Ninguno");

            ActionExecutor.run(plugin, player, null, commands, messages);

            if (cooldown > 0L) {
                CooldownManager.setCooldown(player, key, cooldown);
            }

            // Manejo de usos
            int maxUses = plugin.getConfig().getInt(basePath + ".right-click.uses", 0);
            int reduceAmount = plugin.getConfig().getInt(basePath + ".right-click.reduce-amount", 0);
            NamespacedKey usesKey = new NamespacedKey(plugin, "custom_uses");

            if (maxUses > 0) {
                int currentUses = meta.getPersistentDataContainer().getOrDefault(usesKey, PersistentDataType.INTEGER, maxUses);
                int newUses = currentUses - 1;

                if (newUses <= 0) {
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage(ColorUtil.format(LangManager.get("item-broken")));
                } else {
                    meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, newUses);

                    // Actualizar lore
                    List<String> updatedLore = meta.getLore();
                    if (updatedLore != null) {
                        updatedLore = updatedLore.stream()
                                .map(line -> ColorUtil.format(line.replace("%uses%", String.valueOf(newUses))))
                                .toList();
                        meta.setLore(updatedLore);
                    }

                    item.setItemMeta(meta);
                    player.getInventory().setItemInMainHand(item);
                }

            } else if (reduceAmount > 0) {
                int newAmount = item.getAmount() - reduceAmount;
                if (newAmount <= 0) {
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage(ColorUtil.format(LangManager.get("item-consumed")));
                } else {
                    item.setAmount(newAmount);
                    player.getInventory().setItemInMainHand(item);
                }
            }

            break; // Ya coincidió con uno
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
