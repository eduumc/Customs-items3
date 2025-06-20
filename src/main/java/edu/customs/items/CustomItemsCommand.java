package edu.customs.items;

import edu.customs.items.util.AdventureUtil;
import edu.customs.items.util.ColorUtil;
import edu.customs.items.util.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class CustomItemsCommand implements CommandExecutor {
    private final ItemActionsPlugin plugin;

    public CustomItemsCommand(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0 && !args[0].equalsIgnoreCase("help")) {
            switch (args[0].toLowerCase()) {
                case "give":
                    return handleGive(sender, args);
                case "reload":
                    return handleReload(sender);
                case "list":
                    return handleList(sender);
                default:
                    sender.sendMessage(LangManager.get("errors.invalid-subcommand"));
                    return true;
            }
        } else {
            sendHelp(sender);
            return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
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
        String key = args[2];
        ItemStack original = plugin.getCustomItemByName(key);
        if (original == null) {
            sender.sendMessage(LangManager.get("errors.item-not-found").replace("%item%", key));
            return true;
        }

        // Clonar para no alterar el original
        ItemStack item = original.clone();

        // Obtener configuración de este ítem
        String basePath = "items." + key;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(basePath);

        // Si existe config, aplicar nombre y usos iniciales
        if (section != null) {
            // 1. Inicializar usos si aplica (attack.uses o consume.uses)
            // Preferimos attack.uses si existe, sino consume.uses
            if (section.isConfigurationSection("attack")) {
                int uses = section.getInt("attack.uses", 0);
                if (uses > 0) {
                    applyUsesAndLore(item, basePath + ".attack", uses);
                }
            } else if (section.isConfigurationSection("consume")) {
                int uses = section.getInt("consume.uses", 0);
                if (uses > 0) {
                    applyUsesAndLore(item, basePath + ".consume", uses);
                }
            }
            // 2. Aplicar nombre coloreado
            String displayName = section.getString("name");
            if (displayName != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (displayName.contains("&#") || displayName.contains("<")) {
                        AdventureUtil.setDisplayName(meta, ColorUtil.toComponent(displayName));
                    } else {
                        meta.setDisplayName(ColorUtil.format(displayName));
                    }
                    item.setItemMeta(meta);
                }
            }
        }

        // 3. Manejar cantidad
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
                    .replace("%item%", key)
                    .replace("%max%", "1"));
            return true;
        }
        if (amount < 1 || amount > 128) {
            sender.sendMessage(LangManager.get("errors.invalid-amount-range")
                    .replace("%min%", "1")
                    .replace("%max%", "128"));
            return true;
        }
        item.setAmount(amount);

        // 4. Dar el ítem
        target.getInventory().addItem(item);
        sender.sendMessage(LangManager.get("success.item-given")
                .replace("%item%", key)
                .replace("%player%", target.getName()));
        target.sendMessage(LangManager.get("success.item-received")
                .replace("%item%", key));
        return true;
    }

    /**
     * Aplica el contador de usos inicial y actualiza el lore %uses% en base al path ("items.X.attack" o "items.X.consume").
     */
    private void applyUsesAndLore(ItemStack item, String sectionPrefix, int uses) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Poner PersistentData de usos
        NamespacedKey usesKey = new NamespacedKey(plugin, "custom_uses");
        meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, uses);

        // Actualizar lore si existe
        List<String> loreConfig = plugin.getConfig().getStringList(sectionPrefix + ".lore");
        if (!loreConfig.isEmpty()) {
            List<String> newLore = new ArrayList<>();
            for (String line : loreConfig) {
                String replaced = line.replace("%uses%", String.valueOf(uses));
                // Formatear colores + hex
                newLore.add(ColorUtil.format(replaced));
            }
            meta.setLore(newLore);
        }
        item.setItemMeta(meta);
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("customitems.reload")) {
            sender.sendMessage(LangManager.get("no-permission"));
            return true;
        }
        try {
            plugin.reloadConfig();
            plugin.loadLists();
            plugin.loadCustomItems();
            LangManager.reload(plugin);
            sender.sendMessage(LangManager.get("success.reload"));
        } catch (Exception e) {
            sender.sendMessage(LangManager.get("errors.reload"));
            e.printStackTrace();
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("customitems.list")) {
            sender.sendMessage(LangManager.get("no-permission"));
            return true;
        }
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection != null && !itemsSection.getKeys(false).isEmpty()) {
            sender.sendMessage(LangManager.get("list.title"));
            for (String key : itemsSection.getKeys(false)) {
                String rawName = plugin.getConfig().getString("items." + key + ".name", "&cSin nombre");
                // Formatear colores y hex
                String name = ColorUtil.format(rawName);
                String material = plugin.getConfig().getString("items." + key + ".material", "§cSin material");
                sender.sendMessage("§e - §b" + key + "§7 | Nombre: " + name + "§7 | Material: §a" + material);
            }
        } else {
            sender.sendMessage(LangManager.get("list.empty"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        if (!sender.hasPermission("customitems.help")) {
            sender.sendMessage(LangManager.get("no-permission"));
        } else {
            sender.sendMessage(LangManager.get("help.header"));
            sender.sendMessage(LangManager.get("help.give"));
            sender.sendMessage(LangManager.get("help.reload"));
            sender.sendMessage(LangManager.get("help.list"));
            sender.sendMessage(LangManager.get("help.help"));
        }
    }
}
