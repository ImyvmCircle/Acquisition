package com.imyvm.spigot;

import cat.nyaa.nyaacore.Message;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import static com.imyvm.spigot.Acquisition.econ;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

public class AcqInstance {
    private final Runnable finishCallback;
    private final double unitPrice;
    private final String price;
    private BukkitRunnable timeoutListener;
    private ItemStack templateItem;
    private Location location;
    private int amountRemains;
    private long endTime;
    private Acquisition plugin;
    private static int timeoutTicks = Acquisition.getTimeoutTicks();
    private static List<String> lore = Acquisition.getLore();
    private static String MoneyUUID = Acquisition.getMoneyUUID();

    public AcqInstance(
            ItemStack templateItem,
            double unitPrice, int AcqAmount, Location location,
            Acquisition plugin, Runnable finishCallback) {
        DecimalFormat df = new DecimalFormat("0.00");
        this.plugin = plugin;
        this.finishCallback = finishCallback;
        this.unitPrice = unitPrice;
        this.price = df.format(unitPrice);
        this.templateItem = templateItem;
        this.amountRemains = AcqAmount;
        this.location = location;
        this.endTime = System.currentTimeMillis() + timeoutTicks * 50;

        timeoutListener = new TimeoutListener();
        timeoutListener.runTaskLater(plugin, timeoutTicks);
        new Message("§b收购: §r").append(this.templateItem).append(", §b收购时长: §r" + timeoutTicks / 20 + "秒," +
                " §b单价：§r" + price + " D").broadcast();
        new Message("§e输入指令§r/acq sell [数量(可选)] §e出售手上的物品").broadcast();
    }

    public boolean canSellAmount(int amount) {
        return amountRemains <= -1 || amountRemains >= amount;
    }

    public int getAmountRemains() {
        return amountRemains;
    }

    /**
     * @return zero or positive: give that much money to player
     * -1: not enough item in hand
     * -2: item not match
     * -3: money not enough
     */
    public double purchase(Player p, int amount) {
        ItemStack itemHand = p.getInventory().getItemInMainHand();
        if (itemHand.getAmount() < amount) return -1;
        if (!templateItem.isSimilar(itemHand)) return -2;
        if (amountRemains < amount && amountRemains >= 0) amount = amountRemains;
        if (!econ.has(Bukkit.getOfflinePlayer(UUID.fromString(MoneyUUID)), unitPrice * amount)) {
            return -3;
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
            ItemStack additem = new ItemStack(templateItem.getType(), amount);
            ItemMeta im = additem.getItemMeta();
            im.setLore(lore);
            additem.setItemMeta(im);
            inventory.addItem(additem);
        } else {
            new Message("123,245箱子不存在").broadcast();
        }

        if (amountRemains >= 0) amountRemains -= amount;
        new Message(p.getDisplayName() + " §b卖出: §r" + amount + ", §b剩余收购量: §r" + amountRemains).broadcast();
        if (amountRemains == 0) {
            new Message("收购完成，结束").broadcast();
            timeoutListener.cancel();
            finishCallback.run();
        }
        return unitPrice * amount;
    }

    private class TimeoutListener extends BukkitRunnable {
        @Override
        public void run() {
            finishCallback.run();
            amountRemains = 0;
            new Message("收购结束").broadcast();
        }
    }

    public class AcqHintTimer extends BukkitRunnable {
        private final AcqManager manager;

        public AcqHintTimer(AcqManager manager, int interval, JavaPlugin plugin) {
            super();
            this.manager = manager;
            runTaskTimer(plugin, interval, interval);
        }

        @Override
        public void run() {
            if (AcqInstance.this != manager.getCurrentAcq()
                    || amountRemains <= 0
                    || endTime - System.currentTimeMillis() < 100) {
                cancel();
            } else {
                new Message("§b当前正在收购: §r").append(templateItem).append(", §b剩余收购量：§r" +
                        amountRemains + ", §b单价：§r" + price + " D" + ", §b剩余时间：§r" +
                        (endTime - System.currentTimeMillis()) / 1000 + "秒").broadcast();
            }

        }
    }
}
