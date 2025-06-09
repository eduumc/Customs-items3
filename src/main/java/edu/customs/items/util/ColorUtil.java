package edu.customs.items.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final boolean SUPPORTS_HEX = isHexSupported();

    public static String format(String message) {
        if (message == null) return "";

        // Si la versión soporta hex, reemplazamos colores hex
        if (SUPPORTS_HEX) {
            Matcher matcher = HEX_PATTERN.matcher(message);
            while (matcher.find()) {
                String hexCode = matcher.group();
                try {
                    ChatColor color = ChatColor.of(hexCode);
                    message = message.replace(hexCode, color.toString());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

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
        String bukkitVersion = org.bukkit.Bukkit.getBukkitVersion(); // Ej: "1.8.8-R0.1-SNAPSHOT"
        return bukkitVersion.split("-")[0]; // Resultado: "1.8.8"
    }
}
