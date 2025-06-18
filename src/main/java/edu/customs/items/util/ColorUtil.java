package edu.customs.items.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /**
     * Traduce una cadena con soporte para:
     * - Códigos hexadecimales (&#rrggbb)
     * - Códigos clásicos de color (&a, &b, etc.)
     */
    public static String format(String message) {
        if (message == null || message.isEmpty()) return "";
        if (message.contains("&#") || message.contains("#")) {
            message = translateSpigotHex(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Convierte códigos hexadecimales tipo &#rrggbb a formato §x§r§r§g§g§b§b.
     */
    public static String translateSpigotHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder();

        int lastIndex = 0;
        while (matcher.find()) {
            builder.append(message, lastIndex, matcher.start());
            String hexCode = matcher.group().substring(2); // Remove '&#'
            builder.append(ChatColor.COLOR_CHAR).append('x');
            for (char c : hexCode.toCharArray()) {
                builder.append(ChatColor.COLOR_CHAR).append(c);
            }
            lastIndex = matcher.end();
        }

        builder.append(message.substring(lastIndex));
        return builder.toString();
    }

    /**
     * Compara dos nombres con colores formateados y sin colores.
     */
    public static boolean matchColorName(String configName, String itemName) {
        if (configName != null && itemName != null) {
            String formattedConfigName = ChatColor.stripColor(format(configName));
            String strippedItemName = ChatColor.stripColor(itemName);
            return formattedConfigName.equalsIgnoreCase(strippedItemName);
        } else {
            return false;
        }
    }

    /**
     * Convierte un string a Adventure Component.
     * Si contiene <>, usa MiniMessage. Si contiene colores, los convierte.
     */
    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        // Si contiene MiniMessage
        if (message.contains("<") && message.contains(">")) {
            return MINI.deserialize(message);
        }

        // Si contiene RGB, convertir
        if (message.contains("&#") || message.contains("#")) {
            message = translateSpigotHex(message);
        }

        // Colores clásicos
        message = ChatColor.translateAlternateColorCodes('&', message);

        return LegacyComponentSerializer.legacySection().deserialize(message);
    }
}
