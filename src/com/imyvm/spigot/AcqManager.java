package com.imyvm.spigot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import static com.imyvm.spigot.Acquisition.econ;

import java.util.*;

public class AcqManager extends BukkitRunnable {
    private final Acquisition plugin;
    private static AcqInstance currentReq = null;
    private int minimalPlayer = Acquisition.getMinimalPlayer();
    private static List<String> locations = Acquisition.getLocations();
    private static int HintInterval = Acquisition.getHintInterval();
    private static String MoneyUUID = Acquisition.getMoneyUUID();

    public AcqManager(Acquisition plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        runTaskTimerAsynchronously(plugin, config.getInt("IntervalTicks"), config.getInt("IntervalTicks"));
    }

    @Override
    public void run() {
        if (Bukkit.getOnlinePlayers().size() < minimalPlayer) {
            return;
        }
        if (!econ.has(Bukkit.getOfflinePlayer(UUID.fromString(MoneyUUID)), 8)) {
            return;
        }
        (new BukkitRunnable() {
            @Override
            public void run() {
                newRequisition();
            }
        }).runTaskLater(plugin, 1000);
    }

    public boolean newRequisition() {
        if (currentReq != null) return false;
        if (locations.isEmpty()) return false;
        int loc = new Random().nextInt(locations.size());
        String random = locations.get(loc);

        Location location = Commands.getLiteLocationFromString(random);
        Block chest = location.getBlock();
        int amount = getamount(((Chest) chest.getState()).getInventory());
        if (amount >= 54 * 64) {
            for (String loca : locations) {
                location = Commands.getLiteLocationFromString(loca);
                chest = location.getBlock();
                amount = getamount(((Chest) chest.getState()).getInventory());
                random = loca;
                if (amount < 54 * 64) {
                    break;
                }
            }
        }
        if (amount >= 54 * 64) {
            Bukkit.broadcastMessage("§4收购所已满");
            return false;
        }

        final String[] parts = random.split(":");
        double price = Double.valueOf(parts[5]) / 64;
        int remain_amount = 54 * 64 - amount;
        ItemStack itemStack = new ItemStack(Material.getMaterial(parts[4]), remain_amount);

        return newRequisition(itemStack, price, remain_amount, location);
    }

    public boolean newRequisition(ItemStack item, double price, int remain_amount, Location location) {
        if (currentReq != null) return false;
        if (item == null) return false;
        currentReq = new AcqInstance(item, price, remain_amount, location, plugin, () -> this.currentReq = null);
        if (HintInterval > 0) {
            currentReq.new AcqHintTimer(this, HintInterval, plugin);
        }
        return true;
    }

    public static AcqInstance getCurrentAcq() {
        return currentReq;
    }


    private static int getamount(Inventory inventory) {
        int amount = 0;
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot != null) {
                amount += slot.getAmount();
            }
        }
        return amount;
    }

}
