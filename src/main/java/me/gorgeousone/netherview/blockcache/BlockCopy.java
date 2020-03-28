package me.gorgeousone.netherview.blockcache;

import org.bukkit.util.Vector;
import org.bukkit.block.data.BlockData;

public class BlockCopy {

	private Vector position;
	private BlockData blockData;
	
	public BlockCopy(Vector position, BlockData blockData) {
		this.position = position;
		this.blockData = blockData;
	}
	
	public Vector getPosition() {
		return position;
	}
	
	public BlockData getBlockData() {
		return blockData;
	}
}
