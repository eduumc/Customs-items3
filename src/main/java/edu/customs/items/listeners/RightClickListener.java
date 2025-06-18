package edu.customs.items.listeners;

import edu.customs.items.ItemActionsPlugin;
import edu.customs.items.util.ActionExecutor;
import edu.customs.items.util.ColorUtil;
import edu.customs.items.util.CooldownManager;
import edu.customs.items.util.LangManager;
import edu.customs.items.util.VersionUtils;
import edu.customs.items.util.WGUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RightClickListener implements Listener {
    private final ItemActionsPlugin plugin;

    public RightClickListener(ItemActionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = VersionUtils.getItemInHand(player);
        if (item != null && item.hasItemMeta()) {
            ConfigurationSection itemsSection = this.plugin.getConfig().getConfigurationSection("items");
            if (itemsSection != null) {
                for(String key : itemsSection.getKeys(false)) {
                    String basePath = "items." + key;
                    String material = this.plugin.getConfig().getString(basePath + ".material");
                    String nameConfig = this.plugin.getConfig().getString(basePath + ".name");
                    if (material != null && nameConfig != null && item.getType().name().equalsIgnoreCase(material)) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && meta.hasDisplayName()) {
                            String displayName = meta.getDisplayName();
                            if (ColorUtil.matchColorName(nameConfig, displayName)) {
                                boolean canUse = WGUtils.canUseItem(player, player.getLocation(), this.plugin.getGlobalRegionBlacklist().stream().toList(), this.plugin.getItemBlacklist().stream().toList(), key, material, (List)this.plugin.getCustomItemRegionBlocks().getOrDefault(key.toLowerCase(), Collections.emptyList()));
                                if (!canUse) {
                                    player.sendMessage(ColorUtil.format(LangManager.get("blocked")));
                                    event.setCancelled(true);
                                    return;
                                }

                                long cooldown = this.plugin.getConfig().getLong(basePath + ".right-click.cooldown", 0L);
                                if (CooldownManager.hasCooldown(player, key)) {
                                    long remaining = CooldownManager.getRemaining(player, key);
                                    String prefix = this.plugin.getConfig().getString("Prefix", "&7[Items] ");
                                    player.sendMessage(ColorUtil.format(prefix + "&cTienes que esperar " + remaining + "s para volver a usar este ítem."));
                                    event.setCancelled(true);
                                    return;
                                }

                                List<String> commands = this.plugin.getConfig().getStringList(basePath + ".right-click.commands");
                                List<String> messages = this.plugin.getConfig().getStringList(basePath + ".right-click.messages");
                                commands = this.replaceVariables(commands, player.getName(), "Ninguno");
                                messages = this.replaceVariables(messages, player.getName(), "Ninguno");
                                ActionExecutor.run(this.plugin, player, (Entity)null, commands, messages);
                                if (cooldown > 0L) {
                                    CooldownManager.setCooldown(player, key, cooldown);
                                }

                                int maxUses = this.plugin.getConfig().getInt(basePath + ".right-click.uses", 0);
                                int reduceAmount = this.plugin.getConfig().getInt(basePath + ".right-click.reduce-amount", 0);
                                if (maxUses > 0) {
                                    if (meta != null) {
                                        List<String> lore = meta.getLore();
                                        int currentUses = maxUses;
                                        if (lore != null) {
                                            for(String line : lore) {
                                                if (line.matches(".*\\d+.*")) {
                                                    String digits = line.replaceAll("[^0-9]", "");

                                                    try {
                                                        currentUses = Integer.parseInt(digits);
                                                    } catch (NumberFormatException var25) {
                                                    }
                                                    break;
                                                }
                                            }
                                        }

                                        int newUses = currentUses - 1;
                                        if (newUses <= 0) {
                                            player.getInventory().setItemInMainHand((ItemStack)null);
                                            player.sendMessage(LangManager.get("item-broken"));
                                        } else if (lore != null) {
                                            List<String> newLore = new ArrayList();

                                            for(String line : lore) {
                                                newLore.add(ColorUtil.format(line.replace("%uses%", String.valueOf(newUses))));
                                            }

                                            meta.setLore(newLore);
                                            item.setItemMeta(meta);
                                            player.getInventory().setItemInMainHand(item);
                                        }
                                    }
                                } else if (reduceAmount > 0) {
                                    int newAmount = item.getAmount() - reduceAmount;
                                    if (newAmount <= 0) {
                                        player.getInventory().setItemInMainHand((ItemStack)null);
                                        player.sendMessage(LangManager.get("item-consumed"));
                                    } else {
                                        item.setAmount(newAmount);
                                        player.getInventory().setItemInMainHand(item);
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
