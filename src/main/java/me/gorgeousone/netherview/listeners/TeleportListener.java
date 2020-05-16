package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
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
	
	private NetherView main;
	private PortalHandler portalHandler;
	
	public TeleportListener(NetherView main,
	                        PortalHandler portalHandler) {
		this.main = main;
		this.portalHandler = portalHandler;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPortalTravel(PlayerTeleportEvent event) {
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
			return;
		
		if (!event.getPlayer().hasPermission(NetherView.LINK_PERM))
			return;
		
		Location to = event.getTo();
		Location from = event.getFrom();
		
		if (!main.canViewOtherWorlds(from.getWorld()) || !main.canBeViewed(to.getWorld()))
			return;
		
		Block portalBlock = getNearbyPortalBlock(from);
		
		if (portalBlock == null)
			return;
		
		Player player = event.getPlayer();
		Portal portal = portalHandler.getPortalByBlock(portalBlock);
		
		if (portal == null) {
			
			try {
				portal = portalHandler.addPortalStructure(portalBlock);
				
			} catch (Exception ex) {
				player.sendMessage(ex.getMessage());
				return;
			}
		}
		
		
		if (!portal.isLinked()) {
			
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
				portalHandler.linkPortalTo(portal, counterPortal);
				event.setCancelled(true);
				
			} catch (IllegalStateException ex) {
				player.sendMessage(ex.getMessage());
			}
		}
	}
	
	/**
	 * Finds the portal block a player might have touched at the location or the blocks next to it
	 * (players in creative mode often teleport to the nether before their location appears to be inside a portal).
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
