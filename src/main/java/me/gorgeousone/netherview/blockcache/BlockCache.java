package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.utils.FacingUtils;
import me.gorgeousone.netherview.wrapper.WrappedBoundingBox;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * One big array of BlockTypes used to store information about all blocks in a cuboid area around a portal.
 * For each of the 2 sides of a portal there is a separate BlockCache.
 */
public class BlockCache {
	
	private final Portal portal;
	private final Set<Chunk> chunks;
	
	private final BlockType[][][] blockCopies;
	private final BlockVec min;
	private final BlockVec max;
	
	private final BlockVec facing;
	private final BlockType borderType;
	
	public BlockCache(Portal portal,
	                  BlockVec offset,
	                  BlockVec size,
	                  BlockVec facing,
	                  BlockType borderType) {
		
		this.portal = portal;
		this.chunks = new HashSet<>();
		
		this.blockCopies = new BlockType[size.getX()][size.getY()][size.getZ()];
		this.min = offset.clone();
		this.max = offset.clone().add(size());
		
		this.facing = facing;
		this.borderType = borderType;
		
		for (int chunkX = min.getX() >> 4; chunkX <= max.getX() >> 4; chunkX++) {
			for (int chunkZ = min.getZ() >> 4; chunkZ <= max.getZ() >> 4; chunkZ++) {
				
				chunks.add(portal.getWorld().getChunkAt(chunkX, chunkZ));
			}
		}
	}
	
	private BlockVec size() {
		return new BlockVec(blockCopies.length, blockCopies[0].length, blockCopies[0][0].length);
	}
	
	public Portal getPortal() {
		return portal;
	}
	
	public World getWorld() {
		return portal.getWorld();
	}
	
	public Set<Chunk> getChunks() {
		return chunks;
	}
	
	public BlockVec getMin() {
		return min.clone();
	}
	
	public BlockVec getMax() {
		return max.clone();
	}
	
	public BlockVec getFacing() {
		return facing.clone();
	}
	
	public boolean contains(BlockVec loc) {
		return contains(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public boolean contains(Vector loc) {
		return contains(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public boolean contains(double x, double y, double z) {
		return x >= min.getX() && x < max.getX() &&
		       y >= min.getY() && y < max.getY() &&
		       z >= min.getZ() && z < max.getZ();
	}
	
	public boolean isBorder(BlockVec loc) {
		return isBorder(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public BlockType getBorderBlockType() {
		return borderType.clone();
	}
	
	/**
	 * Returns true if the block is at any position bordering the cuboid except the side facing the portal.
	 */
	public boolean isBorder(int x, int y, int z) {
		
		if (y == min.getY() || y == max.getY() - 1) {
			return true;
		}
		
		int minX = min.getX();
		int minZ = min.getZ();
		int maxX = max.getX() - 1;
		int maxZ = max.getZ() - 1;
		
		if (facing.getZ() != 0) {
			if (x == minX || x == maxX) {
				return true;
			}
		} else if (z == minZ || z == maxZ) {
			return true;
		}
		
		if (facing.getX() == 1) {
			return x == maxX;
		}
		if (facing.getX() == -1) {
			return x == minX;
		}
		if (facing.getZ() == 1) {
			return z == maxZ;
		} else {
			return z == minZ;
		}
	}
	
	public BlockType getBlockTypeAt(BlockVec blockPos) {
		return getBlockTypeAt(blockPos.getX(),
		                      blockPos.getY(),
		                      blockPos.getZ());
	}
	
	public BlockType getBlockTypeAt(int x, int y, int z) {
		
		if (!contains(x, y, z)) {
			return null;
		}
		
		return blockCopies
				[x - min.getX()]
				[y - min.getY()]
				[z - min.getZ()];
	}
	
	public void setBlockTypeAt(BlockVec blockPos, BlockType blockType) {
		setBlockTypeAt(
				blockPos.getX(),
				blockPos.getY(),
				blockPos.getZ(),
				blockType);
	}
	
	public void setBlockTypeAt(int x, int y, int z, BlockType blockType) {
		blockCopies
				[x - min.getX()]
				[y - min.getY()]
				[z - min.getZ()] = blockType;
	}
	
	public void removeBlockDataAt(BlockVec blockPos) {
		blockCopies
				[blockPos.getX() - min.getX()]
				[blockPos.getY() - min.getY()]
				[blockPos.getZ() - min.getZ()] = null;
	}
	
	/**
	 * Returns true if the block copy at the given position is listed as visible (when it's not null)
	 */
	public boolean isBlockListedVisible(BlockVec blockPos) {
		return getBlockTypeAt(blockPos) != null;
	}
	
	/**
	 * Returns true if the block copy at the given position is visible by checking the surrounding blocks in the world for transparent ones
	 */
	public boolean isBlockNowVisible(BlockVec blockPos) {
		
		for (BlockVec facing : FacingUtils.getAxesBlockVecs()) {
			
			BlockVec touchingBlockPos = blockPos.clone().add(facing);
			
			Block touchingBlock = getWorld().getBlockAt(
					touchingBlockPos.getX(),
					touchingBlockPos.getY(),
					touchingBlockPos.getZ());
			
			
			if (!BlockType.of(touchingBlock).isOccluding()) {
				return true;
			}
		}
		
		//TODO check if block is directly in front of the portal.
		return false;
	}
	
	private final Map<Chunk, Set<Entity>> unloadedChunks = new HashMap<>();
	
	/**
	 * Returns a set of all entities that are contained by this cache
	 */
	public Set<Entity> getEntities() {
		
		Set<Entity> containedEntities = new HashSet<>();
		
		for (Chunk chunk : chunks) {
			
			if (!getWorld().isChunkLoaded(chunk)) {
				
				unloadedChunks.computeIfAbsent(chunk, set -> addContainedEntities(chunk, new HashSet<>()));
				containedEntities.addAll(unloadedChunks.get(chunk));
				continue;
			}
			
			addContainedEntities(chunk, containedEntities);
			unloadedChunks.remove(chunk);
		}
		
		return containedEntities;
	}
	
	private Set<Entity> addContainedEntities(Chunk chunk, Set<Entity> setToAddTo) {
		
		boolean wasChunkLoaded = getWorld().isChunkLoaded(chunk);
		
		for (Entity entity : chunk.getEntities()) {
			
			if (WrappedBoundingBox.of(entity).intersectsBlockCache(this)) {
				setToAddTo.add(entity);
			}
		}
		
		if (!wasChunkLoaded) {
			getWorld().unloadChunk(chunk);
		}
		
		return setToAddTo;
	}
}