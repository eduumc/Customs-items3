package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

            String displayName = meta.getDisplayName();

            // ✅ Comparación segura usando ColorUtil
            if (!ColorUtil.matchColorName(nameConfig, displayName)) continue;

            // Región bloqueada
            boolean canUse = WGUtils.canUseItem(
                    player, player.getLocation(),
                    plugin.getGlobalRegionBlacklist().stream().toList(),
                    plugin.getItemBlacklist().stream().toList(),
                    key, material,
                    plugin.getCustomItemRegionBlocks().getOrDefault(key.toLowerCase(), Collections.emptyList())
            );

            if (!canUse) {
                player.sendMessage(ColorUtil.format(LangManager.get("blocked")));
                event.setCancelled(true);
                return;
            }

            // ⏳ Cooldown
            long cooldown = plugin.getConfig().getLong(basePath + ".right-click.cooldown", 0);
            if (CooldownManager.hasCooldown(player, key)) {
                long remaining = CooldownManager.getRemaining(player, key);
                String prefix = plugin.getConfig().getString("Prefix", "&7[Items] ");
                player.sendMessage(ColorUtil.format(prefix + "&cTienes que esperar " + remaining + "s para volver a usar este ítem."));
                event.setCancelled(true);
                return;
            }

            // ✅ Ejecutar comandos y mensajes
            List<String> commands = plugin.getConfig().getStringList(basePath + ".right-click.commands");
            List<String> messages = plugin.getConfig().getStringList(basePath + ".right-click.messages");

            commands = replaceVariables(commands, player.getName(), "Ninguno");
            messages = replaceVariables(messages, player.getName(), "Ninguno");

            ActionExecutor.run(plugin, player, null, commands, messages);

            // 🧊 Establecer cooldown
            if (cooldown > 0) {
                CooldownManager.setCooldown(player, key, cooldown);
            }

            // 🧪 Usos o cantidad
            int maxUses = plugin.getConfig().getInt(basePath + ".right-click.uses", 0);
            int reduceAmount = plugin.getConfig().getInt(basePath + ".right-click.reduce-amount", 0);

            if (maxUses > 0) {
                if (meta == null) break;

                List<String> lore = meta.getLore();
                int currentUses = maxUses;

                if (lore != null) {
                    for (String line : lore) {
                        if (line.matches(".*\\d+.*")) {
                            String digits = line.replaceAll("[^0-9]", "");
                            try {
                                currentUses = Integer.parseInt(digits);
                            } catch (NumberFormatException ignored) {}
                            break;
                        }
                    }
                }

                int newUses = currentUses - 1;

                if (newUses <= 0) {
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage(LangManager.get("item-broken"));
                } else {
                    if (lore != null) {
                        List<String> newLore = new ArrayList<>();
                        for (String line : lore) {
                            newLore.add(ColorUtil.format(line.replace("%uses%", String.valueOf(newUses))));
                        }
                        meta.setLore(newLore);
                        item.setItemMeta(meta);
                        player.getInventory().setItemInMainHand(item);
                    }
                }

            } else if (reduceAmount > 0) {
                int newAmount = item.getAmount() - reduceAmount;
                if (newAmount <= 0) {
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage(LangManager.get("item-consumed"));
                } else {
                    item.setAmount(newAmount);
                    player.getInventory().setItemInMainHand(item);
                }
            }

            break;
        }
    }

    private List<String> replaceVariables(List<String> list, String executor, String target) {
        if (list == null) return Collections.emptyList();
        List<String> replaced = new ArrayList<>();
        for (String line : list) {
            line = line.replace("%executor%", executor)
                    .replace("%target%", target);
            replaced.add(ColorUtil.format(line));
        }
        return replaced;
    }
}
