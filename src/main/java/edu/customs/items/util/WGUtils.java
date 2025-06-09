package edu.customs.items.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WGUtils {

    public static boolean canUse(Player player, Location loc) {
        try {
            World world = loc.getWorld();
            if (world == null) return true;

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager == null) return true;

            RegionQuery query = container.createQuery();

            Object localPlayer = createLocalPlayer(player);

            if (localPlayer == null) {
                Bukkit.getLogger().warning("[Customs-items] No se pudo crear LocalPlayer para WorldGuard.");
                return true;
            }

            Boolean allowed = (Boolean) query.getClass().getMethod("testState", com.sk89q.worldedit.util.Location.class, Class.forName("com.sk89q.worldguard.protection.LocalPlayer"), com.sk89q.worldguard.protection.flags.StateFlag.class)
                    .invoke(query, BukkitAdapter.adapt(loc), localPlayer, Flags.USE);

            return allowed == null || allowed;

        } catch (Exception e) {
            Bukkit.getLogger().warning("[Customs-items] WorldGuard no detectado o error en WGUtils.canUse() -> " + e.getMessage());
            return true;
        }
    }

    private static Object createLocalPlayer(Player player) {
        try {
            Class<?> wgPlayerClass;

            // Intenta cargar LocalPlayer en paquete viejo o nuevo
            try {
                wgPlayerClass = Class.forName("com.sk89q.worldguard.protection.LocalPlayer");
            } catch (ClassNotFoundException ex) {
                wgPlayerClass = Class.forName("com.sk89q.worldguard.protection.managers.LocalPlayer");
            }

            try {
                return wgPlayerClass.getMethod("adapt", Player.class).invoke(null, player);
            } catch (NoSuchMethodException ex) {
                return wgPlayerClass.getMethod("wrapPlayer", Player.class).invoke(null, player);
            }

        } catch (Exception ex) {
            Bukkit.getLogger().warning("[Customs-items] Error creando LocalPlayer con reflexión: " + ex.getMessage());
            return null;
        }
    }
}
