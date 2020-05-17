package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProjectionCache {
	
	private BlockCache sourceCache;
	private Transform blockTransform;
	
	private BlockData[][][] blockCopies;
	private BlockVec min;
	private BlockVec max;
	private Portal portal;
	
	public ProjectionCache(Portal projectionPortal, BlockCache sourceCache, Transform blockTransform) {
		
		this.sourceCache = sourceCache;
		this.blockTransform = blockTransform;
		this.portal = projectionPortal;
		createBlockCopies();
	}
	
	public Portal getPortal() {
		return portal;
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
	
	public BlockData updateCopy(BlockVec blockPos, BlockData sourceData) {
		
		BlockData newBlockData = blockTransform.rotateData(sourceData.clone());
		
		blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()] = newBlockData;
		
		return newBlockData;
	}
	
	private void createBlockCopies() {
		
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
					BlockData blockData = sourceCache.getDataAt(blockPos);
					
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
		
		int x = blockCorner.getX();
		int y = blockCorner.getY();
		int z = blockCorner.getZ();
		
		locsAroundCorner.add(new BlockVec(x, y, z));
		locsAroundCorner.add(new BlockVec(x, y - 1, z));
		locsAroundCorner.add(new BlockVec(x, y, z - 1));
		locsAroundCorner.add(new BlockVec(x, y - 1, z - 1));
		
		locsAroundCorner.add(new BlockVec(x - 1, y, z));
		locsAroundCorner.add(new BlockVec(x - 1, y - 1, z));
		locsAroundCorner.add(new BlockVec(x - 1, y, z - 1));
		locsAroundCorner.add(new BlockVec(x - 1, y - 1, z - 1));
		
		return locsAroundCorner;
	}
}