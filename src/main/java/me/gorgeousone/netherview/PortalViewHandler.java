package me.gorgeousone.netherview;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Set;

public class PortalViewHandler {
	
	
	public void displayPortal(Player player, PortalStructure portal) {
	
		ViewCone playerViewCone = calculateViewThroughPortal(player.getEyeLocation().toVector(), portal.getPortalRect());
		
		BlockCache netherBlocks = getNetherBlocks(portal);
		
		Set<Block> blocksInCone = detectBlocksInCone(playerViewCone, netherBlocks);
		
		renderBlocks(player, blocksInCone);
	}
	
	private ViewCone calculateViewThroughPortal(Vector viewPoint, AxisAlignedRect nearPlane) {
		return null;
	}
	
	private BlockCache getNetherBlocks(PortalStructure portal) {
		return null;
	}
	
	private Set<Block> detectBlocksInCone(ViewCone playerViewCone, BlockCache netherBlocks) {
		return null;
	}
	
	private void renderBlocks(Player player, Set<Block> blocksInCone) {
	}
}
