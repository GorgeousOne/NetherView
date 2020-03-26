package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.portal.PortalStructureFactory;
import me.gorgeousone.netherview.handlers.PortalHandler;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {
	
	private PortalHandler portalHandler;
	
	public TeleportListener(PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
	}
	
	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPortalTravel(PlayerTeleportEvent event) {
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
			return;
		
		Location from = event.getFrom();
		Location to = event.getTo();
		
		if (from.getWorld().getEnvironment() != World.Environment.NORMAL &&
		    to.getWorld().getEnvironment() != World.Environment.NETHER)
			return;
		
		event.getPlayer().sendMessage(ChatColor.GRAY + "Trying to connect portals...");
		event.getPlayer().sendMessage(ChatColor.GRAY + "Check: " + !portalHandler.containsPortalWithBlock(from.getBlock()));
		
		if (!portalHandler.containsPortalWithBlock(from.getBlock())) {
			event.getPlayer().sendMessage(ChatColor.GRAY + "Locating fresh portals...");
			addPortals(from, to);
			event.setCancelled(true);
		}
	}
	
	private void addPortals(Location from, Location to) {
		
		try {
			PortalStructure overworldPortal = PortalStructureFactory.locatePortalStructure(from.getBlock());
			PortalStructure netherPortal = PortalStructureFactory.locatePortalStructure(to.getBlock());
			portalHandler.addPortals(overworldPortal, netherPortal);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
