package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.ActionExecutor;
import edu.customs.items.util.VersionUtils;
import edu.customs.items.util.WGUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AttackListener implements Listener {

    private final ItemActionsPlugin plugin;

    public AttackListener(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = VersionUtils.getItemInHand(player);

        if (item == null || !item.hasItemMeta()) return;

        ConfigurationSection items = plugin.getConfig().getConfigurationSection("items");
        if (items == null) return;

        for (String key : items.getKeys(false)) {
            String path = "items." + key;
            String material = plugin.getConfig().getString(path + ".material");
            String name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(path + ".name"));

            if (!item.getType().name().equalsIgnoreCase(material)) continue;
            if (!item.getItemMeta().getDisplayName().equals(name)) continue;

            // WorldGuard
            if (!WGUtils.canUse(player, player.getLocation())) {
                player.sendMessage("§cNo puedes usar este ítem en esta zona protegida.");
                event.setCancelled(true);
                return;  // No ejecutar comandos ni mensajes si no tiene permiso
            }

            List<String> commands = plugin.getConfig().getStringList(path + ".attack.commands");
            List<String> messages = plugin.getConfig().getStringList(path + ".attack.messages");

            ActionExecutor.run(plugin, player, commands, messages);
        }
    }
}
