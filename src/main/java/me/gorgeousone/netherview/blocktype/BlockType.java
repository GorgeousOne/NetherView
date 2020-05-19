package me.gorgeousone.netherview.blocktype;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

public abstract class BlockType {
	
	private static boolean isLegacyServer;
	
	public static void configureVersion(String versionString) {
	
		isLegacyServer =
				versionString.contains("1.8") ||
			    versionString.contains("1.9") ||
			    versionString.contains("1.10") ||
			    versionString.contains("1.11") ||
			    versionString.contains("1.12");
		
		System.out.println("is legacy " + isLegacyServer);
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
	
	public static BlockType match(String materialName, MaterialData alternative) {
		
		Material material = Material.matchMaterial(materialName);
		
		if(material == null)
			return new LegacyBlockType(alternative);
		else
			return BlockType.of(material);
	}
	
	public abstract BlockType rotate(int quarterTurns);
	public abstract WrappedBlockData getWrapped();
	
	public abstract boolean isOccluding();
	public abstract BlockType clone();
}
