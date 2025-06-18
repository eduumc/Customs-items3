//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package edu.customs.items.us.com.java.edu.nolook.any;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class A1 {
    private static final Logger LOGGER = Logger.getLogger("CustomsItemsLicense");
    private static final String VERIFICATION_URL = "https://script.google.com/macros/s/AKfycbzdSEX1nNXEonLEkVKJOgBA7kznfeAR_LzhmIRVdL81g3K3C21zBkRcUZnAUC36yMQl/exec";
    private static final String SECRET = "edu084784858958ngnjdmasdaw";
    private static final AtomicBoolean verificationInProgress = new AtomicBoolean(false);

    public static void checkLicense(JavaPlugin plugin, String licenseId, Runnable onSuccess) {
        if (verificationInProgress.get()) {
            LOGGER.warning("Ya hay una verificación de licencia en curso. Se omite esta solicitud.");
        } else {
            verificationInProgress.set(true);
            String verificationId = UUID.randomUUID().toString();
            LOGGER.info("Iniciando verificación de licencia: " + verificationId);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    String serverIp = getPublicIp();
                    String pluginName = URLEncoder.encode(plugin.getName(), StandardCharsets.UTF_8);
                    String secretKey = URLEncoder.encode("edu084784858958ngnjdmasdaw", StandardCharsets.UTF_8);
                    String licEnc = URLEncoder.encode(licenseId, StandardCharsets.UTF_8);
                    String urlStr = "https://script.google.com/macros/s/AKfycbzdSEX1nNXEonLEkVKJOgBA7kznfeAR_LzhmIRVdL81g3K3C21zBkRcUZnAUC36yMQl/exec?ip=" + serverIp + "&plugin=" + pluginName + "&secret=" + secretKey + "&licenseId=" + licEnc + "&v=" + verificationId;
                    boolean isValid = callApi(urlStr, verificationId);
                    if (!isValid) {
                        handleInvalidLicense(plugin);
                    } else {
                        Bukkit.getScheduler().runTask(plugin, onSuccess);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al verificar la licencia: ", e);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().severe("ERROR AL VERIFICAR LA LICENCIA: " + e.getMessage());
                        Bukkit.getPluginManager().disablePlugin(plugin);
                    });
                } finally {
                    verificationInProgress.set(false);
                }

            });
        }
    }

    private static boolean callApi(String urlStr, String verificationId) {
        try {
            HttpURLConnection con = (HttpURLConnection)(new URL(urlStr)).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                boolean var6;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String result = reader.readLine();
                    LOGGER.info("[LicenseCheck] [" + verificationId + "] → " + result);
                    var6 = result != null && result.trim().equalsIgnoreCase("true");
                }

                return var6;
            }

            LOGGER.warning("Error en la API. Código HTTP: " + responseCode);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al conectar con la API: ", e);
        }

        return false;
    }

    private static void handleInvalidLicense(JavaPlugin plugin) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().severe("LICENCIA NO VÁLIDA PARA ESTE SERVIDOR, plugin desactivado.");
            Bukkit.getPluginManager().disablePlugin(plugin);
        });
    }

    private static String getPublicIp() {
        try {
            HttpURLConnection con = (HttpURLConnection)(new URL("https://api.ipify.org")).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);

            String var3;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String ip = reader.readLine();
                var3 = ip != null ? ip.trim() : "UNKNOWN";
            }

            return var3;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo obtener la IP pública: ", e);
            return "UNKNOWN";
        }
    }
}
