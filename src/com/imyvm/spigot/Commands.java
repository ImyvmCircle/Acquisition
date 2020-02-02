package com.imyvm.spigot;

import cat.nyaa.nyaacore.Message;
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
import java.util.*;

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
                locations.add(loc + ":" + args[1] + ":" + args[2] + ":" + args[3]); //Location:Material Name:Price
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
            AcqInstance acqInstance = plugin.acq.getCurrentAcq();
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

        if (cmd.equalsIgnoreCase("csell")){
            if (!sender.hasPermission("acquisition.csell")) {
                sender.sendMessage("§4You don't have the permission!");
                return false;
            }
            Player p = (Player) sender;
            if (locations == null || locations.size() == 0) {
                sender.sendMessage("No Chest added!");
                return false;
            }
            if (args.length == 1){
                return false;
            }
            int matCode = -1;
            try {
                matCode = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c错误,请联系管理员处理");
                return false;
            }
            if (matCode == -1 || matCode >= locations.size()){
                sender.sendMessage("§c错误,请联系管理员处理");
                return false;
            }
            String random = locations.get(matCode);
            Location location = Commands.getLiteLocationFromString(random);
            Block chest = location.getBlock();
            int amount = getamount(((Chest) chest.getState()).getInventory());

            if (amount >= 54 * 64) {
                sender.sendMessage("§4收购所已满");
                return false;
            }

            final String[] parts = random.split(":");
            double unitPrice = Double.parseDouble(parts[5]) / 64;
            int remain_amount = 54 * 64 - amount;
            ItemStack itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial(parts[4])), remain_amount);

            double price = forcepurchase(p, remain_amount, itemStack, location, unitPrice);
            DecimalFormat df = new DecimalFormat("0.00");
            if (price < 0) {
                if (price == -1) {
                    sender.sendMessage("§4该物品收购已满");
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
                sender.sendMessage("§4You don't have the permission!");
                return false;
            }
            OpenAcqGUI((Player) sender, locations);
        }

        if (cmd.equalsIgnoreCase("new")) {
            if (!sender.isOp()) {
                sender.sendMessage("§4You don't have the permission!");
                return false;
            }
            if (!econ.has(Bukkit.getOfflinePlayer(UUID.fromString(MoneyUUID)), 8)) {
                return false;
            }
            boolean success = plugin.acq.newRequisition();
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
//            lores.add("§4建筑师专用");
            lores.add("§b点击购买");
            if (player.hasPermission("acquisition.builder")){
                lores.add("§b价格: " + parts[6] + " D/组");
            }else {
                lores.add("§b价格: " + parts[5] + " D/组");
            }
            lores.add("§b库存: " + amount);
            lores.add(a);
            meta.setLore(lores);
            item.setItemMeta(meta);
            inv.addItem(item);
        }
        player.openInventory(inv);
        player.updateInventory();
    }

    public double forcepurchase(Player p, int amountRemains, ItemStack itemStack, Location location, Double unitPrice) {
        if (amountRemains <= 0) return -1; // 收购所满了
        ItemStack itemHand = p.getInventory().getItemInMainHand();
        if (!itemStack.isSimilar(itemHand)) return -2; //物品不匹配
        int amount = itemHand.getAmount();
        if (amountRemains < amount) amount = amountRemains;
        if (!econ.has(Bukkit.getOfflinePlayer(UUID.fromString(MoneyUUID)), unitPrice * amount)) {
            return -3;  // 收购所钱不足
        }
        int new_amount = itemHand.getAmount() - amount;
        if (new_amount == 0) {
            p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            itemHand.setAmount(new_amount);
        }
        Block chest = location.getBlock();
        if (chest.getType().equals(Material.CHEST)) {
            Inventory inventory = ((Chest) chest.getState()).getInventory();
            ItemStack additem = new ItemStack(itemStack.getType(), amount);
            ItemMeta im = additem.getItemMeta();
            im.setLore(lore);
            additem.setItemMeta(im);
            inventory.addItem(additem);
        } else {
            new Message("123,245箱子不存在").broadcast();
        }
        return unitPrice * amount;
    }
}
