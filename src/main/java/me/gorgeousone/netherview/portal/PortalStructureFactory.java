package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.threedstuff.PortalRectangle;
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
		
		PortalRectangle portalRect = locatePortalRect(sourceBlock);
		World world = sourceBlock.getWorld();
		
		return new PortalStructure(world, portalRect, getPortalBlocks(world, portalRect));
	}
	
	private static PortalRectangle locatePortalRect(Block sourceBlock) {
		
		Orientable portalData = (Orientable) sourceBlock.getBlockData();
		Axis portalAxis = portalData.getAxis();
		
		Vector min = new Vector(sourceBlock.getX(), getPortalExtent(sourceBlock, BlockFace.DOWN).getY(), sourceBlock.getZ());
		Vector max = new Vector(sourceBlock.getX(), getPortalExtent(sourceBlock, BlockFace.UP).getY(), sourceBlock.getZ());
		
		if (portalAxis == Axis.X) {
			min.setX(getPortalExtent(sourceBlock, BlockFace.WEST).getX());
			max.setX(getPortalExtent(sourceBlock, BlockFace.EAST).getX());
		}else {
			min.setZ(getPortalExtent(sourceBlock, BlockFace.NORTH).getZ());
			max.setZ(getPortalExtent(sourceBlock, BlockFace.SOUTH).getZ());
		}
		
		return new PortalRectangle(portalAxis, min, max);
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
	
	private static Set<Block> getPortalBlocks(World world, PortalRectangle portalRect) {
	
		Set<Block> portalBlocks = new HashSet<>();
		
		BlockFace horizontalFace = portalRect.getAxis() == Axis.X ? BlockFace.EAST : BlockFace.SOUTH;
		Block iter = portalRect.getMin().toLocation(world).getBlock();
		
		for(int k = 0; k <= portalRect.height(); k++) {
			
			Block iter2 = iter;
			
			for(int i = 0; i <= portalRect.width(); i++) {
				
				portalBlocks.add(iter2);
				iter2 = iter2.getRelative(horizontalFace);
			}
			
			iter = iter.getRelative(BlockFace.UP);
		}
		
		return portalBlocks;
	}
}
