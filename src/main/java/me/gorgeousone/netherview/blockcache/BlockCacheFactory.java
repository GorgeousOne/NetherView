package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class BlockCacheFactory {
	
	public static BlockCache createBlockCache(Portal portal, int viewDist) {
		
		AxisAlignedRect portalRect = portal.getPortalRect();
		
		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portal.getAxis());
		Vector widthFacing = portalFacing.getCrossProduct(new Vector(0, 1, 0));
		
		Vector cacheCorner1 = portalRect.getMin();
		cacheCorner1.subtract(new Vector(0, viewDist - 1, 0));
		cacheCorner1.subtract(widthFacing.clone().multiply(viewDist - 1));
		cacheCorner1.add(portalFacing);
		
		Vector cacheCorner2 = portalRect.getMax();
		cacheCorner2.add(new Vector(0, viewDist - 1, 0));
		cacheCorner2.add(widthFacing.clone().multiply(viewDist - 1));
		cacheCorner2.add(portalFacing.clone().multiply(viewDist));
		
		Vector cacheMin = Vector.getMinimum(cacheCorner1, cacheCorner2);
		Vector cacheMax = Vector.getMaximum(cacheCorner1, cacheCorner2);
		
		Block[][][] netherBlocks = copyBlocksInBounds(
				cacheMin.getBlockX(), cacheMin.getBlockY(), cacheMin.getBlockZ(),
				cacheMax.getBlockX(), cacheMax.getBlockY(), cacheMax.getBlockZ(), portal.getWorld());
		
		return new BlockCache(netherBlocks, new BlockVec(cacheMin));
	}
	
	private static Block[][][] copyBlocksInBounds(int x1, int y1, int z1, int x2, int y2, int z2, World world) {
		
		if (x2 < x1 || y2 < y1 || z2 < z1)
			throw new IllegalArgumentException("Cannot create a BlockCache of such a small area");
		
		Block[][][] blocks = new Block[x2 - x1 + 1][y2 - y1 + 1][z2 - z1 + 1];
		
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					
					Block block = new Location(world, x, y, z).getBlock();
					
					if(isVisible(block))
						blocks[x - x1][y - y1][z - z1] = block;
				}
			}
		}
		
		return blocks;
	}
	
//	private static Transform createPortalLinkTransform(PortalStructure overworldPortal, PortalStructure netherPortal) {
//
//		Transform transform = new Transform();
//		transform.setTranslation(new BlockVec(overworldPortal.getPortalRect().getMin().subtract(netherPortal.getPortalRect().getMin())));
//
//		if (netherPortal.getAxis() != overworldPortal.getAxis()) {
//			transform.setRotCenter(new BlockVec(netherPortal.getPortalRect().getMin()));
//
//			if (netherPortal.getAxis() == Axis.X)
//				transform.setRotY90DegLeft();
//			else
//				transform.setRotY90DegRight();
//		}
//
//		return transform;
//	}
	
	private static boolean isVisible(Block block) {
		
		for(BlockFace face : AxisUtils.getAxesFaces()) {
			if(!block.getRelative(face).getType().isOccluding())
				return true;
		}
		
		return false;
	}
}