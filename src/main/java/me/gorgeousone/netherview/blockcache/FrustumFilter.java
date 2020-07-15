package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.wrapping.Axis;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public final class FrustumFilter {
	
	private FrustumFilter() {}
	
	/**
	 * Returns a map of all blocks from a projection cache visible with this frustum.
	 */
	public static Map<BlockVec, BlockType> getBlocksInFrustum(BlockCache blockCache, ViewFrustum frustum) {
		
		Vector frustumFacing = frustum.getFacing();
		boolean isNegativeFrustum = frustumFacing.getX() == -1 || frustumFacing.getZ() == -1;
		
		AxisAlignedRect startLayer = isNegativeFrustum ?
				frustum.getFarPlaneRect() :
				frustum.getNearPlaneRect();
		
		AxisAlignedRect endLayer = isNegativeFrustum ?
				frustum.getNearPlaneRect() :
				frustum.getFarPlaneRect();
		
		if (startLayer.getAxis() == Axis.X) {
			
			Map.Entry<Vector, Vector> unitVecs = getSmallestUnitVecs(
					endLayer.getMin().subtract(startLayer.getMin()),
					endLayer.getMax().subtract(startLayer.getMax()));
			
			Vector layerMinIter = unitVecs.getKey();
			Vector layerMaxIter = unitVecs.getValue();
			
			return getBlocksInXAlignedFrustum(blockCache, startLayer, endLayer.getMax(), layerMinIter, layerMaxIter);
		
		} else {
			
			Map.Entry<Vector, Vector> unitVecs = getSmallestUnitVecsZ(
					endLayer.getMin().subtract(startLayer.getMin()),
					endLayer.getMax().subtract(startLayer.getMax()));
			
			Vector layerMinIter = unitVecs.getKey();
			Vector layerMaxIter = unitVecs.getValue();
			
			return getBlocksInZAlignedFrustum(blockCache, startLayer, endLayer.getMax(), layerMinIter, layerMaxIter);
		}
	}
	
	private static Map<BlockVec, BlockType> getBlocksInXAlignedFrustum(BlockCache blockCache,
	                                                                   AxisAlignedRect startLayer,
	                                                                   Vector iterationEnd,
	                                                                   Vector layerMinIter,
	                                                                   Vector layerMaxIter) {
		
		Vector currentLayerMin = startLayer.getMin();
		Vector currentLayerMax = startLayer.getMax();
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		int i = 0;
		
		System.out.println("--- start z:" + startLayer.getMin().getZ() + " end z: " + iterationEnd.getZ() + " ---");
		System.out.println(".");
		System.out.println(layerMinIter);
		System.out.println(layerMaxIter);
		System.out.println(".");
		
		for (double layerZ = currentLayerMin.getZ(); layerZ <= iterationEnd.getZ(); layerZ += layerMinIter.getZ()) {
			
			i++;
			if (i > 50) {
				System.out.println("stop stop stop " + i);
				return new HashMap<>();
			}
			
			System.out.println(i + " layer at " + layerZ);
			
			int layerMax = currentLayerMax.getBlockX();
			boolean isFirstColumn = true;
			boolean isLastColumn = false;
			
			for (double columnX = currentLayerMin.getX(); isFirstColumn || columnX <= layerMax; columnX++) {
				
				int columnMax = currentLayerMax.getBlockY();
				boolean isFirstRow = true;
				boolean isLastRow = false;
				
				if (columnX == layerMax) {
					isLastColumn = true;
				}
				
				for (double rowY = currentLayerMin.getY(); isFirstRow || rowY <= columnMax; rowY++) {
					
					if (rowY == columnMax) {
						isLastRow = true;
					}
					
					//since the iteration only passes every block at one point, the surrounding blocks have to be added on edges for accurately determining all blocks
					if (isFirstColumn || isLastColumn || isFirstRow || isLastRow) {
						addSurroundingBlocks(
								(int) columnX,
								(int) rowY,
								(int) layerZ,
								blockCache,
								blocksInFrustum);
					
					} else {
						addBlock(
								(int) columnX,
						        (int) rowY,
						        (int) layerZ,
						        blockCache,
						        blocksInFrustum);
					}
					
					isFirstRow = false;
				}
				isFirstColumn = false;
			}
			
			currentLayerMin.add(layerMinIter);
			currentLayerMax.add(layerMaxIter);
		}
		
		System.out.println("block count: " + blocksInFrustum.size());
		return blocksInFrustum;
	}
	
	private static Map<BlockVec, BlockType> getBlocksInZAlignedFrustum(BlockCache blockCache,
	                                                                   AxisAlignedRect startLayer,
	                                                                   Vector iterationEnd,
	                                                                   Vector layerMinIter,
	                                                                   Vector layerMaxIter) {
		
		Vector currentLayerMin = startLayer.getMin();
		Vector currentLayerMax = startLayer.getMax();
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		
		
		System.out.println("--- start x:" + currentLayerMin.getX() + " end x: " + iterationEnd.getX() + " ---");
		
		System.out.println(startLayer.getMin());
		System.out.println(startLayer.getMax());
		System.out.println(".");
		System.out.println(layerMinIter);
		System.out.println(layerMaxIter);
		System.out.println(".");
		int i = 0;
		
		for (double layerX = currentLayerMin.getX(); layerX <= iterationEnd.getX(); layerX += layerMinIter.getX()) {
		
			System.out.println(i + " layer at " + layerX);
			
			i++;
			if (i > 50) {
				return new HashMap<>();
			}
			
			
			int layerMax = currentLayerMax.getBlockZ();
			boolean isFirstColumn = true;
			boolean isLastColumn = false;
			
			for (double columnZ = currentLayerMin.getZ(); isFirstColumn || columnZ <= layerMax; columnZ++) {
				
				int columnMax = currentLayerMax.getBlockY();
				boolean isFirstRow = true;
				boolean isLastRow = false;
				
				if (columnZ == layerMax) {
					isLastColumn = true;
				}
				
				for (double rowY = currentLayerMin.getY(); isFirstRow || rowY <= columnMax; rowY++) {
					
					System.out.println("y " + rowY);
					if (rowY == columnMax) {
						isLastRow = true;
					}
					
					//since the iteration only passes every block at one point, the surrounding blocks have to be added on edges for accurately determining all blocks
					if (isFirstColumn || isLastColumn || isFirstRow || isLastRow) {
						addSurroundingBlocks(
								(int) layerX,
								(int) columnZ,
								(int) rowY,
								blockCache,
								blocksInFrustum);
						
					} else {
						addBlock(
								(int) layerX,
								(int) columnZ,
								(int) rowY,
						        blockCache,
						        blocksInFrustum);
					}
					
					isFirstRow = false;
				}
				isFirstColumn = false;
			}
			
			currentLayerMin.add(layerMinIter);
			currentLayerMax.add(layerMaxIter);
		}
		
		System.out.println("block count: " + blocksInFrustum.size());
		return blocksInFrustum;
	}
	
	private static Map.Entry<Vector, Vector> getSmallestUnitVecs(Vector dir1, Vector dir2) {
		
		makeVecBlocky(dir1);
		makeVecBlocky(dir2);
		
		if (roundOn(Math.abs(dir1.getY()), 1) == 1 && roundOn(Math.abs(dir2.getY()), 1) == 1) {
			
			return new AbstractMap.SimpleEntry<>(
					dir1.clone().multiply(1 / Math.abs(dir1.getY())),
					dir2.clone().multiply(1 / Math.abs(dir2.getY())));
			
		}else if (roundOn(Math.abs(dir1.getZ()), 1) == 1 && roundOn(Math.abs(dir2.getZ()), 1) == 1) {
			
			return new AbstractMap.SimpleEntry<>(
					dir1.clone().multiply(1 / Math.abs(dir1.getZ())),
					dir2.clone().multiply(1 / Math.abs(dir2.getZ())));
			
		}else {
			
			return new AbstractMap.SimpleEntry<>(
					dir1.clone().multiply(1 / Math.abs(dir1.getX())),
					dir2.clone().multiply(1 / Math.abs(dir2.getX())));
			
		}
	}
	
	private static Map.Entry<Vector, Vector> getSmallestUnitVecsZ(Vector dir1, Vector dir2) {
		
		makeVecBlocky(dir1);
		makeVecBlocky(dir2);
		
		if (roundOn(Math.abs(dir1.getY()), 1) == 1 && roundOn(Math.abs(dir2.getY()), 1) == 1) {
			
			return new AbstractMap.SimpleEntry<>(
					dir1.clone().multiply(1 / Math.abs(dir1.getY())),
					dir2.clone().multiply(1 / Math.abs(dir2.getY())));
			
		}else if (roundOn(Math.abs(dir1.getX()), 1) == 1 && roundOn(Math.abs(dir2.getX()), 1) == 1) {
			
			return new AbstractMap.SimpleEntry<>(
					dir1.clone().multiply(1 / Math.abs(dir1.getX())),
					dir2.clone().multiply(1 / Math.abs(dir2.getX())));
			
		}else {
			
			return new AbstractMap.SimpleEntry<>(
					dir1.clone().multiply(1 / Math.abs(dir1.getZ())),
					dir2.clone().multiply(1 / Math.abs(dir2.getZ())));
		
		}
	}
	
	private static double roundOn(double value, int decimals) {
		    
		double powerOfTen = Math.pow(10, decimals);
		return Math.round(value * powerOfTen) / powerOfTen;
	}
	
	/**
	 * Changes a vector's magnitude to a value where neither x, y or z exceed an absolute value of 1
	 */
	private static void makeVecBlocky(Vector vec) {
		
		double absX = Math.abs(vec.getX());
		double absY = Math.abs(vec.getY());
		double absZ = Math.abs(vec.getZ());
		
		if (absX > absY && absX > absZ) {
			vec.multiply(1 / absX);
			
		}else if (absY > absZ) {
			vec.multiply(1 / absY);
			
		}else {
			vec.multiply(1 / absZ);
		}
	}
	
	/**
	 * Adds a blocks from the block cache cache to the visible blocks in the frustum.
	 */
	private static void addBlock(int x,
	                      int y,
	                      int z,
	                      BlockCache blockCache,
	                      Map<BlockVec, BlockType> blocksInFrustum) {
		
		BlockType blockType = blockCache.getBlockTypeAt(x, y, z);
		
		if (blockType != null) {
			System.out.println("oke");
			blocksInFrustum.put(new BlockVec(x, y, z), blockType);
		}
	}
	
	/**
	 * Adds the 8 blocks from the block cache cache around a block location to the map of visible blocks in the frustum.
	 */
	private static void addSurroundingBlocks(int x,
	                                  int y,
	                                  int z,
                                      BlockCache blockCache,
	                                  Map<BlockVec, BlockType> blocksInFrustum) {
		
		for (int dx = -1; dx <= 0; dx++) {
			for (int dy = -1; dy <= 0; dy++) {
				for (int dz = -1; dz <= 0; dz++) {
					
					BlockType blockType = blockCache.getBlockTypeAt(x + dx, y + dy, z + dz);
					
					if (blockType != null) {
						//trying to reduce the object creation (the BlockVec)
						blocksInFrustum.put(new BlockVec(x + dx, y + dy, z + dz), blockType);
					}
				}
			}
		}
	}
}
