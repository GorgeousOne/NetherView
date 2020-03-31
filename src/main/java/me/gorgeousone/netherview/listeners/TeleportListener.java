package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.portal.PortalStructureFactory;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
		
		if (from.getWorld().getEnvironment() != World.Environment.NORMAL &&
		    to.getWorld().getEnvironment() != World.Environment.NETHER)
			return;
		
		Block sourceBlock = findPortalBlock(from);
		
		if (sourceBlock != null && !portalHandler.containsPortalWithBlock(sourceBlock)) {
			
			event.getPlayer().sendMessage(ChatColor.GRAY + "Locating fresh portals...");
			addPortals(sourceBlock, to.getBlock());
			event.setCancelled(true);
		}
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
	
	private void addPortals(Block from, Block to) {
		
		try {
			PortalStructure overworldPortal = PortalStructureFactory.locatePortalStructure(from);
			PortalStructure netherPortal = PortalStructureFactory.locatePortalStructure(to);
			portalHandler.linkPortals(overworldPortal, netherPortal);
			
		} catch (Exception ignored) {
		}
	}
}
