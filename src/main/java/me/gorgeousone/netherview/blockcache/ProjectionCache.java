package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Axis;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProjectionCache {
	
	private Portal portal;
	private Transform blockTransform;
	
	private BlockData[][][] blockCopies;
	private BlockVec min;
	private BlockVec max;
	
	private int cacheLength;
	
	public ProjectionCache(Portal projectionPortal, BlockCache sourceCache, Transform blockTransform) {
		
		this.portal = projectionPortal;
		this.blockTransform = blockTransform;
		
		createBlockCopies(sourceCache);
		
		if (portal.getAxis() == Axis.X)
			cacheLength = blockCopies[0][0].length;
		else
			cacheLength = blockCopies.length;
	}
	
	public Portal getPortal() {
		return portal;
	}
	
	public Transform getTransform() {
		return blockTransform;
	}
	
	public World getWorld() {
		return portal.getWorld();
	}
	
	public BlockVec getMin() {
		return min.clone();
	}
	
	public BlockVec getMax() {
		return max.clone();
	}
	
	/**
	 * Returns the length of the projection cache measured from portal to back wall.
	 * The value is important for the length of viewing frustums.
	 */
	public int getCacheLength() {
		return cacheLength;
	}
	
	public boolean contains(BlockVec loc) {
		return loc.getX() >= min.getX() && loc.getX() < max.getX() &&
		       loc.getY() >= min.getY() && loc.getY() < max.getY() &&
		       loc.getZ() >= min.getZ() && loc.getZ() < max.getZ();
	}
	
	public BlockData getCopyAt(BlockVec loc) {
		
		if (!contains(loc))
			return null;
		
		return blockCopies
				[loc.getX() - min.getX()]
				[loc.getY() - min.getY()]
				[loc.getZ() - min.getZ()];
	}
	
	public Map<BlockVec, BlockData> getCopiesAround(BlockVec blockCorner) {
		
		Map<BlockVec, BlockData> blocksAroundCorner = new HashMap<>();
		
		for (BlockVec blockPos : getAllCornerLocs(blockCorner)) {
			
			if (!contains(blockPos))
				continue;
			
			BlockData blockData = getCopyAt(blockPos);
			
			if (blockData != null)
				blocksAroundCorner.put(blockPos, blockData.clone());
		}
		
		return blocksAroundCorner;
	}
	
	public void updateCopy(BlockVec blockPos, BlockData newBlockData) {
		
		blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()] = newBlockData;
	}
	
	private void createBlockCopies(BlockCache sourceCache) {
		
		BlockVec sourceMin = sourceCache.getMin();
		BlockVec sourceMax = sourceCache.getMax();
		
		BlockVec corner1 = blockTransform.transformVec(sourceMin.clone());
		BlockVec corner2 = blockTransform.transformVec(sourceMax.clone());
		
		min = BlockVec.getMinimum(corner1, corner2);
		max = BlockVec.getMaximum(corner1, corner2).add(1, 0, 1);
		
		int minX = min.getX();
		int minY = min.getY();
		int minZ = min.getZ();
		
		blockCopies = new BlockData
				[max.getX() - minX]
				[max.getY() - minY]
				[max.getZ() - minZ];
		
		for (int x = sourceMin.getX(); x < sourceMax.getX(); x++) {
			for (int y = sourceMin.getY(); y < sourceMax.getY(); y++) {
				for (int z = sourceMin.getZ(); z < sourceMax.getZ(); z++) {
					
					BlockVec blockPos = new BlockVec(x, y, z);
					BlockData blockData = sourceCache.getBlockDataAt(blockPos);
					
					if (blockData == null)
						continue;
					
					BlockData rotatedData = blockTransform.rotateData(blockData.clone());
					BlockVec newBlockPos = blockTransform.transformVec(blockPos);
					
					blockCopies
							[newBlockPos.getX() - minX]
							[newBlockPos.getY() - minY]
							[newBlockPos.getZ() - minZ] = rotatedData;
				}
			}
		}
	}
	
	private Set<BlockVec> getAllCornerLocs(BlockVec blockCorner) {
		
		Set<BlockVec> locsAroundCorner = new HashSet<>();
		
		for (int dx = -1; dx <= 0; dx++) {
			for (int dy = -1; dy <= 0; dy++) {
				for (int dz = -1; dz <= 0; dz++) {
					locsAroundCorner.add(new BlockVec(
							blockCorner.getX() + dx,
							blockCorner.getY() + dy,
							blockCorner.getZ() + dz));
				}
			}
		}
		
		return locsAroundCorner;
	}
}