package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import org.bukkit.Axis;
import org.bukkit.Location;
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
		
		return new Portal(
				world,
				portalRect,
				getInnerPortalBlocks(world, portalRect),
				getPortalFrameBlocks(world, portalRect));
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
		
		throw new IllegalArgumentException("Portal appears to be bigger than possible in vanilla.");
	}
	
	/**
	 * Returns a set of blocks of all inner blocks of a portal according to the passed rectangle.
	 */
	private static Set<Block> getInnerPortalBlocks(World world, AxisAlignedRect portalRect) {
		
		Set<Block> portalBlocks = new HashSet<>();
		
		BlockFace horizontalFace = portalRect.getAxis() == Axis.X ? BlockFace.EAST : BlockFace.SOUTH;
		Block iter = portalRect.getMin().toLocation(world).getBlock();
		
		for (int k = 0; k < portalRect.height(); k++) {
			Block iter2 = iter;
			
			for (int i = 0; i < portalRect.width(); i++) {
				
				portalBlocks.add(iter2);
				iter2 = iter2.getRelative(horizontalFace);
			}
			
			iter = iter.getRelative(BlockFace.UP);
		}
		
		return portalBlocks;
	}
	
	/**
	 * Returns a set of blocks where obsidian blocks need to be placed.
	 */
	private static Set<Block> getPortalFrameBlocks(World world, AxisAlignedRect portalRect) {
		
		Set<Block> frameBlocks = new HashSet<>();
		
		BlockFace horizontalFace = portalRect.getAxis() == Axis.X ? BlockFace.EAST : BlockFace.SOUTH;
		Block iter = portalRect.getMin().toLocation(world).getBlock();
		iter = iter.getRelative(BlockFace.DOWN).getRelative(horizontalFace.getOppositeFace());
		
		for (int k = -1; k <= portalRect.height(); k++) {
			
			Block iter2 = iter;
			
			for (int i = -1; i <= portalRect.width(); i++) {
				
				if(iter2.getType() == Material.OBSIDIAN)
					frameBlocks.add(iter2);
				
				iter2 = iter2.getRelative(horizontalFace);
			}
			
			iter = iter.getRelative(BlockFace.UP);
		}
		
		return frameBlocks;
	}
}