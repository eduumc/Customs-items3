package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.ActionExecutor;
import edu.customs.items.util.ColorUtil;
import edu.customs.items.util.CooldownManager;
import edu.customs.items.util.ItemUtils;
import edu.customs.items.util.LangManager;
import edu.customs.items.util.WGUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ConsumibleListeners implements Listener {
    private final ItemActionsPlugin plugin;

    public ConsumibleListeners(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.hasItemMeta()) {
            ConfigurationSection itemsSection = this.plugin.getConfig().getConfigurationSection("items");
            if (itemsSection != null) {
                List<String> globalRegionBlacklist = this.plugin.getConfig().getStringList("region-blacklist");
                List<String> globalItemBlacklist = this.plugin.getConfig().getStringList("item-blacklist");

                for(String key : itemsSection.getKeys(false)) {
                    String basePath = "items." + key;
                    String material = this.plugin.getConfig().getString(basePath + ".material");
                    String nameConfig = this.plugin.getConfig().getString(basePath + ".name");
                    if (material != null && nameConfig != null && item.getType().name().equalsIgnoreCase(material)) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && meta.hasDisplayName()) {
                            String displayName = meta.getDisplayName();
                            if (ColorUtil.matchColorName(nameConfig, displayName)) {
                                List<String> itemRegionBlock = this.plugin.getConfig().getStringList(basePath + ".region-block");
                                boolean canUse = WGUtils.canUseItem(player, player.getLocation(), globalRegionBlacklist, globalItemBlacklist, key, material, itemRegionBlock);
                                if (!canUse) {
                                    player.sendMessage(ColorUtil.format(LangManager.get("blocked")));
                                    event.setCancelled(true);
                                    return;
                                }

                                long cooldown = this.plugin.getConfig().getLong(basePath + ".consume.cooldown", 0L);
                                if (CooldownManager.hasCooldown(player, key)) {
                                    long remaining = CooldownManager.getRemaining(player, key);
                                    String prefix = this.plugin.getConfig().getString("Prefix", "&7[Items] ");
                                    player.sendMessage(ColorUtil.format(prefix + "&cTienes que esperar " + remaining + "s para volver a usar este ítem."));
                                    event.setCancelled(true);
                                    return;
                                }

                                List<String> commands = this.plugin.getConfig().getStringList(basePath + ".consume.commands");
                                List<String> messages = this.plugin.getConfig().getStringList(basePath + ".consume.messages");
                                commands = this.replaceVariables(commands, player.getName(), player.getName());
                                messages = this.replaceVariables(messages, player.getName(), player.getName());
                                ActionExecutor.run(this.plugin, player, player, commands, messages);
                                if (cooldown > 0L) {
                                    CooldownManager.setCooldown(player, key, cooldown);
                                }

                                if (!ItemUtils.shouldReduce(this.plugin, key, "consume")) {
                                    event.setCancelled(true);
                                } else {
                                    int reduceAmount = this.plugin.getConfig().getInt(basePath + ".consume.reduce-amount", 1);
                                    int newAmount = item.getAmount() - reduceAmount;
                                    if (newAmount <= 0) {
                                        player.getInventory().removeItem(new ItemStack[]{item});
                                        player.sendMessage(LangManager.get("item-consumed"));
                                    } else {
                                        item.setAmount(newAmount);
                                        player.getInventory().setItemInMainHand(item);
                                        player.sendMessage(LangManager.get("item-partially-consumed"));
                                    }
                                }
                                break;
                            }
                        }
                    }
                }

            }
        }
    }

    private List<String> replaceVariables(List<String> list, String executor, String target) {
        if (list == null) {
            return Collections.emptyList();
        } else {
            List<String> replaced = new ArrayList();

            for(String line : list) {
                line = line.replace("%executor%", executor).replace("%target%", target);
                replaced.add(ColorUtil.format(line));
            }

            return replaced;
        }
    }
}
