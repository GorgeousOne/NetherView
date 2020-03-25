package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.portal.PortalStructure;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Orientable;

public class PortalStructureFactory {
	
	public static PortalStructure findPortalStructure(Block block) {
		
		if (block.getType() != Material.NETHER_PORTAL)
			throw new IllegalArgumentException("Passed block is not part of a nether portal.");
		
		return new PortalStructure();
	}
	
	private static AxisAlignedRect getPortalArea(Block sourceBlock) {
		
		Orientable portalData = (Orientable) sourceBlock.getBlockData();
		Axis portalAxis = portalData.getAxis();
		
		int minY = getPortalExtent(sourceBlock, BlockFace.DOWN).getY();
		int maxY = getPortalExtent(sourceBlock, BlockFace.UP).getY();
		
		return new AxisAlignedRect();
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
}
