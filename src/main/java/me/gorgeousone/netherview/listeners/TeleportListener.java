package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.handlers.BlockCacheHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.FacingUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {
	
	private Main main;
	private PortalHandler portalHandler;
	private BlockCacheHandler cacheHandler;
	
	public TeleportListener(Main main,
	                        PortalHandler portalHandler,
	                        BlockCacheHandler cacheHandler) {
		this.main = main;
		this.portalHandler = portalHandler;
		this.cacheHandler = cacheHandler;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPortalTravel(PlayerTeleportEvent event) {
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
			return;
		
		if (!event.getPlayer().hasPermission(Main.LINK_PERM))
			return;
		
		Location to = event.getTo();
		Location from = event.getFrom();
		
		if (!main.canViewOtherWorlds(from.getWorld()) || !main.canBeViewed(to.getWorld()))
			return;
		
		Block portalBlock = getNearbyPortalBlock(from);
		
		if (portalBlock == null)
			return;
		
		Portal portal = portalHandler.getPortalByBlock(portalBlock);
		Player player = event.getPlayer();
		
		if (portal == null) {
			
			try {
				portal = portalHandler.addPortalStructure(portalBlock);
				
			} catch (Exception ex) {
				player.sendMessage(ex.getMessage());
				return;
			}
		}
		
		if (cacheHandler.getCounterPortal(portal) == null) {
			
			Block counterPortalBlock = getNearbyPortalBlock(to);
			Portal counterPortal = portalHandler.getPortalByBlock(counterPortalBlock);
			
			if (counterPortal == null) {
				try {
					counterPortal = portalHandler.addPortalStructure(counterPortalBlock);
					
				} catch (Exception ex) {
					player.sendMessage(ex.getMessage());
					return;
				}
			}
			
			try {
				cacheHandler.linkPortal(portal, counterPortal);
				event.setCancelled(true);
				
			} catch (IllegalStateException ex) {
				player.sendMessage(ex.getMessage());
			}
		}
	}
	
	/**
	 * Finds the portal block a player might have touched at the location or the blocks next to it.
	 * (players in creative mode teleport to the nether before directly touching any portal block)
	 */
	private Block getNearbyPortalBlock(Location location) {
		
		Block block = location.getBlock();
		
		if (block.getType() == Material.NETHER_PORTAL)
			return block;
		
		for (BlockFace face : FacingUtils.getAxesFaces()) {
			Block neighbor = block.getRelative(face);
			
			if (neighbor.getType() == Material.NETHER_PORTAL)
				return neighbor;
		}
		
		return null;
	}
	
}
