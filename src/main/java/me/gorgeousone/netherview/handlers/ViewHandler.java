package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.portal.PortalSide;
import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import me.gorgeousone.netherview.threedstuff.ViewCone;
import me.gorgeousone.netherview.threedstuff.PortalRectangle;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class ViewHandler {
	
	private PortalHandler portalHandler;
	
	public ViewHandler(PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
	}
	
	public void displayPortal(Player player, PortalStructure portal) {
		
		Location playerEyeLoc = player.getEyeLocation();
		PortalRectangle nearPlane = portal.getPortalRect();
		
		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portal.getAxis());
		boolean playerIsRelativelyNegativeToPortal = isPlayerRelativelyNegativeToPortal(playerEyeLoc, portal.getLocation(), portalFacing);
		
		if(playerIsRelativelyNegativeToPortal)
			nearPlane.translate(portalFacing);
		
		ViewCone playerViewCone = new ViewCone(playerEyeLoc.toVector(), nearPlane);
		PortalSide portalSideToDisplay = playerIsRelativelyNegativeToPortal ? PortalSide.POSITIVE : PortalSide.NEGATIVE;
		
		BlockCache netherBlockCache = portalHandler.getCachedBlocks(portal, portalSideToDisplay);
		Set<BlockState> visibleBlocks = detectBlocksInCone(playerViewCone, netherBlockCache);
		renderNetherBLocks(player, visibleBlocks);
	}
	
	private boolean isPlayerRelativelyNegativeToPortal(Location playerLoc, Location portalLoc, Vector portalFacing) {
		Vector portalDist = portalLoc.toVector().subtract(playerLoc.toVector());
		return portalFacing.dot(portalDist) > 0;
	}
	
	private Set<BlockState> detectBlocksInCone(ViewCone viewCone, BlockCache netherCache) {
		
		Set<BlockState> blocksInCone = new HashSet<>();
		Vector min = netherCache.getMin();
		Vector max = netherCache.getMax();
		
		for (int x = min.getBlockX() + 1; x < max.getX(); x++) {
			for (int y = min.getBlockY() + 1; y < max.getY(); y++) {
				for (int z = min.getBlockZ() + 1; z < max.getZ(); z++) {
					
					if (viewCone.contains(new Vector(x, y, z)))
						blocksInCone.addAll(netherCache.getBlocksAtCorner(x, y, z));
				}
			}
		}
		
		return null;
	}
	
	private void renderNetherBLocks(Player player, Set<BlockState> blocks) {
		
		for (BlockState block : blocks) {
			
			Location shiftedWorldLoc = block.getLocation();
			shiftedWorldLoc.setWorld(player.getWorld());
			
			player.sendBlockChange(shiftedWorldLoc, block.getBlockData());
		}
	}
}
