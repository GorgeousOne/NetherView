package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {
	
	private PortalHandler portalHandler;
	
	public TeleportListener(PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPortalTravel(PlayerTeleportEvent event) {
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
			return;
		
		Location from = event.getFrom();
		Location to = event.getTo();
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL ||
		    !isTpBetweenNetherAndNormal(from, to))
			return;
		
		Block sourceBlock = findPortalBlock(from);
		
		if (sourceBlock != null && !portalHandler.containsPortalWithBlock(sourceBlock)) {
			
			event.getPlayer().sendMessage(ChatColor.GRAY + "Locating fresh portals...");
			addPortals(sourceBlock, to.getBlock(), event.getPlayer());
			event.setCancelled(true);
		}
	}
	
	private boolean isTpBetweenNetherAndNormal(Location from, Location to) {
		
		World.Environment fromWorldType = from.getWorld().getEnvironment();
		World.Environment toWorldType = to.getWorld().getEnvironment();
		
		return fromWorldType == World.Environment.NORMAL && toWorldType == World.Environment.NETHER; // ||
//		       fromWorldType == World.Environment.NETHER && toWorldType == World.Environment.NORMAL;
	}
	
	private Block findPortalBlock(Location location) {
		
		Block sourceBlock = location.getBlock();
		
		if(sourceBlock.getType() == Material.NETHER_PORTAL)
			return sourceBlock;
		
		for(BlockFace face : AxisUtils.getAxesFaces()) {
			Block neighbor = sourceBlock.getRelative(face);
			
			if(neighbor.getType() == Material.NETHER_PORTAL)
				return neighbor;
		}
		
		return null;
	}
	
	private void addPortals(Block from, Block to, Player player) {
		
		try {
			Portal overworldPortal = PortalLocator.locatePortalStructure(from);
			Portal netherPortal = PortalLocator.locatePortalStructure(to);
			
			for(Block block : overworldPortal.getPortalBlocks()) {
				player.sendMessage("DEBUG portal blocks displayed as air");
				player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
			}
			
			portalHandler.linkPortals(overworldPortal, netherPortal);
			
		} catch (Exception ignored) {}
	}
}
