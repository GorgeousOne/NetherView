package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.utils.FacingUtils;
import me.gorgeousone.netherview.blocktype.BlockType;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class BlockCacheFactory {
	
	public static Map.Entry<BlockCache, BlockCache> createBlockCaches(Portal portal,
	                                                                  int viewDist,
	                                                                  BlockType cacheBorderBlockType) {
		
		//theoretically the view distance needs to be increased by 1 for the extra layer of border around the cuboid of blocks.
		//but somehow it's 2. Don't ask me.
		viewDist += 2;
		
		AxisAlignedRect portalRect = portal.getPortalRect();
		Vector portalFacing = portalRect.getNormal();
		Vector widthFacing = portalRect.getCrossNormal();
		
		//the view distance in blocks to the front shall be greater than at the sides
		int frontViewDist = viewDist;
		int horizontalViewDist = (int) Math.max(2, viewDist - portalRect.width()) / 2;
		int verticalViewDist = (int) Math.max(2, viewDist - portalRect.height()) / 2;
		
		Vector cacheCorner1 = portalRect.getMin();
		cacheCorner1.subtract(new Vector(0, verticalViewDist, 0));
		cacheCorner1.subtract(widthFacing.clone().multiply(horizontalViewDist));
		
		Vector cacheCorner2 = portalRect.getMax();
		cacheCorner2.add(new Vector(0, verticalViewDist, 0));
		cacheCorner2.add(widthFacing.clone().multiply(horizontalViewDist));
		
		//TODO pass parameters more efficient?
		BlockCache front = copyBlocksInBounds(
				portal,
				cacheCorner1.clone().add(portalFacing),
				cacheCorner2.clone().add(portalFacing.clone().multiply(frontViewDist)),
				portalFacing,
				cacheBorderBlockType);
		
		BlockCache back = copyBlocksInBounds(
				portal,
				cacheCorner1.clone().subtract(portalFacing.clone().multiply(frontViewDist - 1)),
				cacheCorner2,
				portalFacing.clone().multiply(-1),
				cacheBorderBlockType);
		
		return new AbstractMap.SimpleEntry<>(front, back);
	}
	
	//TODO rather copy the blocs into an empty BlockCache?
	private static BlockCache copyBlocksInBounds(Portal portal,
	                                             Vector cacheCorner1,
	                                             Vector cacheCorner2,
	                                             Vector cacheFacing,
	                                             BlockType cacheBorderBlockType) {
		
		Vector cacheMin = Vector.getMinimum(cacheCorner1, cacheCorner2);
		Vector cacheMax = Vector.getMaximum(cacheCorner1, cacheCorner2);
		
		int minX = cacheMin.getBlockX();
		int minY = cacheMin.getBlockY();
		int minZ = cacheMin.getBlockZ();
		int maxX = cacheMax.getBlockX();
		int maxY = cacheMax.getBlockY();
		int maxZ = cacheMax.getBlockZ();
		
		if (maxX < minX || maxY < minY || maxZ < minZ) {
			throw new IllegalArgumentException("Cannot create a BlockCache smaller than 1 block.");
		}
		
		BlockType[][][] copiedBlocks = new BlockType[maxX - minX][maxY - minY][maxZ - minZ];
		World cacheWorld = portal.getWorld();
		
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					
					Block block = new Location(cacheWorld, x, y, z).getBlock();
					
					if (!isVisible(block)) {
						continue;
					}
					
					BlockType blockType = BlockType.of(block);
					
					//make sure that the cache border onl consists of occluding blocks
					if (!blockType.isOccluding() && isCacheBorder(x, y, z, minX, minY, minZ, maxX, maxY, maxZ, cacheFacing)) {
						blockType = cacheBorderBlockType.clone();
					}
					
					copiedBlocks[x - minX][y - minY][z - minZ] = blockType;
				}
			}
		}
		
		return new BlockCache(portal, new BlockVec(cacheMin), copiedBlocks, cacheFacing, cacheBorderBlockType);
	}
	
	/**
	 * Updates a block that changed it's appearance in a block cache, if it's not part of the cache border.
	 *
	 * @return all block copies that were affected and updated in the process.
	 */
	public static Map<BlockVec, BlockType> updateBlockInCache(
			BlockCache cache,
			Block changedBlock,
			BlockType newBlockType,
			boolean blockWasOccluding) {
		
		
		BlockVec blockPos = new BlockVec(changedBlock);
		Map<BlockVec, BlockType> changedBlocks = new HashMap<>();
		
		//any new transparent block in the cache border will be replaced with the border block type (of course)
		if (!newBlockType.isOccluding() && cache.isBorder(blockPos)) {
			newBlockType = cache.getBorderBlockType();
		}
		
		//if the block did not change it's occlusion then only the block itself needs to be updated
		if (blockWasOccluding == newBlockType.isOccluding()) {
			
			//return no cache updates when the block is hidden as border anyway
			if (!blockWasOccluding && cache.isBorder(blockPos)) {
				return changedBlocks;
			}
			
			cache.setBlockTypeAt(blockPos, newBlockType);
			changedBlocks.put(blockPos, newBlockType);
			return changedBlocks;
		}
		
		World cacheWorld = cache.getWorld();
		
		//hide other block copies that are now covered by this occluding block
		//but they don't need to be updated in the projections
		if (newBlockType.isOccluding()) {
			
			for (BlockVec facing : FacingUtils.getAxesBlockVecs()) {
				
				BlockVec touchingBlockPos = blockPos.clone().add(facing);
				
				if (cache.contains(touchingBlockPos) && !cache.isBlockNowVisible(touchingBlockPos)) {
					cache.removeBlockDataAt(touchingBlockPos);
				}
			}
			
			//re-add fake blocks that are revealed by the new transparent block
		} else {
			
			for (BlockVec facing : FacingUtils.getAxesBlockVecs()) {
				
				BlockVec touchingBlockPos = blockPos.clone().add(facing);
				
				if (!cache.contains(touchingBlockPos) || cache.isBlockListedVisible(touchingBlockPos)) {
					continue;
				}
				
				BlockType touchingBlockType = BlockType.of(cacheWorld.getBlockAt(
						touchingBlockPos.getX(),
						touchingBlockPos.getY(),
						touchingBlockPos.getZ()));
				
				if (!touchingBlockType.isOccluding() && cache.isBorder(touchingBlockPos)) {
					touchingBlockType = cache.getBorderBlockType();
				}
				
				cache.setBlockTypeAt(touchingBlockPos, touchingBlockType);
				changedBlocks.put(touchingBlockPos, touchingBlockType);
			}
		}
		
		changedBlocks.put(blockPos, newBlockType);
		return changedBlocks;
	}

//		private static boolean isCacheBorder(BlockVec blockPos, BlockCache cache) {
//
//			BlockVec cacheMin = cache.getMin();
//			BlockVec cacheMax = cache.getMax();
//
//			return isCacheBorder(
//					blockPos.getX(), blockPos.getY(), blockPos.getZ(),
//					cacheMin.getX(), cacheMin.getY(), cacheMin.getZ(),
//					cacheMax.getX(), cacheMax.getY(), cacheMax.getZ(),
//					cache.getFacing());
//		}
	
	/**
	 * Returns true if the block is part of the border of the cache cuboid except the side where the portal is
	 */
	private static boolean isCacheBorder(
			int x, int y, int z,
			int minX, int minY, int minZ,
			int maxX, int maxY, int maxZ,
			Vector cacheFacing) {
		
		if (y == minY || y == maxY - 1) {
			return true;
		}
		
		if (cacheFacing.getZ() != 0) {
			if (x == minX || x == maxX - 1) {
				return true;
			}
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
			if (!block.getRelative(face).getType().isOccluding()) {
				return true;
			}
		}
		
		return false;
	}
}