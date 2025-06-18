package edu.customs.items;

import edu.customs.items.listeners.AttackListener;
import edu.customs.items.listeners.ConsumibleListeners;
import edu.customs.items.listeners.RightClickListener;
import edu.customs.items.us.com.java.edu.nolook.any.A1;
import edu.customs.items.util.ColorUtil;
import edu.customs.items.util.LangManager;
import edu.customs.items.util.UpdateChecker;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;

public class ItemActionsPlugin extends JavaPlugin {
    private static ItemActionsPlugin instance;
    private String licenseId;
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
        FileConfiguration cfg = getConfig();
        blockOnlyListedItems = cfg.getBoolean("block-only-listed-items", true);

        try {
            licenseId = loadOrCreateLicenseFile();
        } catch (IOException e) {
            getLogger().severe("No se pudo crear/leer license.txt: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        A1.checkLicense(this, licenseId, () -> {
            getLogger().info("Licencia válida ✓. Iniciando plugin...");
            loadLocale();
            loadCustomItems();
            loadLists();
            Bukkit.getPluginManager().registerEvents(new ConsumibleListeners(this), this);
            Bukkit.getPluginManager().registerEvents(new RightClickListener(this), this);
            Bukkit.getPluginManager().registerEvents(new AttackListener(this), this);
            getCommand("customitems").setExecutor(new CustomItemsCommand(this));
            new UpdateChecker(this, "https://pastebin.com/raw/y3q4y5mD").checkForUpdates();
            getLogger().info("§5[Customs-items] Plugin versión " + getDescription().getVersion() + " habilitado correctamente.");
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[Customs-items] Plugin deshabilitado.");
    }

    private String loadOrCreateLicenseFile() throws IOException {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        Path licensePath = dataFolder.toPath().resolve("license.txt");
        if (Files.notExists(licensePath, new LinkOption[0])) {
            String uuid = UUID.randomUUID().toString();
            Files.write(licensePath, uuid.getBytes(), new OpenOption[0]);
            getLogger().info("Generado nuevo license-id: " + uuid);
            return uuid;
        } else {
            String uuid = Files.readString(licensePath).trim();
            getLogger().info("Usando license-id existente: " + uuid);
            return uuid;
        }
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
                    if (displayName.contains("&#")) {
                        meta.displayName(ColorUtil.toComponent(displayName));
                    } else {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                    }
                }

                List<String> loreConfig = getConfig().getStringList(path + ".lore");
                if (!loreConfig.isEmpty()) {
                    int maxUses = getConfig().getInt(path + ".attack.uses", 0);
                    List<Component> lore = new ArrayList<>();

                    for (String line : loreConfig) {
                        String txt = line.replace("%uses%", maxUses > 0 ? String.valueOf(maxUses) : "∞");
                        lore.add(ColorUtil.toComponent(txt));
                    }

                    meta.lore(lore);
                }

                item.setItemMeta(meta);
            }

            customItems.put(key.toLowerCase(), item);

            List<String> regionBlockList = getConfig().getStringList(path + ".region-block");
            if (!regionBlockList.isEmpty()) {
                List<String> lowerRegions = new ArrayList<>();
                for (String region : regionBlockList) {
                    lowerRegions.add(region.toLowerCase());
                }
                customItemRegionBlocks.put(key.toLowerCase(), lowerRegions);
            }
        }

        getLogger().info("§5[Customs-items] Ítems personalizados cargados: " + customItems.keySet());
    }

    public void loadLists() {
        globalRegionBlacklist.clear();
        for (String region : getConfig().getStringList("region-blacklist")) {
            globalRegionBlacklist.add(region.toLowerCase());
        }
        getLogger().info("§5[Customs-items] Regiones bloqueadas globalmente: " + globalRegionBlacklist);

        itemBlacklist.clear();
        for (String item : getConfig().getStringList("item-blacklist")) {
            itemBlacklist.add(item.toLowerCase());
        }
        getLogger().info("§5[Customs-items] Ítems bloqueados globalmente: " + itemBlacklist);
    }

    public boolean isRegionBlockedGlobally(String region, String itemKey) {
        region = region.toLowerCase();
        itemKey = itemKey.toLowerCase();
        return globalRegionBlacklist.contains(region) && (!blockOnlyListedItems || itemBlacklist.contains(itemKey));
    }

    public boolean isRegionBlockedForItem(String itemKey, String region) {
        List<String> regs = customItemRegionBlocks.get(itemKey.toLowerCase());
        return regs != null && regs.contains(region.toLowerCase());
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
