package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.portal.PortalSide;
import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import me.gorgeousone.netherview.threedstuff.ViewCone;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
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
		AxisAlignedRect nearPlane = portal.getPortalRect();
		
		Vector portalFacing = AxisUtils.getAsVector(portal.getAxis());
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
					
					Vector blockCorner = new Vector(x, y, z);
					
					if (viewCone.contains(blockCorner))
						blocksInCone.addAll(netherCache.getBlocksAtCorner(blockCorner));
				}
			}
		}
		
		return null;
	}
	
	private void renderNetherBLocks(Player player, Set<BlockState> blocks) {
		for (BlockState block : blocks)
			player.sendBlockChange(block.getLocation(), block.getBlockData());
	}
}
