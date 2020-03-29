package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Orientable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PortalStructureFactory {
	
	public static PortalStructure locatePortalStructure(Block sourceBlock) {
		
		if (sourceBlock.getType() != Material.NETHER_PORTAL)
			throw new IllegalArgumentException("Passed block is not part of a nether portal: world: " + sourceBlock.getWorld().getEnvironment().name().toLowerCase() + " x: " + sourceBlock.getX() + " y: " + sourceBlock.getY() + " z: " + sourceBlock.getZ());
		
		AxisAlignedRect portalRect = locatePortalRect(sourceBlock);
		World world = sourceBlock.getWorld();
		
		return new PortalStructure(world, portalRect, getPortalBlocks(world, portalRect));
	}
	
	private static AxisAlignedRect locatePortalRect(Block sourceBlock) {
		
		Orientable portalData = (Orientable) sourceBlock.getBlockData();
		Axis portalAxis = portalData.getAxis();
		
		Vector location = new Vector(sourceBlock.getX(), getPortalExtent(sourceBlock, BlockFace.DOWN).getY(), sourceBlock.getZ());
		
		int height = getPortalExtent(sourceBlock, BlockFace.UP).getY() + 1 - location.getBlockY();
		int width;
		
		if (portalAxis == Axis.X) {
			location.setX(getPortalExtent(sourceBlock, BlockFace.WEST).getX());
			width = getPortalExtent(sourceBlock, BlockFace.EAST).getX() + 1 - location.getBlockX();
		} else {
			location.setZ(getPortalExtent(sourceBlock, BlockFace.NORTH).getZ());
			width = getPortalExtent(sourceBlock, BlockFace.SOUTH).getZ() + 1 - location.getBlockZ();
		}
		
		return new AxisAlignedRect(portalAxis, location, width, height);
	}
	
	private static Block getPortalExtent(Block sourceBlock, BlockFace facing) {
		
		Block blockIterator = sourceBlock;
		
		for (int i = 0; i < 22; i++) {
			
			Block nextBlock = blockIterator.getRelative(facing);
			
			if (nextBlock.getType() != Material.NETHER_PORTAL)
				return blockIterator;
			
			blockIterator = nextBlock;
		}
		
		throw new IllegalArgumentException("Portal appears to be bigger than vanilla allows.");
	}
	
	private static Set<Block> getPortalBlocks(World world, AxisAlignedRect portalRect) {
		
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
}
