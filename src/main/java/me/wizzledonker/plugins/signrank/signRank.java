package me.wizzledonker.plugins.signrank;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import java.util.Arrays;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

//Importing milkbowl api
import net.milkbowl.vault.economy.Economy;

//Other Imports
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;

public class signRank extends JavaPlugin {
    public List<String> PROTECT_BLOCK_TYPES;
    public List<String> IGNORE_REGIONS;
    public List<String> FACTION_REGIONS;
    
    public int PROTECT_DISTANCE;
    public int PROTECT_HEIGHT;
    public int MAX_REGIONS;
    
    public static Economy economy = null;
    public static WorldGuardPlatform worldGuard = null;
    
    Listener mainListener = new signRankListener(this);
    public signRankLots lots = null;
    
    @Override
    public void onEnable() {
        PluginManager pm = this.getServer().getPluginManager();
        worldGuard = getWorldGuard();
        if (setupEconomy() && (worldGuard != null)) {
            System.out.println(getPluginName() + " economy + protection system successfully loaded! Economy: " + economy.getName());
        } else {
            System.out.println(getPluginName() + " Failed to initialise economy/worldguard via vault. Disabling...");
            pm.disablePlugin(this);
            return;
        }
        
        lots = new signRankLots(this);
        
        //Materials the fence may be created from
        if (!getConfig().getIntegerList("protect_blocks").isEmpty()) {
            PROTECT_BLOCK_TYPES = getConfig().getStringList("protect_blocks");
        } else {
            PROTECT_BLOCK_TYPES = Arrays.asList("OAK_FENCE", "OAK_FENCE_GATE");
            getConfig().set("protect_blocks", PROTECT_BLOCK_TYPES);
        }
        
        //Regions to dissallow the protection of blocks
        if (!getConfig().getStringList("ignore_regions").isEmpty()) {
            IGNORE_REGIONS = getConfig().getStringList("ignore_regions");
        } else {
            IGNORE_REGIONS = Arrays.asList("city", "shops");
            getConfig().set("ignore_regions", IGNORE_REGIONS);
        }
        
        //Distance underground the protection will reach
        if (getConfig().isInt("protect_distance")) {
            PROTECT_DISTANCE = getConfig().getInt("protect_distance");
        } else {
            PROTECT_DISTANCE = 3;
            getConfig().set("protect_distance", PROTECT_DISTANCE);
        }
        
        //Distance above ground protection will reach
        if (getConfig().isInt("protect_height")) {
            PROTECT_HEIGHT = getConfig().getInt("protect_height");
        } else {
            PROTECT_HEIGHT = 256;
            getConfig().set("protect_height", PROTECT_HEIGHT);
        }
        
        if (getConfig().isInt("max_regions")) {
            MAX_REGIONS = getConfig().getInt("max_regions");
        } else {
            MAX_REGIONS = 15;
            getConfig().set("max_regions", MAX_REGIONS);
        }
        
        saveConfig();
        
        PROTECT_BLOCK_TYPES = getConfig().getStringList("protect_blocks");
        pm.registerEvents(mainListener, this);
        System.out.println(getPluginName() + " for oblicom.com successfully started");
    }

    @Override
    public void onDisable() {
        System.out.println(getPluginName() + " for oblicom.com shutting down.");
    }
    
    public double determineValue(String type) {
        double price = getConfig().getDouble("lot_prices." + type + ".price");
        
        if (price <= 0) {
            price = getConfig().getDouble("lot_prices.default.price");
        }
        
        return price;
    }
    
    public String getSmallestLot() {
        String current = "regular";
        for (String currentType : getConfig().getConfigurationSection("lot_prices").getKeys(false)) {
            if (getConfig().getInt("lot_prices." + currentType + ".size") < getConfig().getInt("lot_prices." + current + ".size")) {
                current = currentType;
            }
        }
        return current;
    }
    
    public void ChargeAndPromote(Player player, Double amount, String world) {
        economy.withdrawPlayer(player, amount);
        //player.performCommand("sethome"); Leave this up to the player
        //Find the WorldGuard region
        
        //The below code is legacy code from when the player was promoted
        /* if (!player.hasPermission("SignRank.exempt")) {
            RegionManager rm = worldGuard.getRegionManager(player.getWorld());
            ApplicableRegionSet set = rm.getApplicableRegions(player.getLocation());
            String rank = "";
            for (ProtectedRegion region : set) {
                //Analyse all the regions and find the lowest priority
                if (!IGNORE_REGIONS.contains(region.getId())) {
                    if (region.getOwners().getGroups().iterator().hasNext()) {
                        rank = region.getOwners().getGroups().iterator().next();
                    }
                }
            }
            if (!rank.isEmpty()) {
                this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "pex user " + player.getName() + " group set "+rank);
            }
        }*/

    }
    
    private Boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
    
    private String getPluginName() {
        return "[" + this + "]";
    }
    
    private WorldGuardPlatform getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return WorldGuard.getInstance().getPlatform();
    }
}
