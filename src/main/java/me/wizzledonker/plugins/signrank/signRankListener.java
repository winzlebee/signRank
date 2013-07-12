/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.wizzledonker.plugins.signrank;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
            if (!block.getType().equals(Material.SIGN_POST)) {
                return;
            }
            if (!((Sign) block.getState()).getLine(0).toLowerCase().contains("[lot]")) {
                return;
            }
            Sign signblock = ((Sign) block.getState());
            String lottype = signblock.getLine(1);
            if (signRank.economy.getBalance(player.getName()) < plugin.determineValue(lottype)) {
                player.sendMessage(ChatColor.RED + "You do not have enough money to purchase that lot!");
                return;
            }
            plugin.ChargeAndPromote(player, plugin.determineValue(lottype), player.getWorld().getName());
            plugin.lots.setupWorldguardRegion(block.getRelative(BlockFace.DOWN), player.getName());
            signblock.setLine(0, player.getName() + "'s");
            signblock.setLine(1, lottype + " lot");
            signblock.setLine(2, "");
            signblock.setLine(3, "");
            signblock.update(true);
            plugin.getServer().broadcastMessage(ChatColor.GREEN + player.getName() + " has bought a " + lottype + " lot!");
        }
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (event.getLine(0).toLowerCase().contains("[update]")) {
            if (!signRank.worldGuard.getRegionManager(player.getWorld()).getApplicableRegions(event.getBlock().getLocation()).iterator().hasNext()) {
                ProtectedRegion region = signRank.worldGuard.getRegionManager(player.getWorld()).getApplicableRegions(event.getBlock().getLocation()).iterator().next();
                if (!plugin.IGNORE_REGIONS.contains(region.getId())) {
                    boolean isOwner = region.isOwner(player.getName());

                    if (player.hasPermission("SignRank.update") || isOwner) {
                        for (String line : event.getLines()) {
                            if (line != "") {
                                DefaultDomain owners = region.getOwners();
                                owners.addPlayer(line);
                                region.setOwners(owners);
                                player.sendMessage(line + ChatColor.GREEN + " Added");
                            }
                        }
                        return;
                    }
                }
            }
            if (player.hasPermission("SignRank.update")) {
                plugin.lots.setupWorldguardRegion(event.getBlock().getRelative(BlockFace.DOWN), event.getLine(1));
                player.sendMessage(ChatColor.GREEN + "A new lot has been created for " + event.getLine(1));
            }
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
        Sign signBlock = null;
        if (event.getBlock().getType().equals(Material.SIGN_POST)) {
            signBlock = (Sign) event.getBlock().getState();
        }
        if (event.getBlock().getRelative(BlockFace.UP).getType().equals(Material.SIGN_POST)) {
            signBlock = (Sign) event.getBlock().getRelative(BlockFace.UP).getState();
        }
        if (signBlock != null) {
            if (signBlock.getLine(0).toLowerCase().contains("[lot]")) {
                if (!event.getPlayer().hasPermission("SignRank.create")) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to break lot signs.");
                    event.setCancelled(true);
                }
            }
        }
    }
}
