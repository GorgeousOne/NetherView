package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import me.gorgeousone.netherview.threedstuff.Rectangle;
import me.gorgeousone.netherview.threedstuff.Transform;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class BlockCacheFactory {
	
	public static BlockCache createBlockCache(PortalStructure overworldPortal, PortalStructure netherPortal) {
		
		Rectangle overworldRect = overworldPortal.getPortalRect();
		Rectangle viewRect = netherPortal.getPortalRect();
		viewRect.setSize(overworldRect.width(), overworldRect.height());
		
		int viewDist = 6;
		
		Vector facing = AxisUtils.getAxisPlaneNormal(viewRect.getAxis());
		Vector horizontal = facing.getCrossProduct(new Vector(0, 1, 0));
		
		Vector min = viewRect.getMin();
		Vector max = viewRect.getMax();
		
		Vector cacheMin = min.clone();
		cacheMin.subtract(new Vector(0, viewDist, 0));
		cacheMin.add(horizontal.clone().multiply(-viewDist));
		cacheMin.add(facing);
		
		Vector cacheMax = max.clone();
		cacheMax.add(new Vector(0, viewDist, 0));
		cacheMax.add(horizontal.clone().multiply(viewDist));
		cacheMax.add(facing.clone().multiply(viewDist));
		
		Block[][][] netherBlocks = copyBlocksInBounds(
				cacheMin.getBlockX(), cacheMin.getBlockY(), cacheMin.getBlockZ(),
				cacheMax.getBlockX(), cacheMax.getBlockY(), cacheMax.getBlockZ(), netherPortal.getWorld());
		
		return new BlockCache(netherBlocks, cacheMin, createPortalLinkTransform(overworldPortal, netherPortal));
	}
	
	private static Block[][][] copyBlocksInBounds(int x1, int y1, int z1, int x2, int y2, int z2, World world) {
		
		if (x2 <= x1 || y2 <= y1 || z2 <= z1)
			throw new IllegalArgumentException("Cannot create a BlockCache of such a small area");
		
		Block[][][] blocks = new Block[x2 - x1 + 1][y2 - y1 + 1][z2 - z1 + 1];
		
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					
					blocks[x - x1][y - y1][z - z1] = new Location(world, x, y, z).getBlock();
				}
			}
		}
		
		return blocks;
	}
	
	private static Transform createPortalLinkTransform(PortalStructure overworldPortal, PortalStructure netherPortal) {
		
		Transform transform = new Transform();
		transform.setTranslation(overworldPortal.getPortalRect().getMin().subtract(netherPortal.getPortalRect().getMin()));
		
		if (netherPortal.getAxis() != overworldPortal.getAxis()) {
			transform.setRotCenter(netherPortal.getPortalRect().getMin());
			
			if (netherPortal.getAxis() == Axis.X)
				transform.setRotY90DegLeft();
			else
				transform.setRotY90DegRight();
		}
		
		return transform;
	}
}
