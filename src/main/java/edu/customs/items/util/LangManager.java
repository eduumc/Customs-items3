package edu.customs.items.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LangManager {

    private static final Map<String, String> messages = new HashMap<>();
    private static String prefix = "";

    public static void loadLanguage(File langFile, String configPrefix) {
        messages.clear();
        prefix = ChatColor.translateAlternateColorCodes('&', configPrefix);

        if (!langFile.exists()) {
            try {
                langFile.getParentFile().mkdirs();
                try (var in = LangManager.class.getResourceAsStream("/locale/" + langFile.getName())) {
                    if (in == null) {
                        Bukkit.getLogger().warning("[CustomItems] Language file " + langFile.getName() + " not found in plugin jar.");
                        return;
                    }
                    try (var out = new FileOutputStream(langFile)) {
                        in.transferTo(out);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        YamlConfiguration lang = YamlConfiguration.loadConfiguration(langFile);
        for (String key : lang.getKeys(true)) {
            if (lang.isString(key)) {
                String msg = lang.getString(key, "&cMessage not found");
                messages.put(key, ChatColor.translateAlternateColorCodes('&', msg));
            }
        }

        Bukkit.getLogger().info("[CustomItems] Loaded language file: " + langFile.getName());
        Bukkit.getLogger().info("[CustomItems] Loaded keys: " + messages.keySet());
    }

    public static void reload(JavaPlugin plugin) {
        String locale = plugin.getConfig().getString("Locale", "En-us");
        String configPrefix = plugin.getConfig().getString("Prefix", "&a[CustomItems] ");
        File langFile = new File(plugin.getDataFolder(), "locale/" + locale + ".yml");

        loadLanguage(langFile, configPrefix);
    }

    public static String get(String key) {
        String msg = messages.get(key);
        if (msg == null) {
            Bukkit.getLogger().warning("[CustomItems] Missing lang key: " + key);
            return prefix + ChatColor.RED + "Message not found: " + key;
        }
        return prefix + msg;
    }

    public static String raw(String key) {
        String msg = messages.get(key);
        if (msg == null) {
            Bukkit.getLogger().warning("[CustomItems] Missing lang key (raw): " + key);
            return ChatColor.RED + "Message not found: " + key;
        }
        return msg;
    }
}
