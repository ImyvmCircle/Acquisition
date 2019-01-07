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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.imyvm.spigot.Acquisition.econ;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Commands implements CommandExecutor {
    private Acquisition plugin;

    public Commands(Acquisition pl) {
        plugin = pl;
    }

    private static List<String> locations = Acquisition.getLocations();
    private static List<String> lore = Acquisition.getLore();
    private static String MoneyUUID = Acquisition.getMoneyUUID();

    @Override
    public boolean onCommand(CommandSender sender, Command cmdObj, String label, String[] args) {
        String cmd = args[0];

        // Add Chest locations
        if (cmd.equalsIgnoreCase("add")){
            if (!(sender instanceof Player)){
                return false;
            }
            if (sender.hasPermission("acquisition.add")){
                String loc = getLiteStringFromLocation(((Player) sender).getLocation());
                //player.add(((Player) sender).getUniqueId().toString());
                locations.add(loc + ":" + args[1] + ":" + args[2]); //Location:Material Name:Price
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

        if (cmd.equalsIgnoreCase("sell")) {
            Player p = (Player) sender;
            AcqInstance acqInstance = AcqManager.getCurrentAcq();
            if (acqInstance == null) {
                sender.sendMessage("当前无收购");
                return false;
            }

            int amount;
            if (args.length == 1) {
                amount = Math.min(p.getInventory().getItemInMainHand().getAmount(), acqInstance.getAmountRemains());
            } else if (args.length == 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§e输入指令§r/acq sell [数量(可选)] §e出售手上的物品");
                    return false;
                }
            } else {
                sender.sendMessage("§e输入指令§r/acq sell [数量(可选)] §e出售手上的物品");
                return false;
            }

            if (!acqInstance.canSellAmount(amount)) {
                sender.sendMessage("§4超出收购数量");
                return false;
            }
            double price = acqInstance.purchase(p, amount);
            DecimalFormat df = new DecimalFormat("0.00");
            if (price < 0) {
                if (price == -1) {
                    sender.sendMessage("§4无足够的物品出售");
                    return false;
                } else if (price == -2) {
                    sender.sendMessage("§4所出售物品不匹配");
                    return false;
                } else if (price == -3) {
                    sender.sendMessage("§4收购所资金不足");
                    return false;
                } else {
                    sender.sendMessage("§4出售失败");
                    return false;
                }
            } else {
                sender.sendMessage("§b出售成功,获得" + df.format(price) + " D");
                econ.depositPlayer(p, price);
                econ.withdrawPlayer(Bukkit.getOfflinePlayer(UUID.fromString(MoneyUUID)), price);
            }
        }

        if (cmd.equalsIgnoreCase("buy")) {
            if (!sender.hasPermission("acquisition.buy")) {
                return false;
            }
            OpenAcqGUI((Player) sender, locations);
        }
        return true;
    }

    public static Location getLiteLocationFromString(String s) {
        if (s == null || s.trim().equalsIgnoreCase("")) {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length >= 4) {
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


    public static int getAmount(Inventory inventory) {
        int amount = 0;
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot != null){
                amount += 1;
            }
        }
        return amount;
    }

    public static int getamount(Inventory inventory) {
        int amount = 0;
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot != null) {
                amount += slot.getAmount();
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

    public static void OpenAcqGUI(Player player, List<String> locations) {
        Inventory inv = Bukkit.createInventory(null, 27, "§bAcquisition");
        for (String a : locations) {
            final String[] parts = a.split(":");

            Location location = getLiteLocationFromString(a);
            Block chest = location.getBlock();
            int amount;
            if (chest.getType().equals(Material.CHEST)) {
                amount = getamount(((Chest) chest.getState()).getInventory());
            } else {
                player.sendMessage("§4错误：请联系管理员");
                return;
            }
            ItemStack item = new ItemStack(Material.getMaterial(parts[4]));
            ItemMeta meta = item.getItemMeta();
            List<String> lores = new ArrayList<>();
            lores.add("§4建筑师专用");
            lores.add("§b点击购买");
            lores.add("§b价格: " + parts[6] + " D/组");
            lores.add("§b库存: " + amount);
            lores.add(a);
            meta.setLore(lores);
            item.setItemMeta(meta);
            inv.addItem(item);
        }
        player.openInventory(inv);
        player.updateInventory();
    }
}
