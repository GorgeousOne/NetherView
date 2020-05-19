package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.FacingUtils;
import me.gorgeousone.netherview.portal.PortalLocator;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL ||
		    !event.getPlayer().hasPermission(NetherView.LINK_PERM))
			return;
		
		Location to = event.getTo();
		Location from = event.getFrom();
		
		if (!main.canViewOtherWorlds(from.getWorld()) || !main.canBeViewed(to.getWorld()))
			return;
		
		Block portalBlock = PortalLocator.getNearbyPortalBlock(to);
		
		//might happen if the player mysteriously moved more than a block away from the portal in split seconds
		if (portalBlock == null)
			return;
		
		Player player = event.getPlayer();
		Portal portal = portalHandler.getPortalByBlock(portalBlock);
		
		try {
		
			if(portal == null)
				portal = portalHandler.addPortalStructure(portalBlock);
			
			if (portal.isLinked())
				return;
				
			Block counterPortalBlock = PortalLocator.getNearbyPortalBlock(to);
			
			if(counterPortalBlock == null)
				return;
			
			Portal counterPortal = portalHandler.getPortalByBlock(counterPortalBlock);
			
			if (counterPortal == null)
				counterPortal = portalHandler.addPortalStructure(counterPortalBlock);
			
			portalHandler.linkPortalTo(portal, counterPortal);
			player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The veil between the two worlds has lifted a little bit!");
			
			if (player.getGameMode() == GameMode.CREATIVE || main.cancelTeleportWhenLinking())
				event.setCancelled(true);
			
		}catch (IllegalArgumentException | IllegalStateException ex) {
			player.sendMessage(ex.getMessage());
		}
	}
}
