package edu.customs.items.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    // Soporte para #rrggbb y &#rrggbb
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})|#([a-fA-F0-9]{6})");

    // Soporte para &x&F&F&0&0&0&0
    private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("&x((&[A-Fa-f0-9]){6})");

    private static final boolean SUPPORTS_HEX = isHexSupported();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /**
     * Aplica formato de color:
     * - Legacy (&a, &b)
     * - Hex (&x&F&F&0&0&0&0)
     * - Hex (#FF0000 y &#FF0000)
     */
    public static String format(String message) {
        if (message == null) return "";

        // Traducir formato &x&F&F&0&0&0&0 → #FF0000
        message = translateSpigotHex(message);

        // Aplicar colores hex si es compatible
        if (SUPPORTS_HEX) {
            Matcher matcher = HEX_PATTERN.matcher(message);
            while (matcher.find()) {
                String hexCode = matcher.group();
                try {
                    ChatColor color = ChatColor.of(hexCode.replace("&", ""));
                    message = message.replace(hexCode, color.toString());
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Aplicar colores legacy (&a, &b, etc.)
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Traduce formato &x&F&F&0&0&0&0 a #FF0000
     */
    private static String translateSpigotHex(String message) {
        Matcher matcher = SPIGOT_HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String group = matcher.group(1).replace("&", "");
            if (group.length() == 6) {
                String hex = "#" + group;
                message = message.replace(matcher.group(), hex);
            }
        }
        return message;
    }

    /**
     * Usa MiniMessage si se detecta <...>, si no, aplica legacy + hex
     */
    public static BaseComponent[] formatLegacyOrMini(String input) {
        if (input == null) return new BaseComponent[0];

        if (input.contains("<") && input.contains(">")) {
            Component component = MINI.deserialize(input);
            String legacy = LegacyComponentSerializer.legacySection().serialize(component);
            return new ComponentBuilder(legacy).create();
        } else {
            return new ComponentBuilder(format(input)).create();
        }
    }

    /**
     * Devuelve un TextComponent plano con formato legacy + hex
     */
    public static TextComponent simple(String input) {
        return new TextComponent(format(input));
    }

    /**
     * Detecta si el servidor soporta colores hexadecimales (1.16+)
     */
    private static boolean isHexSupported() {
        String version = getServerVersion();
        try {
            int major = Integer.parseInt(version.split("\\.")[1]);
            return major >= 16;
        } catch (Exception e) {
            return false;
        }
    }

    private static String getServerVersion() {
        String bukkitVersion = Bukkit.getBukkitVersion(); // Ej: "1.18.2-R0.1-SNAPSHOT"
        return bukkitVersion.split("-")[0]; // Resultado: "1.18.2"
    }

    /**
     * Compara dos nombres ignorando colores (hex + legacy + MiniMessage)
     */
    public static boolean matchColorName(String configName, String itemName) {
        if (configName == null || itemName == null) return false;

        String formattedConfigName = ChatColor.stripColor(format(configName));
        String strippedItemName = ChatColor.stripColor(itemName);

        return formattedConfigName.equalsIgnoreCase(strippedItemName);
    }
}
