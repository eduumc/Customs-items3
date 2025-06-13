package edu.customs.items;

import edu.customs.items.listeners.AttackListener;
import edu.customs.items.listeners.ConsumibleListeners;
import edu.customs.items.listeners.RightClickListener;
import edu.customs.items.util.LangManager;
import edu.customs.items.util.UpdateChecker;
import edu.customs.items.us.com.java.edu.nolook.any.A1;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class ItemActionsPlugin extends JavaPlugin {

    private static ItemActionsPlugin instance;

    private final Map<String, ItemStack> customItems = new HashMap<>();
    private final Map<String, List<String>> customItemRegionBlocks = new HashMap<>();
    private final Set<String> globalRegionBlacklist = new HashSet<>();
    private final Set<String> itemBlacklist = new HashSet<>();

    private boolean blockOnlyListedItems = true;

    public static ItemActionsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.blockOnlyListedItems = getConfig().getBoolean("block-only-listed-items", true);

        // 1. Guardar service_account.json si no existe
        saveServiceAccount();

        // 2. Verificar licencia con manejo de errores mejorado
        verifyLicenseSafely();

        // 3. Cargar configuración del plugin
        loadLocale();
        loadCustomItems();
        loadLists();

        // 4. Registrar eventos y comandos
        getServer().getPluginManager().registerEvents(new ConsumibleListeners(this), this);
        getServer().getPluginManager().registerEvents(new RightClickListener(this), this);
        getServer().getPluginManager().registerEvents(new AttackListener(this), this);
        this.getCommand("customitems").setExecutor(new CustomItemsCommand(this));

        // 5. Verificar actualizaciones
        new UpdateChecker(this, "https://pastebin.com/raw/y3q4y5mD").checkForUpdates();

        getLogger().info("§a[Customs-items] Plugin versión " + getDescription().getVersion() + " habilitado correctamente.");
    }

    private void saveServiceAccount() {
        File file = new File(getDataFolder(), "service_account.json");
        if (!file.exists()) {
            saveResource("service_account.json", false);
            getLogger().info("Archivo service_account.json creado");
        }
    }

    private void verifyLicenseSafely() {
        try {
            // Verificar explícitamente la clase crítica
            Class<?> googleCredentials = Class.forName("shaded.com.google.auth.oauth2.GoogleCredentials");
            getLogger().info("Google Auth cargado desde: " +
                    googleCredentials.getProtectionDomain().getCodeSource().getLocation());

            // Ejecutar verificación de licencia
            A1.checkLicense(this);

        } catch (ClassNotFoundException e) {
            getLogger().severe("ERROR CRÍTICO: Clases de Google Auth no encontradas");
            logDiagnosticInfo();

            // Deshabilitar de forma segura
            Bukkit.getScheduler().runTask(this, () -> {
                Bukkit.getPluginManager().disablePlugin(this);
                getLogger().severe("Plugin deshabilitado por error de dependencias");
            });
        } catch (Exception e) {
            getLogger().severe("Error en verificación de licencia: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logDiagnosticInfo() {
        try {
            // Listar clases en el JAR
            java.util.jar.JarFile jar = new java.util.jar.JarFile(getFile());
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();

            getLogger().severe("=== DIAGNÓSTICO DEL JAR ===");
            getLogger().severe("Ruta del JAR: " + getFile().getAbsolutePath());

            int shadedCount = 0;
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("shaded/com/google/")) {
                    shadedCount++;
                    if (shadedCount <= 10) { // Mostrar solo las primeras 10
                        getLogger().severe("Clase shaded encontrada: " + name);
                    }
                }
            }

            getLogger().severe("Total de clases shaded: " + shadedCount);
            jar.close();

            // Verificar dependencias en tiempo de ejecución
            ClassLoader classLoader = getClassLoader();
            getLogger().severe("ClassLoader: " + classLoader.getClass().getName());

        } catch (Exception ex) {
            getLogger().severe("Error en diagnóstico: " + ex.getMessage());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[Customs-items] Plugin deshabilitado.");
    }

    private void loadLocale() {
        String locale = getConfig().getString("Locale", "En-us");
        String prefix = getConfig().getString("Prefix", "&a[CustomItems] ");
        File langFile = new File(getDataFolder(), "locale/" + locale + ".yml");

        if (!langFile.exists()) {
            saveResource("locale/En-us.yml", false);
        }

        LangManager.loadLanguage(langFile, prefix);
    }

    public void loadCustomItems() {
        customItems.clear();
        customItemRegionBlocks.clear();

        if (!getConfig().isConfigurationSection("items")) {
            getLogger().warning("No se encontró la sección 'items' en config.yml");
            return;
        }

        for (String key : getConfig().getConfigurationSection("items").getKeys(false)) {
            String path = "items." + key;

            Material material = Material.getMaterial(getConfig().getString(path + ".material", "STONE").toUpperCase());
            if (material == null) {
                getLogger().warning("Material inválido para el ítem " + key);
                continue;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                String displayName = getConfig().getString(path + ".name");
                if (displayName != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                }

                List<String> loreConfig = getConfig().getStringList(path + ".lore");
                if (!loreConfig.isEmpty()) {
                    int maxUses = getConfig().getInt(path + ".attack.uses", 0);
                    List<String> lore = new ArrayList<>();
                    for (String line : loreConfig) {
                        String parsed = ChatColor.translateAlternateColorCodes('&', line.replace("%uses%", maxUses > 0 ? String.valueOf(maxUses) : "∞"));
                        lore.add(parsed);
                    }
                    meta.setLore(lore);
                }

                item.setItemMeta(meta);
            }

            customItems.put(key.toLowerCase(), item);

            // Regiones bloqueadas por ítem
            List<String> regionBlockList = getConfig().getStringList(path + ".region-block");
            if (!regionBlockList.isEmpty()) {
                List<String> lowerRegions = new ArrayList<>();
                for (String region : regionBlockList) {
                    lowerRegions.add(region.toLowerCase());
                }
                customItemRegionBlocks.put(key.toLowerCase(), lowerRegions);
            }
        }

        getLogger().info("§a[Customs-items] Ítems personalizados cargados: " + customItems.keySet());
    }

    public void loadLists() {
        loadGlobalRegionBlacklist();
        loadItemBlacklist();
    }

    private void loadGlobalRegionBlacklist() {
        globalRegionBlacklist.clear();
        for (String region : getConfig().getStringList("region-blacklist")) {
            globalRegionBlacklist.add(region.toLowerCase());
        }
        getLogger().info("§a[Customs-items] Regiones bloqueadas globalmente: " + globalRegionBlacklist);
    }

    private void loadItemBlacklist() {
        itemBlacklist.clear();
        for (String item : getConfig().getStringList("item-blacklist")) {
            itemBlacklist.add(item.toLowerCase());
        }
        getLogger().info("§a[Customs-items] Ítems bloqueados globalmente: " + itemBlacklist);
    }

    public boolean isRegionBlockedGlobally(String region, String itemKey) {
        region = region.toLowerCase();
        itemKey = itemKey.toLowerCase();
        return globalRegionBlacklist.contains(region) &&
                (!blockOnlyListedItems || itemBlacklist.contains(itemKey));
    }

    public boolean isRegionBlockedForItem(String itemKey, String region) {
        List<String> blockedRegions = customItemRegionBlocks.get(itemKey.toLowerCase());
        return blockedRegions != null && blockedRegions.contains(region.toLowerCase());
    }

    public ItemStack getCustomItemByName(String name) {
        return name == null ? null : customItems.get(name.toLowerCase());
    }

    public Map<String, List<String>> getCustomItemRegionBlocks() {
        return customItemRegionBlocks;
    }

    public Set<String> getGlobalRegionBlacklist() {
        return globalRegionBlacklist;
    }

    public Set<String> getItemBlacklist() {
        return itemBlacklist;
    }

    public boolean isBlockOnlyListedItems() {
        return blockOnlyListedItems;
    }
}