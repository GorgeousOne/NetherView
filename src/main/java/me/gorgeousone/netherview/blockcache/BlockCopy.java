package me.gorgeousone.netherview.blockcache;

import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

public class BlockCopy {
	
	private BlockVec position;
	private BlockData blockData;
	
	public BlockCopy(Block block) {
		this.position = new BlockVec(block);
		this.blockData = block.getBlockData();
	}
	
	public BlockCopy(BlockVec position, BlockData blockData) {
		this.position = position;
		this.blockData = blockData;
	}
	
	public BlockVec getPosition() {
		return position.clone();
	}
	
	public BlockData getBlockData() {
		return blockData.clone();
	}
	
	public BlockCopy setPosition(BlockVec position) {
		this.position = position.clone();
		return this;
	}
	
	public BlockCopy setData(BlockData blockData) {
		this.blockData = blockData.clone();
		return this;
	}
	
	public Block getBlock(World world) {
		return  world.getBlockAt(position.getX(), position.getY(), position.getZ());
	}
	
	@Override
	public BlockCopy clone() {
		return new BlockCopy(getPosition(), getBlockData());
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BlockCopy)) return false;
		BlockCopy copy = (BlockCopy) o;
		return position.equals(copy.position);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(position);
	}
}