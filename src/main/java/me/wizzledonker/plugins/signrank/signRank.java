package me.wizzledonker.plugins.signrank;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import me.wizzledonker.plugins.oblicomranks.OblicomRankScore;
import me.wizzledonker.plugins.oblicomranks.OblicomRanks;
import org.bukkit.plugin.java.JavaPlugin;

//Importing milkbowl api
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

//Other Imports
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;

public class signRank extends JavaPlugin {
    public List<Integer> PROTECT_BLOCK_TYPES;
    public List<String> IGNORE_REGIONS;
    public List<String> FACTION_REGIONS;
    
    public static Economy economy = null;
    public static Permission permission = null;
    public static WorldGuardPlugin worldGuard = null;
    
    public OblicomRankScore scores = null;
    
    Listener mainListener = new signRankListener(this);
    public signRankLots lots = null;
    
    @Override
    public void onEnable() {
        PluginManager pm = this.getServer().getPluginManager();
        worldGuard = getWorldGuard();
        if (setupEconomy() && setupPermission() && setupRanks() && (worldGuard != null)) {
            System.out.println(getPluginName() + " permission + economy + protection system successfully loaded! Permissions: " + permission.getName() + "Economy: " + economy.getName());
        } else {
            System.out.println(getPluginName() + " Failed to initialise permissions/economy/worldguard via vault. Disabling...");
            pm.disablePlugin(this);
            return;
        }
        
        lots = new signRankLots(this);
        
        //Materials the fence may be created from
        if (!getConfig().getIntegerList("protect_blocks").isEmpty()) {
            PROTECT_BLOCK_TYPES = getConfig().getIntegerList("protect_blocks");
        } else {
            PROTECT_BLOCK_TYPES = Arrays.asList(85, 107);
            getConfig().set("protect_blocks", PROTECT_BLOCK_TYPES);
        }
        
        //Regions to dissallow the protection of blocks
        if (!getConfig().getStringList("ignore_regions").isEmpty()) {
            IGNORE_REGIONS = getConfig().getStringList("ignore_regions");
        } else {
            IGNORE_REGIONS = Arrays.asList("city", "shops");
            getConfig().set("ignore_regions", IGNORE_REGIONS);
        }
        
        //Regions which will result in a change of group when a lot is purchased
        if (!getConfig().getStringList("faction_regions").isEmpty()) {
            FACTION_REGIONS = getConfig().getStringList("faction_regions");
        } else {
            FACTION_REGIONS = Arrays.asList("police", "criminals");
            getConfig().set("faction_regions", FACTION_REGIONS);
        }
        
        saveConfig();
        
        PROTECT_BLOCK_TYPES = getConfig().getIntegerList("protect_blocks");
        pm.registerEvents(mainListener, this);
        System.out.println(getPluginName() + " for oblicom.com successfully started");
    }

    @Override
    public void onDisable() {
        System.out.println(getPluginName() + " for oblicom.com shutting down.");
    }
    
    public double determineValue(String type) {
        double price = 500.00;
        price = getConfig().getDouble("lot_prices." + type + ".price");
        return price;
    }
    
    public String determineLotType(ProtectedRegion region) {
        int size = Math.abs(region.getMaximumPoint().getBlockX() - region.getMinimumPoint().getBlockX())*Math.abs(region.getMaximumPoint().getBlockZ() - region.getMinimumPoint().getBlockZ());
        String selection = getSmallestLot();
        String prev = getSmallestLot();
        for (String current : getConfig().getConfigurationSection("lot_prices").getKeys(false)) {
            if ((getConfig().getInt("lot_prices." + prev + ".size") < size) && (getConfig().getInt("lot_prices." + current + ".size") > size)) {
                if (getConfig().getInt("lot_prices." + selection + ".size") > getConfig().getInt("lot_prices." + current + ".size")) {
                    selection = current;
                }
            }
        }
        this.getServer().getLogger().log(Level.INFO, "A lot was sold of size {0} as {1}", new Object[]{size, selection});
        return selection;
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
        //Find the WorldGuard region
        if (!player.hasPermission("SignRank.exempt")) {
            RegionManager rm = worldGuard.getRegionManager(player.getWorld());
            ApplicableRegionSet set = rm.getApplicableRegions(player.getLocation());
            String rank = "citizen";
            for (ProtectedRegion region : set) {
                //Analyse all the regions and find the lowest priority
                if (!IGNORE_REGIONS.contains(region.getId())) {
                    rank = region.getOwners().getGroups().iterator().next();
                }
            }
           for (String group : permission.getPlayerGroups(player)) {
               permission.playerRemoveGroup(player, group);
           }
           permission.playerAddGroup(player, rank);
        }
        player.performCommand("sethome");
        scores.addScore(getConfig().getInt("lot_score", 100), player);
    }
    
    private Boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
    
    private Boolean setupPermission() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider .getProvider();
        }

        return (permission != null);
    }
    
    private String getPluginName() {
        return "[" + this + "]";
    }
    
    private boolean setupRanks() {
        Plugin pl = getServer().getPluginManager().getPlugin("oblicomRanks");
        if (pl == null) {
            return false;
        }
        OblicomRanks oblicomRanks = (OblicomRanks) pl;
        scores = oblicomRanks.score;
        return (scores != null);
    }
    
    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }
}
