package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.portal.PortalSide;
import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import me.gorgeousone.netherview.threedstuff.ViewCone;
import me.gorgeousone.netherview.threedstuff.PortalRectangle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ViewHandler {
	
	private PortalHandler portalHandler;
	private Map<PortalStructure, BlockCache> netherViews;
	
	public ViewHandler(PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
		netherViews = new HashMap<>();
	}
	
	public void displayPortal(Player player, PortalStructure portal) {
		
		if(portal.getWorld().getEnvironment() != World.Environment.NORMAL)
			return;
		
		if(!netherViews.containsKey(portal))
			loadNetherView(portal);
		
		Location playerEyeLoc = player.getEyeLocation();
		PortalRectangle nearPlane = portal.getPortalRect();
		
		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portal.getAxis());
		boolean playerIsRelativelyNegativeToPortal = isPlayerRelativelyNegativeToPortal(playerEyeLoc, portal.getLocation(), portalFacing);
		
		if(playerIsRelativelyNegativeToPortal)
			nearPlane.translate(portalFacing);
		
		ViewCone playerViewCone = new ViewCone(playerEyeLoc.toVector(), nearPlane);
		PortalSide portalSideToDisplay = playerIsRelativelyNegativeToPortal ? PortalSide.POSITIVE : PortalSide.NEGATIVE;
		
		BlockCache netherBlockCache = getCachedBlocks(portal, portalSideToDisplay);
		Set<BlockState> visibleBlocks = detectBlocksInCone(playerViewCone, netherBlockCache);
		renderNetherBLocks(player, portal, netherBlockCache.getBlocks());
	}
	
	private BlockCache getCachedBlocks(PortalStructure portal, PortalSide portalSideToDisplay) {
		return netherViews.get(portal);
	}
	
	private void loadNetherView(PortalStructure overworldPortal) {
		
		PortalStructure netherPortal = portalHandler.getLinkedNetherPortal(overworldPortal);
		
		PortalRectangle portalRect = netherPortal.getPortalRect();
		int viewDist = 6;
		
		Vector facing = AxisUtils.getAxisPlaneNormal(portalRect.getAxis());
		Vector horizontal = facing.getCrossProduct(new Vector(0, 1, 0));
		
		Vector min = portalRect.getMin();
		Vector max = portalRect.getMax();
		
		Vector viewMin = min.clone();
		viewMin.subtract(new Vector(0, viewDist, 0));
		viewMin.add(horizontal.clone().multiply(-viewDist));
		viewMin.add(facing);
		
		Vector viewMax = max.clone();
		viewMax.add(new Vector(0, viewDist, 0));
		viewMax.add(horizontal.clone().multiply(viewDist));
		viewMax.add(facing.clone().multiply(viewDist));
		
//		System.out.println("Loading nether view: facing: " + facing.toString() + " horizone: " + horizontal.toString());
//		System.out.println("Loading nether view: min: " + min.toString() + " max: " + max.toString());
//		System.out.println("Loading nether view: min: " + viewMin.toString() + " max: " + viewMax.toString());
		
		BlockCache netherBlocks = BlockCacheFactory.createBlockCache(
				viewMin.getBlockX(), viewMin.getBlockY(), viewMin.getBlockZ(),
				viewMax.getBlockX(), viewMax.getBlockY(), viewMax.getBlockZ(), netherPortal.getWorld());
		
		netherViews.put(overworldPortal, netherBlocks);
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
		
		return blocksInCone;
	}
	
	private void renderNetherBLocks(Player player, PortalStructure portal, Set<BlockState> visibleBlocks) {
		
		for (Block block : portal.getPortalBlocks())
			player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
		
		for (BlockState block : visibleBlocks) {
			
			Location shiftedWorldLoc = block.getLocation();
			shiftedWorldLoc.setWorld(player.getWorld());
			player.sendBlockChange(shiftedWorldLoc, block.getBlockData());
		}
	}
}
