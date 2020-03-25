package me.gorgeousone.netherview;

import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;

import java.util.HashSet;
import java.util.Set;

public class PortalDetector {
	
	public static PortalStructure findPortalStructure(Block block) {
		
		if(block.getType() != Material.NETHER_PORTAL)
			throw new IllegalArgumentException("Passed block is not part of a nether portal.");
		
		Orientable portalData = (Orientable) block.getBlockData();
		Axis portalAxis = portalData.getAxis();
		
		return new PortalStructure();
	}
	
	private static AxisAlignedRect getPortalArea(Block firstBlock, Axis portalAxis) {
		
		
		Set<Block> portalBlocks = new HashSet<>();
		
		
		return new AxisAlignedRect();
	}
}
