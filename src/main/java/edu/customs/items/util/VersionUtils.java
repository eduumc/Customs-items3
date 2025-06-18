//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.customs.items.util;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VersionUtils {
    private static final boolean isLegacy = Bukkit.getVersion().contains("1.8");

    public static ItemStack getItemInHand(Player player) {
        try {
            if (isLegacy) {
                Method method = Player.class.getMethod("getItemInHand");
                return (ItemStack)method.invoke(player);
            } else {
                Method method = Player.class.getMethod("getInventory");
                Object inventory = method.invoke(player);
                Method getItemInMainHand = inventory.getClass().getMethod("getItemInMainHand");
                return (ItemStack)getItemInMainHand.invoke(inventory);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
