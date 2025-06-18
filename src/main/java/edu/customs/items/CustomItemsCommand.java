package edu.customs.items;

import edu.customs.items.util.ColorUtil;
import edu.customs.items.util.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomItemsCommand implements CommandExecutor {

    private final ItemActionsPlugin plugin;

    public CustomItemsCommand(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("customitems.give")) {
                sender.sendMessage(LangManager.get("no-permission"));
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(LangManager.get("usage.give"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(LangManager.get("errors.player-not-found"));
                return true;
            }

            ItemStack item = plugin.getCustomItemByName(args[2]);
            if (item == null) {
                sender.sendMessage(LangManager.get("errors.item-not-found").replace("%item%", args[2]));
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(LangManager.get("errors.invalid-amount"));
                    return true;
                }
            }

            boolean stackable = item.getMaxStackSize() > 1;

            if (!stackable && amount > 1) {
                sender.sendMessage(LangManager.get("errors.not-stackable")
                        .replace("%item%", args[2])
                        .replace("%max%", "1"));
                return true;
            }

            if (stackable && (amount < 1 || amount > 128)) {
                sender.sendMessage(LangManager.get("errors.invalid-amount-range")
                        .replace("%min%", "1")
                        .replace("%max%", "128"));
                return true;
            }

            item.setAmount(amount);
            target.getInventory().addItem(item);

            sender.sendMessage(LangManager.get("success.item-given")
                    .replace("%item%", args[2])
                    .replace("%player%", target.getName()));
            target.sendMessage(LangManager.get("success.item-received")
                    .replace("%item%", args[2]));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("customitems.reload")) {
                sender.sendMessage(LangManager.get("no-permission"));
                return true;
            }

            try {
                plugin.reloadConfig();
                plugin.loadLists();
                plugin.loadCustomItems();
                    LangManager.reload(plugin); // 🔄 Recargar lenguaje

                sender.sendMessage(LangManager.get("success.reload"));
            } catch (Exception e) {
                sender.sendMessage(LangManager.get("errors.reload"));
                e.printStackTrace();
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("customitems.list")) {
                sender.sendMessage(LangManager.get("no-permission"));
                return true;
            }

            ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
            if (itemsSection == null || itemsSection.getKeys(false).isEmpty()) {
                sender.sendMessage(LangManager.get("list.empty"));
                return true;
            }

            sender.sendMessage(LangManager.get("list.title"));
            for (String key : itemsSection.getKeys(false)) {
                String rawName = plugin.getConfig().getString("items." + key + ".name", "&cSin nombre");
                String name = ColorUtil.format(rawName);
                String material = plugin.getConfig().getString("items." + key + ".material", "§cSin material");

                sender.sendMessage("§e - §b" + key + "§7 | Nombre: " + name + "§7 | Material: §a" + material);
            }
            return true;
        }

        sender.sendMessage(LangManager.get("errors.invalid-subcommand"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        if (!sender.hasPermission("customitems.help")) {
            sender.sendMessage(LangManager.get("no-permission"));
            return;
        }

        sender.sendMessage(LangManager.get("help.header"));
        sender.sendMessage(LangManager.get("help.give"));
        sender.sendMessage(LangManager.get("help.reload"));
        sender.sendMessage(LangManager.get("help.list"));
        sender.sendMessage(LangManager.get("help.help"));
    }
}
