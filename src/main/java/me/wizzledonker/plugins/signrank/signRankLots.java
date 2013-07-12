/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.wizzledonker.plugins.signrank;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 *
 * @author Winfried
 */
public class signRankLots {
    public signRank plugin;
    
    public signRankLots(signRank instance) {
        plugin = instance;
    }
    
    private boolean found = false;
    private List<Block> fences = new ArrayList<Block>();
    
    //The faces to be used when checking
    BlockFace[] faces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };
    
    public boolean setupWorldguardRegion(Block signBlock, String owner) {
        
        this.fences.clear();
        
        Block last = signBlock;
        Block check = last;
        
        //For all sides of the square
        while (true) {
            this.found = false;
            int count = 0;
            for (BlockFace direction : faces) {
                signBlock  = check.getRelative(direction);
                //Make sure it isn't the same as the last cycle
                if ((last.getX() == signBlock.getX()) && (last.getY() == signBlock.getY()) && (last.getZ() == signBlock.getZ()))
                    continue;
                if (plugin.PROTECT_BLOCK_TYPES.contains(signBlock.getTypeId())) {
                    this.found = true;
                    last = check;
                    check = signBlock;
                    System.out.println("Added new FenceBlock at " + signBlock.getLocation());
                    count++;
                    //Done checking around for this cycle
                    break;
                }
            }
            System.out.println(count);
            if (!this.found) {
              break;
            }
            if (this.fences.contains(check))
              break;
            this.fences.add(check);
        }
        
        if (!this.found)
        {
          plugin.getServer().getLogger().log(Level.WARNING, "Fence wasn't closed in world {0}", signBlock.getWorld().getName());
          this.fences.clear();
          return false;
        }
        
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(owner + "lot", blockVectorFromLoc(getPrimaryLoc()), blockVectorFromLoc(getSecondaryLoc()));
        region.setPriority(2);
        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(owner);
        region.setOwners(owners);
        signRank.worldGuard.getRegionManager(signBlock.getWorld()).addRegion(region);
        try {
            signRank.worldGuard.getRegionManager(signBlock.getWorld()).save();
        } catch (ProtectionDatabaseException ex) {
            Logger.getLogger(signRankLots.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
    
    private BlockVector blockVectorFromLoc(Location loc) {
        return new BlockVector(loc.getX(), loc.getY(), loc.getZ());
    }
    
    private Location getPrimaryLoc() {
        Block result = fences.get(0);
        for (Block block : fences) {
            if (block.getX() < result.getX() || block.getZ() < result.getZ()) {
                result = block;
            }
        }
        Location end = result.getLocation();
        end.setY(0);
        return end;
    }
    
    private Location getSecondaryLoc() {
        Block result = fences.get(0);
        for (Block block : fences) {
            if (block.getX() > result.getX() || block.getZ() > result.getZ()) {
                result = block;
            }
        }
        Location end = result.getLocation();
        end.setY(256);
        return end;
    }
    
    
}
