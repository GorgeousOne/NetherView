package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.AxisUtils;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class BlockCacheFactory {
	
	public static BlockCache getTransformed(BlockCache cache, Transform transform) {
		
		BlockVec min = cache.getMin();
		BlockVec max = cache.getMax();
		
		BlockVec corner1 = transform.getTransformedVec(min);
		BlockVec corner2 = transform.getTransformedVec(max);
		
		BlockVec newMin = BlockVec.getMinimum(corner1, corner2);
		BlockVec newMax = BlockVec.getMaximum(corner1, corner2);
		
		int x1 = newMin.getX();
		int y1 = newMin.getY();
		int z1 = newMin.getZ();
		int x2 = newMax.getX();
		int y2 = newMax.getY();
		int z2 = newMax.getZ();
		
		BlockCopy[][][] newBlockCopies = new BlockCopy[x2 - x1 + 1][y2 - y1 + 1][z2 - z1 + 1];
		
		for(int x = min.getX(); x < max.getX(); x++) {
			for (int y = min.getY(); y < max.getY(); y++) {
				for (int z = min.getZ(); z < max.getZ(); z++) {
					
					BlockVec blockPos = new BlockVec(x, y, z);
					BlockCopy blockCopy = cache.getCopyAt(blockPos);
					
					if(blockCopy == null)
						continue;
					
					BlockVec newBlockPos = transform.getTransformedVec(blockPos);
					newBlockCopies[newBlockPos.getX() - x1]
							[newBlockPos.getY() - y1]
							[newBlockPos.getZ() - z1] = blockCopy.clone().setPosition(newBlockPos);
				}
			}
		}
		
		return new BlockCache(newMin, newBlockCopies);
	}
	
	public static Map.Entry<BlockCache, BlockCache> createBlockCache(Portal portal, int viewDist) {
		
		//theoretically the view distance needs to be increased by 1 for the extra layer of border around the cuboid of blocks
		//but somehow it is even 2. dont ask me
		viewDist += 2;
		AxisAlignedRect portalRect = portal.getPortalRect();
		Axis portalAxis = portal.getAxis();
		
		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portalAxis);
		Vector widthFacing = portalAxis == Axis.X ? new Vector(1, 0, 0) : new Vector(0, 0, 1);
		
		//the view distance in blocks to the front shall be greater than at the sides
		int minPortalExtent = (int) Math.min(portalRect.width(), portalRect.height());
		int frontViewDist =  minPortalExtent + viewDist;
		int sideViewDist = (int) Math.ceil(viewDist / 2d);
		
		Vector cacheCorner1 = portalRect.getMin();
		cacheCorner1.subtract(new Vector(0, sideViewDist, 0));
		cacheCorner1.subtract(widthFacing.clone().multiply(sideViewDist));
		
		Vector cacheCorner2 = portalRect.getMax();
		cacheCorner2.add(new Vector(0, sideViewDist, 0));
		cacheCorner2.add(widthFacing.clone().multiply(sideViewDist));
		
		BlockCache front = copyBlocksInBounds(
				cacheCorner1.clone().add(portalFacing),
				cacheCorner2.clone().add(portalFacing.clone().multiply(frontViewDist)),
				portal.getWorld(),
				portalFacing);
		
		BlockCache back = copyBlocksInBounds(
				cacheCorner1.clone().subtract(portalFacing.clone().multiply(frontViewDist-1)),
				cacheCorner2,
				portal.getWorld(),
				portalFacing.clone().multiply(-1));
		
		return new AbstractMap.SimpleEntry<>(front, back);
	}
	
//	public static BlockCache createBlockCache2(Portal portal, int viewDist) {
//
//		//theoretically the view distance needs to be increased by 1 for the extra layer of border around the cuboid of blocks
//		//but somehow it is even 2. dont ask me
//		viewDist += 2;
//		AxisAlignedRect portalRect = portal.getPortalRect();
//		Axis portalAxis = portal.getAxis();
//
//		Vector portalFacing = AxisUtils.getAxisPlaneNormal(portalAxis);
//		Vector widthFacing = portalAxis == Axis.X ? new Vector(1, 0, 0) : new Vector(0, 0, 1);
//
//		//the view distance in blocks to the front shall be greater than at the sides
//		int minPortalExtent = (int) Math.min(portalRect.width(), portalRect.height());
//		int frontViewDist =  minPortalExtent + viewDist;
//		int sideViewDist = (int) Math.ceil(viewDist / 2d);
//
//		Vector cacheCorner1 = portalRect.getMin();
//		cacheCorner1.subtract(new Vector(0, sideViewDist, 0));
//		cacheCorner1.subtract(widthFacing.clone().multiply(sideViewDist));
//		cacheCorner1.subtract(portalFacing.clone().multiply(frontViewDist - 1));
//
//		Vector cacheCorner2 = portalRect.getMax();
//		cacheCorner2.add(new Vector(0, sideViewDist, 0));
//		cacheCorner2.add(widthFacing.clone().multiply(sideViewDist));
//
//		return copyBlocksInBounds(cacheCorner1, cacheCorner2, portal.getWorld(), portalFacing.clone().multiply(-1));
//	}
	
	private static BlockCache copyBlocksInBounds(Vector cacheCorner1, Vector cacheCorner2, World world, Vector cacheFacing) {
		
		Vector cacheMin = Vector.getMinimum(cacheCorner1, cacheCorner2);
		Vector cacheMax = Vector.getMaximum(cacheCorner1, cacheCorner2);
		
		int x1 = cacheMin.getBlockX();
		int y1 = cacheMin.getBlockY();
		int z1 = cacheMin.getBlockZ();
		int x2 = cacheMax.getBlockX();
		int y2 = cacheMax.getBlockY();
		int z2 = cacheMax.getBlockZ();
		
		if (x2 < x1 || y2 < y1 || z2 < z1)
			throw new IllegalArgumentException("Cannot create a BlockCache smaller than 1 block.");
		
		World.Environment worldType = world.getEnvironment();
		BlockCopy[][][] copiedBlocks = new BlockCopy[x2 - x1][y2 - y1][z2 - z1];
		
		BlockData borderMaterial = worldType == World.Environment.NETHER ?
				Material.RED_CONCRETE.createBlockData() :
				Material.WHITE_CONCRETE.createBlockData();

		for (int x = x1; x < x2; x++) {
			for (int y = y1; y < y2; y++) {
				for (int z = z1; z < z2; z++) {
					
					Block block = new Location(world, x, y, z).getBlock();
					
					if(!isVisible(block))
						continue;
					
					BlockCopy copy = new BlockCopy(block);
					
					if(isCacheBorder(x, y, z, x1, y1, z1, x2, y2, z2, cacheFacing))
						copy.setData(borderMaterial);
					
					copiedBlocks[x - x1][y - y1][z - z1] = copy;
				}
			}
		}
		
		return new BlockCache(new BlockVec(cacheMin), copiedBlocks);
	}
	
	/**
	 * Returns true if the block is part of the border of the cahche cuboid except the side where the portal is
	 */
	private static boolean isCacheBorder(int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2, Vector cacheFacing) {
		
		if(y == y1 || y == y2-1)
			return true;
		
		if(cacheFacing.getZ() != 0) {
			if (x == x1 || x == x2-1)
				return true;
		}else if(z == z1 || z == z2-1) {
			return true;
		}
	
		if(cacheFacing.getX() == 1) {
			return x == x2-1;
		}else if(cacheFacing.getX() == -1) {
			return x == x1;
		}else if(cacheFacing.getZ() == 1) {
			return z == z2-1;
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