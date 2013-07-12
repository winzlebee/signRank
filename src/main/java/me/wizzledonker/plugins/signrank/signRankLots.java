/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.wizzledonker.plugins.signrank;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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
    
    public boolean setupWorldguardRegion(Block fenceBlock, String owner) {
        
        this.fences.clear();
        
        Block last = fenceBlock;
        Block check = last;
        
        //For all sides of the square
        while (true) {
            this.found = false;
            for (BlockFace direction : faces) {
                fenceBlock  = check.getRelative(direction);
                //Make sure it isn't the same as the last cycle
                if ((last.getX() != fenceBlock.getX()) || (last.getY() != fenceBlock.getY()) || (last.getZ() != fenceBlock.getZ())) {
                    if (plugin.PROTECT_BLOCK_TYPES.contains(fenceBlock.getRelative(direction).getTypeId())) {
                        this.found = true;
                        last = check;
                        check = fenceBlock;
                        //Done checking around for this cycle
                        break;
                    }
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
          plugin.getServer().getLogger().log(Level.WARNING, "Fence wasn't closed in world {0}", fenceBlock.getWorld().getName());
          this.fences.clear();
          return false;
        }
        
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(owner + "lot", blockVectorFromBlock(getPrimaryBlock()), blockVectorFromBlock(getSecondaryBlock()));
        region.setPriority(2);
        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(owner);
        region.setOwners(owners);
        signRank.worldGuard.getRegionManager(fenceBlock.getWorld()).addRegion(region);
        return true;
    }
    
    private BlockVector blockVectorFromBlock(Block block) {
        return new BlockVector(block.getX(), block.getY(), block.getZ());
    }
    
    private Block getPrimaryBlock() {
        Block result = fences.get(0);
        for (Block block : fences) {
            if (block.getX() < result.getX() && block.getZ() < result.getZ()) {
                result = block;
            }
        }
        result.getLocation().setY(0);
        return result;
    }
    
    private Block getSecondaryBlock() {
        Block result = fences.get(0);
        for (Block block : fences) {
            if (block.getX() > result.getX() && block.getZ() > result.getZ()) {
                result = block;
            }
        }
        result.getLocation().setY(256);
        return result;
    }
    
    
}
