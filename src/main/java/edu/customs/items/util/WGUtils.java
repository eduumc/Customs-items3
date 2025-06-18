//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.customs.items.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WGUtils {
    public static boolean canUseItem(Player player, Location location, List<String> globalRegionBlacklist, List<String> globalItemBlacklist, String itemName, String itemMaterial, List<String> itemRegionBlock) {
        try {
            LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt((World)Objects.requireNonNull(location.getWorld())));
            if (regionManager == null) {
                return true;
            } else {
                BlockVector3 vector = BukkitAdapter.asBlockVector(location);
                ApplicableRegionSet regions = regionManager.getApplicableRegions(vector);

                for(ProtectedRegion region : regions) {
                    String regionId = region.getId();
                    if (globalRegionBlacklist.stream().anyMatch((r) -> r.equalsIgnoreCase(regionId))) {
                        return false;
                    }
                }

                if (globalItemBlacklist.stream().anyMatch((i) -> i.equalsIgnoreCase(itemName))) {
                    return false;
                } else {
                    if (itemRegionBlock != null) {
                        for(ProtectedRegion region : regions) {
                            String regionId = region.getId();
                            if (itemRegionBlock.stream().anyMatch((r) -> r.equalsIgnoreCase(regionId))) {
                                return false;
                            }
                        }
                    }

                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}
