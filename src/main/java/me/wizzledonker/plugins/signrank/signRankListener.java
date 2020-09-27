/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.wizzledonker.plugins.signrank;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 *
 * @author Win
 */
public class signRankListener implements Listener {
    public static signRank plugin;
    
    public signRankListener(signRank instance) {
        plugin = instance;
    }
    
    @EventHandler
    public void whenPlayerInteracts(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("SignRank.purchase")) {
            Action action = event.getAction();
            if (!action.equals(Action.RIGHT_CLICK_BLOCK)) {
                return;
            }
            Block block = event.getClickedBlock();

            // Check if we're dealing with a sign
            if (!(block.getState() instanceof Sign)) {
                return;
            }

            if (!((Sign) block.getState()).getLine(0).toLowerCase().contains("[lot]")) {
                return;
            }

            Sign signblock = ((Sign) block.getState());
            String lottype = signblock.getLine(1);
            double value = plugin.determineValue(lottype);
            if (lottype.isEmpty()) {
                value = Double.parseDouble(signblock.getLine(2));
            }

            if (signRank.economy.getBalance(player) < value) {
                player.sendMessage(ChatColor.RED + "You do not have enough money! It costs " + ChatColor.WHITE + signRank.economy.format(value));
                return;
            }
            if (!plugin.lots.setupWorldguardRegion(block, player.getName())) {
                player.sendMessage(ChatColor.RED + "The fence around this lot isn't enclosed. Find another. Notify a mod too!");
                return;
            }
            
            AbstractPlayerActor wgPlayer = (AbstractPlayerActor) BukkitAdapter.adapt(player);
            
            int numRegions = signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(signblock.getWorld())).getRegionCountOfPlayer((LocalPlayer) wgPlayer);
            if (numRegions > plugin.MAX_REGIONS) {
                player.sendMessage(ChatColor.RED + "You own too many lots. You'll have to sell one.");
                return;
            }
            if (lottype.isEmpty()) {
                // TODO: Change this to instead store player UUIDs in a config file then look them up.
                OfflinePlayer playerTo = plugin.getServer().getOfflinePlayer(signblock.getLine(3));
                signRank.economy.depositPlayer(playerTo, value);
                if (playerTo.getPlayer() != null) {
                    playerTo.getPlayer().sendMessage(ChatColor.GREEN + "Your lot has been sold, you have received " +ChatColor.WHITE+ signRank.economy.format(value));
                }
                
            }
            signblock.setLine(0, player.getName() + "'s");
            signblock.setLine(1, lottype + " lot");
            signblock.setLine(2, "");
            signblock.setLine(3, "");
            signblock.update(true);
            plugin.getServer().broadcastMessage(ChatColor.GREEN + player.getName() + " has bought a" +
                    (lottype.isEmpty() ? "" : " "+lottype) + " lot!");
            plugin.ChargeAndPromote(player, value, player.getWorld().getName());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (event.getLine(0).toLowerCase().contains("[update]")) {
            if (signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getApplicableRegions(BukkitAdapter.asBlockVector(event.getBlock().getLocation())).iterator().hasNext()) {
                ProtectedRegion region = signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getApplicableRegions(BukkitAdapter.asBlockVector(event.getBlock().getLocation())).iterator().next();
                if (!plugin.IGNORE_REGIONS.contains(region.getId())) {
                    AbstractPlayerActor wgPlayer = (AbstractPlayerActor) BukkitAdapter.adapt(player);
                    boolean isOwner = region.isOwner((LocalPlayer) wgPlayer);

                    if (player.hasPermission("SignRank.update") || isOwner) {
                        for (int i = 1 ; i < event.getLines().length ; i++) {
                            String line = event.getLines()[i];
                            if (line.length() != 0) {
                                DefaultDomain owners = region.getOwners();
                                owners.addPlayer(line);
                                region.setOwners(owners);
                                try {
                                    signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).save();
                                } catch (StorageException ex) {
                                    Logger.getLogger(signRankListener.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                player.sendMessage(line + ChatColor.GREEN + " Added");
                            }
                        }
                        return;
                    }
                }
            }
            if (player.hasPermission("SignRank.update")) {
                if(!plugin.lots.setupWorldguardRegion(event.getBlock(), event.getLine(1))) {
                    player.sendMessage(ChatColor.RED + "That fence isn't enclosed! Make sure there are no gaps!");
                    return;
                }
                player.sendMessage(ChatColor.GREEN + "A new lot has been created for " + event.getLine(1));
            }
            return;
        }
        if (event.getLine(0).toLowerCase().contains("[sell]")) {
            if (signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getApplicableRegions(BukkitAdapter.asBlockVector(event.getBlock().getLocation())).iterator().hasNext()) {
                ProtectedRegion region = signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getApplicableRegions(BukkitAdapter.asBlockVector(event.getBlock().getLocation())).iterator().next();
                if (!plugin.IGNORE_REGIONS.contains(region.getId())) {
                    AbstractPlayerActor wgPlayer = (AbstractPlayerActor) BukkitAdapter.adapt(player);
                    boolean isOwner = region.isOwner((LocalPlayer) wgPlayer);
                    if (player.hasPermission("SignRank.sell") || isOwner) {
                        if (!event.getLine(2).isEmpty()) {
                            if (plugin.lots.canBeWorldguardRegion(event.getBlock())) {
                                try {
                                    //Make the sign more yummy for selling
                                    double price = Double.parseDouble(event.getLine(2));
                                    event.setLine(3, player.getName());
                                    event.setLine(1, "");
                                    event.setLine(2, "$" + Double.toString(price));
                                    event.setLine(0, "[lot]");

                                    //Remove the protection around the lot
                                    signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(event.getBlock().getWorld())).removeRegion(region.getId());
                                    try {
                                        signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(event.getBlock().getWorld())).save();
                                    } catch (StorageException ex) {
                                        Logger.getLogger(signRankListener.class.getName()).log(Level.SEVERE, null, ex);
                                    }

                                    //Do the honours to the player
                                    player.sendMessage(ChatColor.GREEN + "This lot is now on the market for " + ChatColor.WHITE + signRank.economy.format(price));
                                } catch (NumberFormatException ex) {
                                    player.sendMessage(ChatColor.RED + "Make sure your sign has a price on the third line.");
                                }
                                return;
                            } else {
                                player.sendMessage(ChatColor.RED + "Please enclose your lot with a fence before selling, and put the sign on top of the fence");
                                return;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Make sure your sign has a price on the third line.");
                            return;
                        }
                    }
                }
            }
            player.sendMessage(ChatColor.RED + "There's no lot here to sell!");
            return;
        }
        if (player.hasPermission("SignRank.create")) {
            return;
        }
        if (event.getLine(0).toLowerCase().contains("[lot]")) {
            player.sendMessage(ChatColor.RED + "You're not allowed to create a lot sign!");
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        // Deny breaking lot signs
        Sign signBlock = null;
        if (event.getBlock().getState() instanceof Sign) {
            signBlock = (Sign) event.getBlock().getState();
        }
        if (event.getBlock().getRelative(BlockFace.UP).getState() instanceof Sign) {
            signBlock = (Sign) event.getBlock().getRelative(BlockFace.UP).getState();
        }

        if (signBlock != null) {
            if (signBlock.getLine(0).toLowerCase().contains("[lot]")) {
                if (!event.getPlayer().hasPermission("SignRank.create")) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to break lot signs.");
                    event.setCancelled(true);
                }
            }
            if (signBlock.getLine(0).toLowerCase().contains("[update]")) {
                Player player = event.getPlayer();
                if (signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getApplicableRegions(BukkitAdapter.asBlockVector(event.getBlock().getLocation())).iterator().hasNext()) {
                    ProtectedRegion region = signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).getApplicableRegions(BukkitAdapter.asBlockVector(event.getBlock().getLocation())).iterator().next();
                    if (!plugin.IGNORE_REGIONS.contains(region.getId())) {
                        AbstractPlayerActor wgPlayer = (AbstractPlayerActor) BukkitAdapter.adapt(player);
                        boolean isOwner = region.isOwner((LocalPlayer) wgPlayer);
                        
                        if (player.hasPermission("SignRank.update") || isOwner) {
                            for (int i = 1 ; i < signBlock.getLines().length ; i++) {
                                String line = signBlock.getLines()[i];
                                if (line.length() != 0) {
                                    DefaultDomain owners = region.getOwners();
                                    owners.removePlayer(line);
                                    region.setOwners(owners);
                                    try {
                                        signRank.worldGuard.getRegionContainer().get(BukkitAdapter.adapt(player.getWorld())).save();
                                    } catch (StorageException ex) {
                                        Logger.getLogger(signRankListener.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    player.sendMessage(line + ChatColor.RED + " Removed");
                                }
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
}
