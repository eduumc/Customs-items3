package edu.customs.items.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.lang.reflect.Method;
import java.util.List;

public class AdventureUtil {

    /**
     * Establece el display name usando Adventure Component en Paper, o fallback a Spigot.
     */
    public static void setDisplayName(ItemMeta meta, Component component) {
        try {
            // Intentar usar Paper API
            Method displayNameMethod = meta.getClass().getMethod("displayName", Component.class);
            displayNameMethod.invoke(meta, component);
        } catch (Exception e) {
            // Fallback Spigot: serializar el Component a legacy con '§'
            String legacy = LegacyComponentSerializer.legacySection().serialize(component);
            meta.setDisplayName(legacy);
        }
    }

    /**
     * Establece lore usando Adventure Component en Paper, o fallback a Spigot.
     */
    @SuppressWarnings("unchecked")
    public static void setLore(ItemMeta meta, List<Component> lore) {
        try {
            // Paper API
            Method loreMethod = meta.getClass().getMethod("lore", List.class);
            loreMethod.invoke(meta, lore);
        } catch (Exception e) {
            // Fallback Spigot: serializar cada Component a String legacy
            List<String> legacy = lore.stream()
                    .map(c -> LegacyComponentSerializer.legacySection().serialize(c))
                    .toList();
            meta.setLore(legacy);
        }
    }

    /**
     * Establece usos en PersistentData y actualiza lore con %uses% si aplica.
     * @param meta ItemMeta a modificar
     * @param usesKey NamespacedKey para el contador
     * @param uses valor inicial
     * @param loreConfig lista de líneas de lore desde config (puede contener %uses%)
     */
    public static void applyUsesAndLore(ItemMeta meta, NamespacedKey usesKey, int uses, List<String> loreConfig) {
        meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, uses);
        if (loreConfig != null && !loreConfig.isEmpty()) {
            List<String> processed = loreConfig.stream()
                    .map(line -> line.replace("%uses%", String.valueOf(uses)))
                    .map(ColorUtil::format)
                    .toList();
            meta.setLore(processed);
        }
    }
}
