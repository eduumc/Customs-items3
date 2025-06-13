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

import java.util.*;

public class AttackListener implements Listener {

    private final ItemActionsPlugin plugin;

    public AttackListener(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        Entity target = event.getEntity();

        ItemStack item = VersionUtils.getItemInHand(player);
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

            String configName = ColorUtil.format(nameConfig);

            if (!item.getType().name().equalsIgnoreCase(material)) continue;
            if (!item.getItemMeta().getDisplayName().equals(configName)) continue;

            // ⛔ Verificación de regiones con WorldGuard u otras condiciones
            List<String> itemRegionBlock = plugin.getConfig().getStringList(basePath + ".region-block");

            boolean canUse = WGUtils.canUseItem(player, player.getLocation(),
                    globalRegionBlacklist, globalItemBlacklist,
                    key, material, itemRegionBlock);

            if (!canUse) {
                player.sendMessage(ColorUtil.format(LangManager.get("blocked")));
                event.setCancelled(true);
                return;
            }

            // ⏳ Manejo del cooldown
            long cooldown = plugin.getConfig().getLong(basePath + ".attack.cooldown", 0);
            if (CooldownManager.hasCooldown(player, key)) {
                long remaining = CooldownManager.getRemaining(player, key);
                String prefix = plugin.getConfig().getString("Prefix", "&7[Items] ");
                player.sendMessage(ColorUtil.format(prefix + "&cTienes que esperar " + remaining + "s para volver a usar este ítem."));
                event.setCancelled(true);
                return;
            }

            // ✅ Ejecución de acciones
            List<String> commands = plugin.getConfig().getStringList(basePath + ".attack.commands");
            List<String> messages = plugin.getConfig().getStringList(basePath + ".attack.messages");

            commands = replaceVariables(commands, player.getName(), target.getName());
            messages = replaceVariables(messages, player.getName(), target.getName());

            ActionExecutor.run(plugin, player, target, commands, messages);

            // 🧊 Establecer cooldown si es necesario
            if (cooldown > 0) {
                CooldownManager.setCooldown(player, key, cooldown);
            }

            // 🧪 Uso por cantidad (uses) o reducción (reduce-amount)
            int maxUses = plugin.getConfig().getInt(basePath + ".attack.uses", 0);
            int reduceAmount = plugin.getConfig().getInt(basePath + ".attack.reduce-amount", 0);

            ItemMeta meta = item.getItemMeta();
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
