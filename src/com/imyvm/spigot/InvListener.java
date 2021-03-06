package com.imyvm.spigot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.imyvm.spigot.Acquisition.econ;

import java.util.List;
import java.util.UUID;

import static com.imyvm.spigot.Commands.getAmount;
import static com.imyvm.spigot.Commands.getLiteLocationFromString;
import static com.imyvm.spigot.Commands.getamount;

public class InvListener implements Listener {
    private final Acquisition plugin;
    private static List<String> locations = Acquisition.getLocations();
    private static String MoneyUUID = Acquisition.getMoneyUUID();

    public InvListener(Acquisition pl) {
        plugin = pl;
        plugin.getServer().getPluginManager().registerEvents(this, pl);
    }

    private static List<String> lore = Acquisition.getLore();

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) event.getWhoClicked();
        if (event.getView().getTitle().equalsIgnoreCase("§bAcquisition")) {
            if (event.getCurrentItem()==null || event.getCurrentItem().getType().equals(Material.AIR)){
                event.setCancelled(true);
                return;
            }

            if (!event.getView().getTitle().endsWith("§bAcquisition")){
                event.setCancelled(true);
                return;
            }

            ItemStack itemStack = event.getCurrentItem();
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lores = itemMeta.getLore();

            if (!lores.contains("§b点击购买")) {
                event.setCancelled(true);
                return;
            }
            ItemStack s = event.getCurrentItem();
            String loc = s.getItemMeta().getLore().get(3);
            Location location = getLiteLocationFromString(loc);
            String[] parts = loc.split(":");
            double price = Double.valueOf(parts[5]);
            if (p.hasPermission("acquisition.builder")){
                price = Double.valueOf(parts[6]);
            }
            if (!econ.has(p, price)) {
                p.sendMessage("§4资金不足");
                return;
            }
            Block chest = location.getBlock();
            if (chest.getType().equals(Material.CHEST)) {
                ItemStack item = new ItemStack(s.getType(), 64);
                if (p.hasPermission("acquisition.builder")){
                    ItemMeta meta = item.getItemMeta();
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                Inventory inventory = ((Chest) chest.getState()).getInventory();
                int result = removeItem(inventory, item, p);
                if (result == -1) {
                    p.sendMessage("§4库存不足");
                } else if (result == 0) {
                    p.sendMessage("§4背包空间不足");
                    p.closeInventory();
                } else {
                    p.sendMessage("§b购买成功,花费" + price + " D");
                    econ.withdrawPlayer(p, price);
                    econ.depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(MoneyUUID)), price);
                    Commands.OpenAcqGUI(p, locations);
                }
            }
        }
    }

    private int removeItem(Inventory inventory, ItemStack itemStack, Player player) {
        if (getamount(inventory) < itemStack.getAmount()) {
            return -1;
        }
        if (player.getInventory().getStorageContents().length - getAmount(player.getInventory()) < 1) {
            return 0;
        }
        inventory.removeItem(itemStack);
        player.getInventory().addItem(itemStack);
        return 1;
    }
}