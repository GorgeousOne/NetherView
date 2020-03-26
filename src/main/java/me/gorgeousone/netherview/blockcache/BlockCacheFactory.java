package me.gorgeousone.netherview.blockcache;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

public class BlockCacheFactory {
	
	public static BlockCache createBlockCache(int x1, int y1, int z1, int x2, int y2, int z2, World world) {
		
		if (x2 <= x1 || y2 <= y1 || z2 <= z1)
			throw new IllegalArgumentException("Cannot create a BlockCache of such a small area");
		
		BlockState[][][] blocks = new BlockState[x2 - x1 + 1][y2 - y1 + 1][z2 - z1 + 1];
		
		for(int x = x1; x <= x2; x++) {
			for(int y = y1; y <= y2; y++) {
				for(int z = z1; z <= z2; z++) {
					
					Block block = new Location(world, x, y, z).getBlock();
					blocks[x-x1][y-y1][z-z1] = block.getState();
				}
			}
		}
		
		return new BlockCache(blocks, new Vector(x1, y1, z1));
	}
}
