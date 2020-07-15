package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.utils.FacingUtils;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public final class BlockCacheFactory {
	
	private BlockCacheFactory() {}
	
	public static Map.Entry<BlockCache, BlockCache> createBlockCaches(Portal portal,
	                                                                  int viewDist,
	                                                                  BlockType cacheBorderBlockType) {
		
		//increasing the view distance by 1 to fit in the extra layer of border
		viewDist += 1;
		
		AxisAlignedRect portalRect = portal.getPortalRect();
		Vector portalFacing = portalRect.getNormal();
		Vector widthFacing = portalRect.getCrossNormal();
		
		Vector cacheCorner1 = portalRect.getMin();
		cacheCorner1.subtract(new Vector(0, viewDist, 0));
		cacheCorner1.subtract(widthFacing.clone().multiply(viewDist));
		
		Vector cacheCorner2 = portalRect.getMax();
		cacheCorner2.add(new Vector(0, viewDist, 0));
		cacheCorner2.add(widthFacing.clone().multiply(viewDist));
		
		int minPortalExtent = (int) Math.min(portalRect.width(), portalRect.height());
		int frontViewDist = minPortalExtent + 2 * viewDist;
		
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
		
		World cacheWorld = portal.getWorld();
		BlockVec cacheSize = new BlockVec(cacheMax.clone().subtract(cacheMin));
		
		BlockCache blockCache = new BlockCache(
				portal,
				new BlockVec(cacheMin),
				cacheSize,
				new BlockVec(cacheFacing),
				cacheBorderBlockType);
		
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					
					Block block = new Location(cacheWorld, x, y, z).getBlock();
					
					if (!isVisible(block)) {
						continue;
					}
					
					BlockType blockType = BlockType.of(block);
					
					//make sure that the cache border onl consists of occluding blocks
					if (!blockType.isOccluding() && blockCache.isBorder(x, y, z)) {
						blockType = cacheBorderBlockType.clone();
					}
					
					blockCache.setBlockTypeAt(x, y, z, blockType);
				}
			}
		}
		
		return blockCache;
	}
	
	public static Map.Entry<ProjectionCache, ProjectionCache> createProjectionCaches(BlockCache frontCache,
	                                                                                 BlockCache backCache,
	                                                                                 Transform portalLinkTransform) {
		
		return new AbstractMap.SimpleEntry<>(
				createProjection(backCache, portalLinkTransform),
				createProjection(frontCache, portalLinkTransform));
	}
	
	/**
	 * Creates projection cache from the content of a block cache.
	 *
	 * @param sourceCache   block cache to be copied
	 * @param linkTransform transformation between the locations of the block cache and the projection cache
	 */
	public static ProjectionCache createProjection(BlockCache sourceCache, Transform linkTransform) {
		
		BlockVec sourceMin = sourceCache.getMin();
		BlockVec sourceMax = sourceCache.getMax();
		
		BlockVec corner1 = linkTransform.transformVec(sourceMin.clone());
		BlockVec corner2 = linkTransform.transformVec(sourceMax.clone().add(-1, 0, -1));
		
		BlockVec projectionMin = BlockVec.getMinimum(corner1, corner2);
		BlockVec projectionMax = BlockVec.getMaximum(corner1, corner2).add(1, 0, 1);
		BlockVec projectionSize = projectionMax.clone().subtract(projectionMin);
		
		ProjectionCache projectionCache = new ProjectionCache(
				sourceCache.getPortal(),
				projectionMin,
				projectionSize,
				linkTransform.transformVec(sourceCache.getFacing()),
				sourceCache.getBorderBlockType(),
				linkTransform);
		
		for (int x = sourceMin.getX(); x < sourceMax.getX(); x++) {
			for (int y = sourceMin.getY(); y < sourceMax.getY(); y++) {
				for (int z = sourceMin.getZ(); z < sourceMax.getZ(); z++) {
					
					BlockVec blockPos = new BlockVec(x, y, z);
					BlockType blockType = sourceCache.getBlockTypeAt(blockPos);
					
					if (blockType == null) {
						continue;
					}
					
					BlockType rotatedBlockType = blockType.clone().rotate(linkTransform.getQuarterTurns());
					BlockVec newBlockPos = linkTransform.transformVec(blockPos);
					projectionCache.setBlockTypeAt(newBlockPos, rotatedBlockType);
				}
			}
		}
		
		return projectionCache;
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
		
		if (newBlockType.isOccluding()) {
			changedBlocks.putAll(hideCoveredBlocks(cache, blockPos));
			
		} else {
			changedBlocks.putAll(reincludeRevealedBlocks(cache, blockPos));
		}
		
		changedBlocks.put(blockPos, newBlockType);
		return changedBlocks;
	}
	
	/**
	 * Removes all block copies from a block cache that have been covered by a new occluding block.
	 */
	private static Map<BlockVec, BlockType> hideCoveredBlocks(BlockCache cache, BlockVec addedBlock) {
		
		Map<BlockVec, BlockType> changedBlocks = new HashMap<>();
		
		for (BlockVec facing : FacingUtils.getAxesBlockVecs()) {
			
			BlockVec touchingBlockPos = addedBlock.clone().add(facing);
			
			if (cache.contains(touchingBlockPos) && !cache.isBlockNowVisible(touchingBlockPos)) {
				cache.removeBlockDataAt(touchingBlockPos);
				changedBlocks.put(touchingBlockPos.clone(), null);
			}
		}
		
		return changedBlocks;
	}
	
	/**
	 * Re-includes block copies to the cache that have been revealed by a new transparent block.
	 *
	 * @return a map of all changed blocks
	 */
	private static Map<BlockVec, BlockType> reincludeRevealedBlocks(BlockCache cache, BlockVec removedBlock) {
		
		Map<BlockVec, BlockType> changedBlocks = new HashMap<>();
		World cacheWorld = cache.getWorld();
		
		for (BlockVec facing : FacingUtils.getAxesBlockVecs()) {
			
			BlockVec touchingBlockPos = removedBlock.clone().add(facing);
			
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
		
		return changedBlocks;
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