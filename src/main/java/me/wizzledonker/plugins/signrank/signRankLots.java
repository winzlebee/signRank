/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.wizzledonker.plugins.signrank;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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
    
    public boolean canBeWorldguardRegion(Block signBlock) {
        this.fences.clear();
        
        Block last = signBlock;
        Block check = last;
        
        //For all sides of the square
        while (true) {
            this.found = false;
            for (BlockFace direction : faces) {
                signBlock  = check.getRelative(direction);
                //Make sure it isn't the same as the last cycle
                if ((last.getX() == signBlock.getX()) && (last.getY() == signBlock.getY()) && (last.getZ() == signBlock.getZ()))
                    continue;
                if (plugin.PROTECT_BLOCK_TYPES.contains(signBlock.getType().toString())) {
                    this.found = true;
                    last = check;
                    check = signBlock;
                    //Done checking around for this cycle
                    break;
                }
            }
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
        return true;
    }
    
    public boolean setupWorldguardRegion(Block signBlock, String owner) {
        
        if (!canBeWorldguardRegion(signBlock)) {
            return false;
        }
        String id = owner + "lot";
        if (signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(signBlock.getWorld())).hasRegion(id)) {
            for (int i = 1; i > 0; i++) {
                if (!signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(signBlock.getWorld())).hasRegion(id + i)) {
                    id = id + i;
                    break;
                }
            }
        }
        
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(id, blockVectorFromLoc(getPrimaryLoc()), blockVectorFromLoc(getSecondaryLoc()));
        region.setPriority(2);
        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(owner);
        region.setOwners(owners);
        signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(signBlock.getWorld())).addRegion(region);
        try {
            signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(signBlock.getWorld())).save();
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.WARNING, ex.toString());
        }
        return true;
    }
    
    public int getAmountOfFencesLast() {
        return fences.size();
    }
    
    private BlockVector blockVectorFromLoc(Location loc) {
        return new BlockVector(loc.getX(), loc.getY(), loc.getZ());
    }
    
    private Location getPrimaryLoc() {
        //Gets the primary locator for the lot... including the ground
        Block result = fences.get(0);
        for (Block block : fences) {
            if (block.getX() < result.getX() || block.getZ() < result.getZ()) {
                result = block;
            }
        }
        Location end = result.getLocation();
        end.setY(end.getBlockY()-(plugin.PROTECT_DISTANCE));
        return end;
    }
    
    private Location getSecondaryLoc() {
        //The secondary locator, which will be the maximum X and Z block while having the max Y value
        Block result = fences.get(0);
        for (Block block : fences) {
            if (block.getX() > result.getX() || block.getZ() > result.getZ()) {
                result = block;
            }
        }
        Location end = result.getLocation();
        end.setY(plugin.PROTECT_HEIGHT);
        return end;
    }
    
    
}
