//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.customs.items.util;

import edu.customs.items.ItemActionsPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {
    public static boolean shouldReduce(ItemActionsPlugin plugin, String itemKey, String context) {
        String basePath = "items." + itemKey + "." + context;
        return plugin.getConfig().contains(basePath + ".reduce-amount") || plugin.getConfig().contains(basePath + ".uses");
    }

    public static int getMaxUses(ItemActionsPlugin plugin, String itemKey, String context) {
        return plugin.getConfig().getInt("items." + itemKey + "." + context + ".uses", 0);
    }

    public static int getReduceAmount(ItemActionsPlugin plugin, String itemKey, String context) {
        return plugin.getConfig().getInt("items." + itemKey + "." + context + ".reduce-amount", 0);
    }

    public static int getUses(ItemActionsPlugin plugin, String itemKey, String context) {
        return plugin.getConfig().getInt("items." + itemKey + "." + context + ".uses", 0);
    }

    public static void setUses(ItemStack item, int uses) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) {
                    lore = new ArrayList();
                }

                List<String> newLore = new ArrayList();
                boolean replaced = false;

                for(String line : lore) {
                    if (line.contains("%uses%")) {
                        line = line.replace("%uses%", String.valueOf(uses));
                        replaced = true;
                    }

                    newLore.add(ColorUtil.format(line));
                }

                if (!replaced) {
                    newLore.add(ColorUtil.format("&7Usos restantes: " + uses));
                }

                meta.setLore(newLore);
                item.setItemMeta(meta);
            }
        }
    }
}
