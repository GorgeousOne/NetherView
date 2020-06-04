package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.UUID;

public class TeleportListener implements Listener {
	
	private NetherView main;
	private PortalHandler portalHandler;
	
	private HashMap<UUID, Location> portalTravellingEntities;
	
	public TeleportListener(NetherView main, PortalHandler portalHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		
		portalTravellingEntities = new HashMap<>();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDisappear(EntityPortalEvent event) {
		Entity entity = event.getEntity();
		portalTravellingEntities.put(entity.getUniqueId(), entity.getLocation());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityReappear(EntitySpawnEvent event) {
		
		Entity entity = event.getEntity();
		UUID entityID = entity.getUniqueId();
		
		if (portalTravellingEntities.containsKey(entityID)) {
			createPortalView(portalTravellingEntities.get(entityID), entity.getLocation(), entity);
			portalTravellingEntities.remove(entityID);
		}
	}
	
	//I did not use the PlayerPortalEvent because it only give information about where the player should theoretically perfectly teleport to
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPortalTravel(PlayerTeleportEvent event) {
		
		Player player = event.getPlayer();
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL ||
		    !player.hasPermission(NetherView.LINK_PERM)) {
			return;
		}
		
		boolean successfullyCreatedPortalView = createPortalView(event.getFrom(), event.getTo(), player);
		
		if (successfullyCreatedPortalView && (player.getGameMode() == GameMode.CREATIVE || main.cancelTeleportWhenLinking())) {
			event.setCancelled(true);
		}
	}
	
	/**
	 * Locates and links two portals at the given locations.
	 *
	 * @param traveller entity that will receive confirmation messages.
	 * @return true if a new link between two portals has been created.
	 */
	private boolean createPortalView(Location from, Location to, Entity traveller) {
		
		if (from == null || to == null) {
			return false;
		}
		
		if (!main.canCreatePortalViews(from.getWorld())) {
			
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[Debug] World '" + from.getWorld().getName() + "' not listed in config for portal viewing");
			}
			return false;
		}
		
		Block portalBlock = PortalLocator.getNearbyPortalBlock(from);
		
		//might happen if the player mysteriously moved more than a block away from the portal in split seconds
		if (portalBlock == null) {
			
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] No portal found at starting point " + new BlockVec(from).toString());
			}
			return false;
		}
		
		Block counterPortalBlock = PortalLocator.getNearbyPortalBlock(to);
		
		if (counterPortalBlock == null) {
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] No portal found at destination point " + new BlockVec(to).toString());
			}
			return false;
		}
		
		try {
			
			Portal portal = portalHandler.getPortalByBlock(portalBlock);
			Portal counterPortal = portalHandler.getPortalByBlock(counterPortalBlock);
			
			if (portal.getCounterPortal() == counterPortal) {
				return false;
			}
			
			if (portal.isLinked()) {
				portal.removeLink();
				portalHandler.linkPortalTo(portal, counterPortal);
				return false;
			}
			
			portalHandler.linkPortalTo(portal, counterPortal);
			traveller.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The veil between the two worlds has lifted a little bit!");
			return true;
			
		} catch (IllegalArgumentException | IllegalStateException e) {
			
			traveller.sendMessage(e.getMessage());
			return false;
		}
	}
}