package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.threedstuff.FacingUtils;
import org.bukkit.Axis;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Orientable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PortalLocator {
	
	public static Portal locatePortalStructure(Block portalBlock) {
		
		World world = portalBlock.getWorld();
		AxisAlignedRect portalRect = getPortalRect(portalBlock);
		
		BlockVec portalMin = new BlockVec(portalRect.getMin());
		BlockVec portalMax = new BlockVec(portalRect.getMax());
		portalMax.add(new BlockVec(portalRect.getPlane().getNormal()));
		
		Set<Block> innerBlocks = getInnerPortalBlocks(world, portalMin, portalMax);
		
		Axis portalAxis = portalRect.getAxis();
		BlockVec frameExtension = new BlockVec(FacingUtils.getAxisWidthFacing(portalAxis));
		frameExtension.setY(1);
		
		portalMin.subtract(frameExtension);
		portalMax.add(frameExtension);
		
		Set<Block> frameBlocks = getPortalFrameBlocks(world, portalMin, portalMax, portalAxis);
			
		return new Portal(world, portalRect, innerBlocks, frameBlocks, portalMin, portalMax);
	}
	
	/**
	 * Returns a rectangle with the size and location of the rectangle the inner portal blocks form.
	 */
	private static AxisAlignedRect getPortalRect(Block portalBlock) {
		
		Orientable portalData = (Orientable) portalBlock.getBlockData();
		Axis portalAxis = portalData.getAxis();
		
		Vector position = new Vector(
				portalBlock.getX(),
				getPortalExtent(portalBlock, BlockFace.DOWN).getY(),
				portalBlock.getZ());
		
		int height = getPortalExtent(portalBlock, BlockFace.UP).getY() + 1 - position.getBlockY();
		int width;
		
		if (portalAxis == Axis.X) {
			position.setX(getPortalExtent(portalBlock, BlockFace.WEST).getX());
			width = getPortalExtent(portalBlock, BlockFace.EAST).getX() - position.getBlockX() + 1;
		
		} else {
			position.setZ(getPortalExtent(portalBlock, BlockFace.NORTH).getZ());
			width = getPortalExtent(portalBlock, BlockFace.SOUTH).getZ() - position.getBlockZ() + 1;
		}
		
		//translate the portalRect towards the middle of the block;
		AxisAlignedRect portalRect = new AxisAlignedRect(portalAxis, position, width, height);
		portalRect.translate(portalRect.getPlane().getNormal().multiply(0.5));
		
		return portalRect;
	}
	
	/**
	 * Returns the last block of the portal inner into a certain direction.
	 */
	private static Block getPortalExtent(Block sourceBlock, BlockFace facing) {
		
		Block blockIterator = sourceBlock;
		
		for (int i = 0; i < 22; i++) {
			
			Block nextBlock = blockIterator.getRelative(facing);
			
			if (nextBlock.getType() != Material.NETHER_PORTAL)
				return blockIterator;
			
			blockIterator = nextBlock;
		}
		
		throw new IllegalArgumentException(ChatColor.GRAY + "" + ChatColor.ITALIC + "This portal appears to be of extraordinary size!");
	}
	
	/**
	 * Returns a set of blocks of all inner blocks of a portal according to the passed rectangle.
	 */
	private static Set<Block> getInnerPortalBlocks(World world, BlockVec portalMin, BlockVec portalMax) {
		
		Set<Block> portalBlocks = new HashSet<>();
		
		for(int x = portalMin.getX(); x < portalMax.getX(); x++) {
			for(int y = portalMin.getY(); y < portalMax.getY(); y++) {
				for(int z = portalMin.getZ(); z < portalMax.getZ(); z++) {
					
					Block portalBlock = world.getBlockAt(x, y, z);
					
					if(portalBlock.getType() == Material.NETHER_PORTAL) {
						portalBlocks.add(portalBlock);
					}else {
						throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "This portal seems to be malformed, yet intact. Mysterious...");
					}
				}
			}
		}
		
		return portalBlocks;
	}
	
	/**
	 * Returns a set of blocks where obsidian blocks need to be placed.
	 */
	private static Set<Block> getPortalFrameBlocks(World world, BlockVec portalMin, BlockVec portalMax, Axis portalAxis) {
		
		Set<Block> frameBlocks = new HashSet<>();
		
		int portalMinX = portalMin.getX();
		int portalMinY = portalMin.getY();
		int portalMinZ = portalMin.getZ();
		int portalMaxX = portalMax.getX();
		int portalMaxY = portalMax.getY();
		int portalMaxZ = portalMax.getZ();
		
		for(int x = portalMinX; x < portalMaxX; x++) {
			for(int y = portalMinY; y < portalMaxY; y++) {
				for(int z = portalMinZ; z < portalMaxZ; z++) {
					
					//only check the frame blocks that are at the border of this "flat cuboid"
					if(y > portalMinY && y < portalMaxY-1 &&
					   (portalAxis == Axis.X ? x > portalMinX : z > portalMinZ) &&
					   (portalAxis == Axis.X ? x < portalMaxX-1 : z < portalMaxZ-1))
						continue;
					
					Block portalBlock = world.getBlockAt(x, y, z);
					
					if(portalBlock.getType() == Material.OBSIDIAN) {
						frameBlocks.add(portalBlock);
					}else {
						throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "Something about this portal frame seems to be incomplete...");
					}
				}
			}
		}
		
		return frameBlocks;
	}
}