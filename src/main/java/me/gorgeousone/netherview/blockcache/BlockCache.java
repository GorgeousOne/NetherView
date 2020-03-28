package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.threedstuff.Transform;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class BlockCache {
	
	private Block[][][] sourceBlocks;
	private BlockCopy[][][] blockCopies;
	
	private Vector min;
	private Vector max;
	private Transform blockTransform;
	
	public BlockCache(Block[][][] sourceBlocks, Vector offset, Transform blockTransform) {
		
		this.sourceBlocks = sourceBlocks;
		this.blockTransform = blockTransform;
		
		createTransformedBounds(offset);
		createBlockCopies();
	}
	
	private Vector sourceCacheSize() {
		return new Vector(sourceBlocks.length, sourceBlocks[0].length, sourceBlocks[0][0].length);
	}
	
	public Vector getCopyMin() {
		return min.clone();
	}
	
	public Vector getCopyMax() {
		return max.clone();
	}
	
	public BlockCopy getCopyAt(Vector loc) {
		return blockCopies
				[loc.getBlockX() - min.getBlockX()]
				[loc.getBlockY() - min.getBlockY()]
				[loc.getBlockZ() - min.getBlockZ()];
	}
	
	public boolean copiesContain(Vector loc) {
		return loc.getBlockX() >= min.getX() && loc.getBlockX() < max.getX() &&
		       loc.getBlockY() >= min.getY() && loc.getBlockY() < max.getY() &&
		       loc.getBlockZ() >= min.getZ() && loc.getBlockZ() < max.getZ();
	}
	
	public Set<BlockCopy> getCopiesAround(int x, int y, int z) {
		
		Set<BlockCopy> blocksAroundCorner = new HashSet<>();
		
		for (Vector loc : getAllCornerLocs(x, y, z)) {
			if (copiesContain(loc))
				blocksAroundCorner.add(getCopyAt(loc));
		}
		
		return blocksAroundCorner;
	}
	
	private Set<Vector> getAllCornerLocs(int x, int y, int z) {
		
		Set<Vector> locsAroundCorner = new HashSet<>();
		
		locsAroundCorner.add(new Vector(x, y, z));
		locsAroundCorner.add(new Vector(x - 1, y, z));
		locsAroundCorner.add(new Vector(x, y, z - 1));
		locsAroundCorner.add(new Vector(x - 1, y, z - 1));
		locsAroundCorner.add(new Vector(x, y - 1, z));
		locsAroundCorner.add(new Vector(x - 1, y - 1, z));
		locsAroundCorner.add(new Vector(x, y - 1, z - 1));
		locsAroundCorner.add(new Vector(x - 1, y - 1, z - 1));
		
		return locsAroundCorner;
	}
	
	private void createTransformedBounds(Vector offset) {
		
		Vector sourceSize = sourceCacheSize();
		
		Vector transformedMin = blockTransform.getTransformed(offset);
		Vector transformedMax = transformedMin.clone().add(blockTransform.rotate(sourceSize));
		
		min = new Vector(Math.min(transformedMin.getBlockX(), transformedMax.getBlockX()),
		                 Math.min(transformedMin.getBlockY(), transformedMax.getBlockY()),
		                 Math.min(transformedMin.getBlockZ(), transformedMax.getBlockZ()));
		
		max = new Vector(Math.max(transformedMin.getBlockX(), transformedMax.getBlockX()),
		                 Math.max(transformedMin.getBlockY(), transformedMax.getBlockY()),
		                 Math.max(transformedMin.getBlockZ(), transformedMax.getBlockZ()));
	}
	
	private void createBlockCopies() {
		
		Vector sourceSize = sourceCacheSize();
		Vector copySize = blockTransform.rotate(sourceSize.clone());
		
		blockCopies = new BlockCopy
				[copySize.getBlockX()]
				[copySize.getBlockY()]
				[copySize.getBlockZ()];
		
		for (int x = 0; x < sourceSize.getX(); x++){
			for (int y = 0; y < sourceSize.getY(); y++) {
				for (int z = 0; z < sourceSize.getZ(); z++) {
				
					Block block = sourceBlocks[x][y][z];
					Vector copyPos = blockTransform.getTransformed(block.getLocation().toVector());
					BlockCopy blockCopy = new BlockCopy(copyPos, block.getBlockData());
					
					Vector copyIndex = blockTransform.rotate(new Vector(x, y, z));
					
					blockCopies[copyIndex.getBlockX()]
							[copyIndex.getBlockY()]
							[copyIndex.getBlockZ()] = blockCopy;
				}
			}
		}
	}
}
