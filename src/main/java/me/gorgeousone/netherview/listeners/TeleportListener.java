package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
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
		
		Block portalBlock = getNearbyPortalBlock(from);
		
		if (portalBlock == null)
			return;
		
		Portal portal = portalHandler.getPortalByBlock(portalBlock);
		
		if(portal == null) {
			Player player = event.getPlayer();
			player.sendMessage(ChatColor.GRAY + "Found a portal");
			
			portal = portalHandler.addPortal(portalBlock);
			event.setCancelled(true);
		}
		
		if (portalHandler.getPortalLink(portal) == null) {
		
			Portal counterPortal = portalHandler.getPortalByBlock(to.getBlock());
			
			if(counterPortal == null)
				counterPortal = portalHandler.addPortal(to.getBlock());
			
			portalHandler.linkPortal(portal, counterPortal);
			event.setCancelled(true);
		}
	}
	
	private boolean isTpBetweenNetherAndNormal(Location from, Location to) {
		
		World.Environment fromWorldType = from.getWorld().getEnvironment();
		World.Environment toWorldType = to.getWorld().getEnvironment();
		
		return fromWorldType == World.Environment.NORMAL && toWorldType == World.Environment.NETHER ||
		       fromWorldType == World.Environment.NETHER && toWorldType == World.Environment.NORMAL;
	}
	
	/**
	 * 	Finds the portal block a player might have touched at the location or the blocks next to it.
	 * 	(players in creative mode teleport to the nether before directly touching any portal block)
	 */
	private Block getNearbyPortalBlock(Location location) {
		
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
	
}
