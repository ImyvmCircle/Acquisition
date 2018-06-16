package com.imyvm.spigot;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class Acquisition extends JavaPlugin {
    private static final Logger log = Logger.getLogger("Acquisition");
    private FileConfiguration config = getConfig();

    private static List<String> locations = new ArrayList<>();
    //private static List<String> name = new ArrayList<>();
    private static List<String> lore = new ArrayList<>(Arrays.asList("建筑师专用"));

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
        config.options().copyDefaults(true);
        saveConfig();


        locations = config.getStringList("Locations");
        lore = config.getStringList("Lore");
        Integer t = config.getInt("Update Time");
        System.out.println(t);

        getCommand("acq").setExecutor(new Commands(this));

        // 定时更新一次
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "acq update");
                } catch (CommandException e) {
                    e.printStackTrace();
                }
            }
        }, 2000, 1000 * 60 * t);

        // Bukkit.getServer().getPluginManager().registerEvents(new test(this), this);
    }

    /*
    public static List<String> getplayer(){
        return name;
    }
    */

    public static List<String> getLocations() {
        return locations;
    }

    public static List<String> getLore() {
        return lore;
    }


}
