package me.gorgeousone.netherview.blocktype;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

public class LegacyBlockType extends BlockType {
	
	private MaterialData materialData;
	
	public LegacyBlockType(Material material) {
		materialData = new MaterialData(material);
	}
	
	public LegacyBlockType(Block block) {
		materialData = block.getState().getData().clone();
	}
	
	public LegacyBlockType(BlockState state) {
		materialData = state.getData().clone();
	}
	
	public LegacyBlockType(MaterialData data) {
		materialData = data.clone();
	}
	
	@Override
	public BlockType rotate(int quarterTurns) {
		
		if(quarterTurns == 0)
			return this;
		
		if(materialData instanceof Directional) {
			
			Directional directional = (Directional) materialData;
			BlockFace facing = directional.getFacing();
			
			if(RotationUtils.isRotatableFace(facing))
				directional.setFacingDirection(RotationUtils.getRotatedFace(directional.getFacing(), quarterTurns));
		}
		
		//TODO find out if other blocks need to be rotated with their byte data
		return this;
	}
	
	@Override
	public WrappedBlockData getWrapped() {
		return WrappedBlockData.createData(materialData.getItemType(), materialData.getData());
	}
	
	@Override
	public boolean isOccluding() {
		return false;
	}
	
	@Override
	public LegacyBlockType clone() {
		return new LegacyBlockType(materialData.clone());
	}
}
