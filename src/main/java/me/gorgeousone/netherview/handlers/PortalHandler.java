package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.portal.PortalSide;
import me.gorgeousone.netherview.portal.PortalStructure;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class PortalHandler {
	
	private Set<PortalStructure> runningPortals;
	
	public PortalHandler() {
		this.runningPortals = new HashSet<>();
	}
	
	public void addPortal(PortalStructure portal) {
		runningPortals.add(portal);
	}
	
	public PortalStructure getRegisteredPortalByBlock(Block portalBlock) {
		
		if (portalBlock.getType() != Material.NETHER_PORTAL)
			return null;
		
		for (PortalStructure portal : runningPortals) {
			if (portal.containsBlock(portalBlock))
				return portal;
		}
		
		return null;
	}
	
	public boolean isPartOfPortal(Block portalBlock) {
		
		if (portalBlock.getType() != Material.NETHER_PORTAL)
			return false;
		
		for (PortalStructure portal : runningPortals) {
			if (portal.containsBlock(portalBlock))
				return true;
		}
		
		return false;
	}
	
	public PortalStructure nearestPortal(Location playerLoc) {
		
		return null;
	}
	
	public BlockCache getCachedBlocks(PortalStructure portal, PortalSide sideToDisplay) {
		return null;
	}
}
