package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.threedstuff.FacingUtils;
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
	
	public static Map.Entry<BlockCache, BlockCache> createBlockCaches(Portal portal, int viewDist) {
		
		//theoretically the view distance needs to be increased by 1 for the extra layer of border around the cuboid of blocks.
		//but somehow it's 2. Don't ask me.
		viewDist += 2;
		
		AxisAlignedRect portalRect = portal.getPortalRect();
		Axis portalAxis = portal.getAxis();
		
		Vector portalFacing = FacingUtils.getAxisPlaneNormal(portalAxis);
		Vector widthFacing = FacingUtils.getAxisWidthFacing(portalAxis);
		
		//the view distance in blocks to the front shall be greater than at the sides
		int minPortalExtent = (int) Math.min(portalRect.width(), portalRect.height());
		int frontViewDist = minPortalExtent + viewDist;
		
		int sideViewDist = (int) Math.ceil(viewDist / 1.618d);
		
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
				cacheCorner1.clone().subtract(portalFacing.clone().multiply(frontViewDist - 1)),
				cacheCorner2,
				portal.getWorld(),
				portalFacing.clone().multiply(-1));
		
		return new AbstractMap.SimpleEntry<>(front, back);
	}
	
	private static BlockCache copyBlocksInBounds(Vector cacheCorner1,
	                                             Vector cacheCorner2,
	                                             World cacheWorld,
	                                             Vector cacheFacing) {
		
		Vector cacheMin = Vector.getMinimum(cacheCorner1, cacheCorner2);
		Vector cacheMax = Vector.getMaximum(cacheCorner1, cacheCorner2);
		
		int minX = cacheMin.getBlockX();
		int minY = cacheMin.getBlockY();
		int minZ = cacheMin.getBlockZ();
		int maxX = cacheMax.getBlockX();
		int maxY = cacheMax.getBlockY();
		int maxZ = cacheMax.getBlockZ();
		
		if (maxX < minX || maxY < minY || maxZ < minZ)
			throw new IllegalArgumentException("Cannot create a BlockCache smaller than 1 block.");
		
		BlockData[][][] copiedBlocks = new BlockData[maxX - minX][maxY - minY][maxZ - minZ];
		BlockData cacheBorderBlock = getCacheBorderBlock(cacheWorld);
		
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					
					Block block = new Location(cacheWorld, x, y, z).getBlock();
					
					if (!isVisible(block))
						continue;
					
					BlockData blockData;
					
					if (isCacheBorder(x, y, z, minX, minY, minZ, maxX, maxY, maxZ, cacheFacing))
						blockData = cacheBorderBlock.clone();
					else
						blockData = block.getBlockData();
					
					copiedBlocks[x - minX][y - minY][z - minZ] = blockData;
				}
			}
		}
		
		return new BlockCache(new BlockVec(cacheMin), copiedBlocks, cacheFacing, cacheWorld, cacheBorderBlock);
	}
	
	/**
	 * Updates a block that changed it's appearance in a block cache, if it's not part of the cache border.
	 *
	 * @return all block copies that were affected and updated in the process.
	 */
	public static Map<BlockVec, BlockData> updateBlockInCache(
			BlockCache cache,
			Block changedBlock,
			BlockData newBlockData,
			boolean blockWasOccluding) {
		
		Map<BlockVec, BlockData> changedBlocks = new HashMap<>();
		BlockVec blockPos = new BlockVec(changedBlock);
		Material newMaterial = newBlockData.getMaterial();
		
		if (cache.isBorder(blockPos))
			return changedBlocks;
		
		BlockData oldBlockData = cache.getDataAt(blockPos);
		
		//if the block did not change it's occlusion then only the block itself needs to be updated
		if (blockWasOccluding == newMaterial.isOccluding()) {
			
			//check if the block was visible (simply listed in the array) before
			if (oldBlockData != null) {
				changedBlocks.put(blockPos, oldBlockData);
			}
			
			return changedBlocks;
		}
		
		World cacheWorld = cache.getWorld();
		
		if (oldBlockData == null) {
			oldBlockData = cacheWorld.getBlockAt(
					blockPos.getX(),
					blockPos.getY(),
					blockPos.getZ()).getBlockData();
			
			cache.setBlockCopy(blockPos, oldBlockData);
		}
		
		changedBlocks.put(blockPos, newBlockData);
		
		//hide other block copies that are now covered by this occluding block
		if (newMaterial.isOccluding()) {
			
			for (BlockVec facing : FacingUtils.getAxesBlockVecs()) {
				BlockVec touchingBlockPos = blockPos.clone().add(facing);
				
				if (!cache.contains(touchingBlockPos))
					continue;
				
				if (!cache.isBlockNowVisible(touchingBlockPos)) {
					cache.removeBlockCopyAt(touchingBlockPos);
					//TODO dont send air, just remove the fake block
					changedBlocks.put(touchingBlockPos, Material.AIR.createBlockData());
				}
			}
			
		//re-add fake blocks that are revealed by the new transparent block
		} else {
			
			for (BlockVec facing : FacingUtils.getAxesBlockVecs()) {
				BlockVec touchingBlockPos = blockPos.clone().add(facing);
				
				if (!cache.contains(touchingBlockPos) || cache.isBlockListedVisible(touchingBlockPos))
					continue;
				
				BlockData touchingBlockData = touchingBlockPos.toLocation(cacheWorld).getBlock().getBlockData();
				cache.setBlockCopy(touchingBlockPos, touchingBlockData);
				changedBlocks.put(touchingBlockPos, touchingBlockData);
			}
		}
		
		return changedBlocks;
	}
	
	//	private static boolean isCacheBorder(BlockVec blockPos, BlockCache cache) {
	//
	//		BlockVec cacheMin = cache.getMin();
	//		BlockVec cacheMax = cache.getMax();
	//
	//		return isCacheBorder(
	//				blockPos.getX(), blockPos.getY(), blockPos.getZ(),
	//				cacheMin.getX(), cacheMin.getY(), cacheMin.getZ(),
	//				cacheMax.getX(), cacheMax.getY(), cacheMax.getZ(),
	//				cache.getFacing());
	//	}
	
	/**
	 * Returns true if the block is part of the border of the cahche cuboid except the side where the portal is
	 */
	private static boolean isCacheBorder(
			int x, int y, int z,
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			Vector cacheFacing) {
		
		if (y == minY || y == maxY - 1)
			return true;
		
		if (cacheFacing.getZ() != 0) {
			if (x == minX || x == maxX - 1)
				return true;
		} else if (z == minZ || z == maxZ - 1) {
			return true;
		}
		
		if (cacheFacing.getX() == 1) {
			return x == maxX - 1;
		} else if (cacheFacing.getX() == -1) {
			return x == minX;
		} else if (cacheFacing.getZ() == 1) {
			return z == maxZ - 1;
		} else {
			return z == minZ;
		}
	}
	
	public static boolean isVisible(Block block) {
		
		for (BlockFace face : FacingUtils.getAxesFaces()) {
			if (!block.getRelative(face).getType().isOccluding())
				return true;
		}
		
		return false;
	}
	
	private static BlockData getCacheBorderBlock(World world) {
		
		switch (world.getEnvironment()) {
			case NORMAL:
				return Material.BLUE_ICE.createBlockData();
			case NETHER:
				return Material.RED_CONCRETE.createBlockData();
			case THE_END:
				return Material.BLACK_CONCRETE.createBlockData();
			default:
				return null;
		}
	}
}