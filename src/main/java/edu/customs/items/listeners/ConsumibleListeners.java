package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

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

        List<String> globalRegionBlacklist = plugin.getConfig().getStringList("region-blacklist");
        List<String> globalItemBlacklist = plugin.getConfig().getStringList("item-blacklist");

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

            List<String> itemRegionBlock = plugin.getConfig().getStringList(basePath + ".region-block");

            boolean canUse = WGUtils.canUseItem(player, player.getLocation(),
                    globalRegionBlacklist, globalItemBlacklist,
                    key, material, itemRegionBlock);

            if (!canUse) {
                player.sendMessage(ColorUtil.format(LangManager.get("blocked")));
                event.setCancelled(true);
                return;
            }

            // ⏳ Cooldown check
            long cooldown = plugin.getConfig().getLong(basePath + ".consume.cooldown", 0);
            if (CooldownManager.hasCooldown(player, key)) {
                long remaining = CooldownManager.getRemaining(player, key);
                String prefix = plugin.getConfig().getString("Prefix", "&7[Items] ");
                player.sendMessage(ColorUtil.format(prefix + "&cTienes que esperar " + remaining + "s para volver a usar este ítem."));
                event.setCancelled(true);
                return;
            }

            // ✅ Ejecutar acciones
            List<String> commands = plugin.getConfig().getStringList(basePath + ".consume.commands");
            List<String> messages = plugin.getConfig().getStringList(basePath + ".consume.messages");

            commands = replaceVariables(commands, player.getName(), player.getName());
            messages = replaceVariables(messages, player.getName(), player.getName());

            ActionExecutor.run(plugin, player, player, commands, messages);

            // 🧊 Establecer cooldown
            if (cooldown > 0) {
                CooldownManager.setCooldown(player, key, cooldown);
            }

            // ❌ Reducción de ítem
            if (!ItemUtils.shouldReduce(plugin, key, "consume")) {
                event.setCancelled(true); // No se debe consumir
            } else {
                int reduceAmount = plugin.getConfig().getInt(basePath + ".consume.reduce-amount", 1);

                int newAmount = item.getAmount() - reduceAmount;
                if (newAmount <= 0) {
                    player.getInventory().removeItem(item);
                    player.sendMessage(LangManager.get("item-consumed"));
                } else {
                    item.setAmount(newAmount);
                    player.getInventory().setItemInMainHand(item);
                    player.sendMessage(LangManager.get("item-partially-consumed"));
                }
            }

            break; // ítem encontrado y procesado
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
