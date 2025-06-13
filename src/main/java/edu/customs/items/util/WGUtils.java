package edu.customs.items.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.LocalPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class WGUtils {

    /**
     * Verifica si un jugador puede usar un ítem personalizado en su ubicación
     * considerando las regiones bloqueadas globales, ítems bloqueados globales
     * y regiones bloqueadas específicas para el ítem.
     *
     * @param player Jugador que usa el ítem
     * @param location Ubicación del jugador
     * @param globalRegionBlacklist Lista global de regiones bloqueadas
     * @param globalItemBlacklist Lista global de ítems bloqueados (nombres de ítems, ej: "example", "BOW")
     * @param itemName Nombre del ítem personalizado (clave config, ej: "example")
     * @param itemMaterial Material del ítem en mayúsculas (ej: "DIAMOND_SWORD")
     * @param itemRegionBlock Lista de regiones bloqueadas específicas para el ítem (ej: config: `region-block`)
     * @return true si puede usar, false si está bloqueado
     */
    public static boolean canUseItem(Player player, Location location,
                                     List<String> globalRegionBlacklist,
                                     List<String> globalItemBlacklist,
                                     String itemName,
                                     String itemMaterial,
                                     List<String> itemRegionBlock) {
        try {
            LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(Objects.requireNonNull(location.getWorld())));

            if (regionManager == null) {
                // No hay regiones, permitir uso
                return true;
            }

            BlockVector3 vector = BukkitAdapter.asBlockVector(location);
            ApplicableRegionSet regions = regionManager.getApplicableRegions(vector);

            // 1) Chequeo: bloquear si alguna región activa está en la lista global
            for (ProtectedRegion region : regions) {
                String regionId = region.getId();
                if (globalRegionBlacklist.stream().anyMatch(r -> r.equalsIgnoreCase(regionId))) {
                    return false;
                }
            }

            // 2) Chequeo: bloquear si el ítem está listado en blacklist global
            if (globalItemBlacklist.stream().anyMatch(i -> i.equalsIgnoreCase(itemName))) {
                return false;
            }

            // 3) Chequeo: bloquear si alguna región activa está listada en region-block del ítem
            if (itemRegionBlock != null) {
                for (ProtectedRegion region : regions) {
                    String regionId = region.getId();
                    if (itemRegionBlock.stream().anyMatch(r -> r.equalsIgnoreCase(regionId))) {
                        return false;
                    }
                }
            }

            // 4) Si no coincide nada, permitir el uso
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // En caso de error, permitir uso por seguridad
            return true;
        }
    }
}
