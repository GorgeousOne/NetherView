package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.portal.PortalStructureFactory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class PlayerMoveListener implements Listener {
	
	private PortalHandler portalHandler;
	private ViewHandler viewHandler;
	
	public PlayerMoveListener(PortalHandler portalHandler, ViewHandler viewHandler) {
		
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
	}
	
//	@EventHandler
//	public void onPlayerMove(PlayerMoveEvent event) {
//
//		Player player = event.getPlayer();
//		Location playerLoc = player.getEyeLocation();
//
//		PortalStructure portal = portalHandler.nearestPortal(playerLoc);
//
//		if (portal == null)
//			return;
//
//		Vector portalDistance = portal.getLocation().subtract(playerLoc).toVector();
//
//		double viewDistanceSquared = 50 * 50;
//
//		if (portalDistance.lengthSquared() > viewDistanceSquared)
//			return;
//
//		viewHandler.displayPortal(player, portal);
//	}
	
	@EventHandler
	public void onPlayerEnterPortal(EntityPortalEnterEvent event) {
		
		if(event.getEntityType() != EntityType.PLAYER)
			return;
		
		Player player = (Player) event.getEntity();
		Block sourceBlock = event.getLocation().getBlock();
		
		PortalStructure portal = PortalStructureFactory.locatePortalStructure(sourceBlock);
		
		for(Block block : portal.getPortalBlocks()) {
			player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
		}
	}
}
