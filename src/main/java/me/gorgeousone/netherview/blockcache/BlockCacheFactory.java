package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

public class BlockCacheFactory {
	
	Material mat;
	
	public static BlockCache createBlockCache(Portal portal, int viewDist) {
		
		viewDist++;
		AxisAlignedRect portalRect = portal.getPortalRect();
		Axis portalAxis = portal.getAxis();
		
		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portalAxis);
		Vector widthFacing = portalAxis == Axis.X ? new Vector(1, 0, 0) : new Vector(0, 0, 1);
		
		//the view distance in blocks to the front shall be greater than at the sides
		int minPortalExtent = (int) Math.min(portalRect.width(), portalRect.height());
		int frontViewDist =  minPortalExtent + viewDist;
		int sideViewDist = viewDist/2;
		
		Vector cacheCorner1 = portalRect.getMin();
		cacheCorner1.subtract(new Vector(0, sideViewDist + 1, 0));
		cacheCorner1.subtract(widthFacing.clone().multiply(sideViewDist + 1));
		cacheCorner1.add(portalFacing);
		
		Vector cacheCorner2 = portalRect.getMax();
		cacheCorner2.add(new Vector(0, sideViewDist, 0));
		cacheCorner2.add(widthFacing.clone().multiply(sideViewDist));
		cacheCorner2.add(portalFacing.clone().multiply(frontViewDist));
		
		Vector cacheMin = Vector.getMinimum(cacheCorner1, cacheCorner2);
		Vector cacheMax = Vector.getMaximum(cacheCorner1, cacheCorner2);
		
		BlockCopy[][][] copiedBlocks = copyBlocksInBounds(
				cacheMin.getBlockX(), cacheMin.getBlockY(), cacheMin.getBlockZ(),
				cacheMax.getBlockX(), cacheMax.getBlockY(), cacheMax.getBlockZ(),
				portal.getWorld(), portalFacing);
		
		return new BlockCache(new BlockVec(cacheMin), copiedBlocks);
	}
	
	private static BlockCopy[][][] copyBlocksInBounds(int x1, int y1, int z1, int x2, int y2, int z2, World world, Vector cacheFacing) {
		
		if (x2 < x1 || y2 < y1 || z2 < z1)
			throw new IllegalArgumentException("Cannot create a BlockCache of such a small area.");
		
		World.Environment worldType = world.getEnvironment();
		BlockCopy[][][] copiedBlocks = new BlockCopy[x2 - x1 + 1][y2 - y1 + 1][z2 - z1 + 1];
		
		BlockData borderMaterial = worldType == World.Environment.NETHER ?
				Material.RED_CONCRETE.createBlockData() :
				Material.WHITE_CONCRETE.createBlockData();

		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					
					Block block = new Location(world, x, y, z).getBlock();
					boolean isCacheBorder = isCacheBorder(x, y, z, x1, y1, z1, x2, y2, z2, cacheFacing);
					
					if(!isCacheBorder && !isVisible(block))
						continue;
					
					BlockCopy copy = new BlockCopy(block);
					
					if(isCacheBorder)
						copy.setData(borderMaterial);
					
					copiedBlocks[x - x1][y - y1][z - z1] = copy;
				}
			}
		}
		
		return copiedBlocks;
	}
	
	/**
	 * Returns true if the block is part of the border of the cahche cuboid except the side where the portal is
	 */
	private static boolean isCacheBorder(int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2, Vector cacheFacing) {
		
		if(y == y1 || y == y2)
			return true;
		
		if(cacheFacing.getZ() != 0) {
			if (x == x1 || x == x2)
				return true;
		}else if(z == z1 || z == z2) {
			return true;
		}
	
		if(cacheFacing.getX() == 1) {
			return x == x2;
		}else if(cacheFacing.getX() == -1) {
			return x == x1;
		}else if(cacheFacing.getZ() == 1) {
			return z == z2;
		}else{
			return z == z1;
		}
	}
	
	private static boolean isVisible(Block block) {
		
		for(BlockFace face : AxisUtils.getAxesFaces()) {
			if(!block.getRelative(face).getType().isOccluding())
				return true;
		}
		
		return false;
	}
}