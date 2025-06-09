package edu.customs.items.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class VersionUtils {

    private static final boolean isLegacy = Bukkit.getVersion().contains("1.8");

    public static ItemStack getItemInHand(Player player) {
        try {
            if (isLegacy) {
                Method method = Player.class.getMethod("getItemInHand");
                return (ItemStack) method.invoke(player);
            } else {
                Method method = Player.class.getMethod("getInventory");
                Object inventory = method.invoke(player);
                Method getItemInMainHand = inventory.getClass().getMethod("getItemInMainHand");
                return (ItemStack) getItemInMainHand.invoke(inventory);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
