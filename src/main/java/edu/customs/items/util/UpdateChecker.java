//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.customs.items.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UpdateChecker {
    private final Plugin plugin;
    private final String versionURL;

    public UpdateChecker(Plugin plugin, String versionURL) {
        this.plugin = plugin;
        this.versionURL = versionURL;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection)(new URL(this.versionURL)).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = in.readLine().trim();
                in.close();
                String currentVersion = this.plugin.getDescription().getVersion();
                if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                    String message = "§a[CustomItems]§c Hay una nueva versión disponible: §e" + latestVersion + " §c(Tienes: §e" + currentVersion + "§c)";
                    Bukkit.getConsoleSender().sendMessage(message);
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        for(Player player : Bukkit.getOnlinePlayers()) {
                            if (player.isOp()) {
                                player.sendMessage(message);
                            }
                        }

                    });
                }
            } catch (Exception var6) {
                Bukkit.getConsoleSender().sendMessage("§a[CustomItems] §cNo se pudo verificar si hay una nueva versión.");
            }

        });
    }
}
