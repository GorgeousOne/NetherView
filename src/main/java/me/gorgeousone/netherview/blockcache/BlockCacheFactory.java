package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.FacingUtils;
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
	
	public static Map.Entry<BlockCache, BlockCache> createBlockCaches(Portal portal, int viewDist) {
		
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
		
		if (maxX < minX || maxY < minY || maxZ < minZ) {
			throw new IllegalArgumentException("Cannot create a BlockCache smaller than 1 block.");
		}
		
		BlockType[][][] copiedBlocks = new BlockType[maxX - minX][maxY - minY][maxZ - minZ];
		BlockType cacheBorderBlock = getCacheBorderBlock(cacheWorld);
		
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					
					Block block = new Location(cacheWorld, x, y, z).getBlock();
					
					if (!isVisible(block)) {
						continue;
					}
					
					BlockType blockType;
					
					if (isCacheBorder(x, y, z, minX, minY, minZ, maxX, maxY, maxZ, cacheFacing)) {
						blockType = cacheBorderBlock.clone();
					} else {
						blockType = BlockType.of(block);
					}
					
					copiedBlocks[x - minX][y - minY][z - minZ] = blockType;
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
	public static Map<BlockVec, BlockType> updateBlockInCache(
			BlockCache cache,
			Block changedBlock,
			BlockType newBlockData,
			boolean blockWasOccluding) {
		
		Map<BlockVec, BlockType> changedBlocks = new HashMap<>();
		BlockVec blockPos = new BlockVec(changedBlock);
		
		if (cache.isBorder(blockPos)) {
			return changedBlocks;
		}
		
		BlockType oldBlockType = cache.getBlockTypeAt(blockPos);
		
		//if the block did not change it's occlusion then only the block itself needs to be updated
		if (blockWasOccluding == newBlockData.isOccluding()) {
			
			changedBlocks.put(blockPos, newBlockData);
			return changedBlocks;
		}
		
		World cacheWorld = cache.getWorld();
		
		if (oldBlockType == null) {
			
			oldBlockType = BlockType.of(cacheWorld.getBlockAt(
					blockPos.getX(),
					blockPos.getY(),
					blockPos.getZ()));
			
			cache.setBlockTypeAt(blockPos, oldBlockType);
		}
		
		changedBlocks.put(blockPos, newBlockData);
		
		//hide other block copies that are now covered by this occluding block
		//they don't need to be redisplayed
		if (newBlockData.isOccluding()) {
			
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
				
				BlockType touchingBlockData = BlockType.of(touchingBlockPos.toLocation(cacheWorld).getBlock());
				cache.setBlockTypeAt(touchingBlockPos, touchingBlockData);
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
	
	private static BlockType getCacheBorderBlock(World world) {
		
		switch (world.getEnvironment()) {
			case NORMAL:
				return BlockType.match("BLUE_ICE", "STAINED_CLAY", (byte) 0);
			case NETHER:
				return BlockType.match("RED_CONCRETE", "STAINED_CLAY", (byte) 14);
			case THE_END:
				return BlockType.match("BLACK_CONCRETE", "STAINED_CLAY", (byte) 11);
			default:
				return null;
		}
	}
}