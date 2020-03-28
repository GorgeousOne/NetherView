package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockCopy;
import me.gorgeousone.netherview.portal.PortalSide;
import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import me.gorgeousone.netherview.threedstuff.Rectangle;
import me.gorgeousone.netherview.threedstuff.ViewCone;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
		
		if (portal.getWorld().getEnvironment() != World.Environment.NORMAL)
			return;
		
		if (!netherViews.containsKey(portal))
			netherViews.put(portal, BlockCacheFactory.createBlockCache(portal, portalHandler.getLinkedNetherPortal(portal), 8));
		
		Location playerEyeLoc = player.getEyeLocation();
		Rectangle nearPlane = portal.getPortalRect();
		
		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portal.getAxis());
		boolean playerIsRelativelyNegativeToPortal = isPlayerRelativelyNegativeToPortal(playerEyeLoc, portal.getLocation(), portalFacing);
		
		if (playerIsRelativelyNegativeToPortal)
			nearPlane.translate(portalFacing);
		
		ViewCone playerViewCone = new ViewCone(playerEyeLoc.toVector(), nearPlane);
		PortalSide portalSideToDisplay = playerIsRelativelyNegativeToPortal ? PortalSide.POSITIVE : PortalSide.NEGATIVE;
		
		BlockCache netherBlockCache = getCachedBlocks(portal, portalSideToDisplay);
		Set<BlockCopy> visibleBlocks = detectBlocksInView(playerViewCone, netherBlockCache);
		renderNetherBLocks(player, portal, visibleBlocks);
		
		System.out.println("point " + playerEyeLoc.toVector().toString());
		System.out.println("plane " + nearPlane.getMin().toString() + " - " + nearPlane.getMax().toString());
		System.out.println("axis " + nearPlane.getAxis().name() + " wdith " + nearPlane.width() + " height " + nearPlane.height());
	}
	
	private BlockCache getCachedBlocks(PortalStructure portal, PortalSide portalSideToDisplay) {
		return netherViews.get(portal);
	}
	
	private boolean isPlayerRelativelyNegativeToPortal(Location playerLoc, Location portalLoc, Vector portalFacing) {
		Vector portalDist = portalLoc.toVector().subtract(playerLoc.toVector());
		return portalFacing.dot(portalDist) > 0;
	}
	
	private Set<BlockCopy> detectBlocksInView(ViewCone viewCone, BlockCache netherCache) {
		
		Set<BlockCopy> blocksInCone = new HashSet<>();
		Vector min = netherCache.getCopyMin();
		Vector max = netherCache.getCopyMax();
		
		for (int x = min.getBlockX(); x < max.getX(); x++) {
			for (int y = min.getBlockY(); y < max.getY(); y++) {
				for (int z = min.getBlockZ(); z < max.getZ(); z++) {

					if (viewCone.contains(new Vector(x, y, z)))
						blocksInCone.addAll(netherCache.getCopiesAround(x, y, z));
				}
			}
		}
		
		return blocksInCone;
	}
	
	private void renderNetherBLocks(Player player, PortalStructure portal, Set<BlockCopy> visibleBlocks) {
		
		player.sendMessage("Enjoy watching " + visibleBlocks.size() + " blocks.");
		
		for (Block block : portal.getPortalBlocks())
			player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
		
		for (BlockCopy copy : visibleBlocks)
			player.sendBlockChange(copy.getPosition().toLocation(player.getWorld()), copy.getBlockData());
	}
}
