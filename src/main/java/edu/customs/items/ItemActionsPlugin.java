package edu.customs.items;

import edu.customs.items.listeners.AttackListener;
import edu.customs.items.listeners.RightClickListener;
import edu.customs.items.util.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemActionsPlugin extends JavaPlugin {

    private static ItemActionsPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new RightClickListener(this), this);
        new UpdateChecker(this, "https://pastebin.com/raw/y3q4y5mD").checkForUpdates();
        getServer().getPluginManager().registerEvents(new RightClickListener(this), this);
        getServer().getPluginManager().registerEvents(new AttackListener(this), this);
        getLogger().info("§a[Customs-items] Plugin versión " + getDescription().getVersion() + " habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[Customs-items] Plugin deshabilitado.");
    }

    public static ItemActionsPlugin getInstance() {
        return instance;
    }
}
