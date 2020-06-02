package me.gorgeousone.netherview.blocktype;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

public abstract class BlockType {
	
	private static boolean isLegacyServer;
	
	/**
	 * Sets whether BlockType.of() will create a LegacyBlockType or an AquaticBlockType
	 */
	public static void configureVersion(boolean isLegacyServer) {
		BlockType.isLegacyServer = isLegacyServer;
	}
	
	public static BlockType of(Block block) {
		return isLegacyServer ? new LegacyBlockType(block) : new AquaticBlockType(block);
	}
	
	public static BlockType of(Material material) {
		return isLegacyServer ? new LegacyBlockType(material) : new AquaticBlockType(material);
	}
	
	public static BlockType of(BlockState state) {
		return isLegacyServer ? new LegacyBlockType(state) : new AquaticBlockType(state);
	}
	
	/**
	 * Tries to match a material with the given material name string. Will return the legacy alternative if server version below 1.13
	 */
	public static BlockType match(String materialName, String alternativeMat, byte data) {
		
		if (isLegacyServer) {
			return new LegacyBlockType(new MaterialData(Material.valueOf(alternativeMat), data));
		} else {
			return BlockType.of(Material.matchMaterial(materialName));
		}
	}
	
	/**
	 * Rotates the BlockType if it is rotatable in the xz plane in any way
	 * @param quarterTurns count of 90° turns performed (between 0 and 3)
	 */
	public abstract BlockType rotate(int quarterTurns);
	
	public abstract WrappedBlockData getWrapped();
	
	public abstract boolean isOccluding();
	
	public abstract BlockType clone();
}
