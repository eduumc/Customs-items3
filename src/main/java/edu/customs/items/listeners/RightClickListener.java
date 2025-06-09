package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.ActionExecutor;
import edu.customs.items.util.WGUtils;  // <--- Importa tu util WorldGuard
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RightClickListener implements Listener {

    private final ItemActionsPlugin plugin;

    public RightClickListener(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        Player player = event.getPlayer();
        ConfigurationSection items = plugin.getConfig().getConfigurationSection("items");
        if (items == null) return;

        for (String key : items.getKeys(false)) {
            String path = "items." + key;
            String material = plugin.getConfig().getString(path + ".material");
            String name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(path + ".name"));

            if (!item.getType().name().equalsIgnoreCase(material)) continue;
            if (!item.getItemMeta().getDisplayName().equals(name)) continue;

            // Verificación WorldGuard
            if (!WGUtils.canUse(player, player.getLocation())) {
                player.sendMessage("§cNo puedes usar este ítem en esta zona protegida.");
                event.setCancelled(true);
                return;
            }

            List<String> commands = plugin.getConfig().getStringList(path + ".right-click.commands");
            List<String> messages = plugin.getConfig().getStringList(path + ".right-click.messages");

            ActionExecutor.run(plugin, player, commands, messages);
        }
    }
}
