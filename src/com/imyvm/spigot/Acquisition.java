package com.imyvm.spigot;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class Acquisition extends JavaPlugin {
    private static final Logger log = Logger.getLogger("Acquisition");
    private FileConfiguration config = getConfig();

    private static List<String> locations = new ArrayList<>();
    private static List<String> lore = new ArrayList<>(Arrays.asList("建筑师专用"));
    private static int MinimalPlayer;
    private static int timeoutTicks;
    private static int HintInterval;
    private static String MoneyUUID;
    public static Economy econ = null;
    public AcqManager acq;

    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(),
                getDescription().getVersion()));
    }

    @Override
    public void onEnable() {
        log.info(String.format("[%s] Enabled Version %s", getDescription().getName(), getDescription().getVersion()));

        config.addDefault("Locations", locations);
        config.addDefault("Lore", lore);
        config.addDefault("Update Time", 5);
        config.addDefault("IntervalTicks", 100);  // 循环时间
        config.addDefault("MinimalPlayer", 5);
        config.addDefault("timeoutTicks", 600);   // 超时
        config.addDefault("HintInterval", 200);   // 探测
        config.addDefault("MoneyUUID", "a641c611-21ef-4b71-b327-e45ef8fdf647");
        config.options().copyDefaults(true);
        saveConfig();


        locations = config.getStringList("Locations");
        lore = config.getStringList("Lore");
        MinimalPlayer = config.getInt("MinimalPlayer");
        timeoutTicks = config.getInt("timeoutTicks");
        HintInterval = config.getInt("HintInterval");
        MoneyUUID = config.getString("MoneyUUID");
        Integer t = config.getInt("Update Time");
        System.out.println(t);

        getCommand("acq").setExecutor(new Commands(this));
        acq = new AcqManager(this);
        InvListener InvListener = new InvListener(this);

        RegisteredServiceProvider<Economy> economyP = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyP != null)
            econ = economyP.getProvider();
        else
            Bukkit.getLogger().info("Unable to initialize Economy Interface with Vault!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEcononomy() {
        return econ;
    }

    public static List<String> getLocations() {
        return locations;
    }

    public static List<String> getLore() {
        return lore;
    }

    public static int getMinimalPlayer() {
        return MinimalPlayer;
    }

    public static int getTimeoutTicks() {
        return timeoutTicks;
    }

    public static int getHintInterval() {
        return HintInterval;
    }

    public static String getMoneyUUID() {
        return MoneyUUID;
    }

}
