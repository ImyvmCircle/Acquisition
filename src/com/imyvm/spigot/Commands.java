package com.imyvm.spigot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Commands implements CommandExecutor {
    Acquisition plugin;

    public Commands(Acquisition pl) {
        plugin = pl;
    }

    //private List<String> player = Acquisition.getplayer();
    private static List<String> locations = Acquisition.getLocations();
    private static List<String> lore = Acquisition.getLore();

    @Override
    public boolean onCommand(CommandSender sender, Command cmdObj, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();
        label = label.toLowerCase();
        String cmd = args[0];

        // Add Chest locations
        if (cmd.equalsIgnoreCase("add")){
            if (!(sender instanceof Player)){
                return false;
            }
            if (sender.hasPermission("acquisition.add")){
                String loc = getLiteStringFromLocation(((Player) sender).getLocation());
                //player.add(((Player) sender).getUniqueId().toString());
                locations.add(loc);
                plugin.getConfig().set("Locations", locations);
                plugin.saveConfig();
                sender.sendMessage("Locations Added!");
            }else {
                sender.sendMessage("You don't have this permission!");
                return false;
            }
        }

        if (cmd.equalsIgnoreCase("update")) {
            if ((sender instanceof Player) || !(sender.isOp())){
                return false;
            }
            if (locations == null || locations.size() == 0) {
                sender.sendMessage("No Chest added!");
                return false;
            }

            for (String loca : locations) {
                //sender.sendMessage("test1");
                Location location = getLiteLocationFromString(loca);
                //sender.sendMessage("test2");
                Block chest = location.getBlock();
                if (chest.getType().equals(Material.CHEST)) {
                    Inventory inventory = ((Chest) chest.getState()).getInventory();
                    if (!empty(inventory) ) {
                        ItemStack[] itemStacks = inventory.getStorageContents();
                        for (int i = 0; i < inventory.getSize(); i++) {
                            if ((itemStacks[i] != null) && !(itemStacks[i].getType().equals(Material.AIR))){
                                ItemMeta im = itemStacks[i].getItemMeta();
                                im.setLore(lore);
                                itemStacks[i].setItemMeta(im);
                            }
                        }
                    }
                }
            }
            sender.sendMessage("Acquisition update success");
        }
        return true;
    }

    private static Location getLiteLocationFromString(String s) {
        if (s == null || s.trim().equalsIgnoreCase("")) {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 4) {
            World w = Bukkit.getServer().getWorld(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(w, x, y, z);
        }
        return null;
    }

    private static String getLiteStringFromLocation(Location loc) {
        if (loc == null) {
            return "";
        }
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }


    private static int getAmount(Inventory inventory) {
        int amount = 0;
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot != null){
                amount += 1;
            }
        }
        return amount;
    }

    private boolean empty(Inventory inventory){
        for(ItemStack it : inventory.getContents())
        {
            if(it != null) return false;
        }
        return true;
    }

}
