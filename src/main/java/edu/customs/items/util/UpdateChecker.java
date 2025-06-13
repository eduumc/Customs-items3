package edu.customs.items.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final Plugin plugin;
    private final String versionURL;

    public UpdateChecker(Plugin plugin, String versionURL) {
        this.plugin = plugin;
        this.versionURL = versionURL;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(versionURL).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = in.readLine().trim();
                in.close();

                String currentVersion = plugin.getDescription().getVersion();

                if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                    String message = "§a[CustomItems]§c Hay una nueva versión disponible: §e" + latestVersion + " §c(Tienes: §e" + currentVersion + "§c)";

                    // Mensaje a la consola
                    Bukkit.getConsoleSender().sendMessage(message);

                    // Enviar mensaje a todos los operadores online
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.isOp()) {
                                player.sendMessage(message);
                            }
                        }
                    });
                }

            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("§a[CustomItems] §cNo se pudo verificar si hay una nueva versión.");
            }
        });
    }
}
