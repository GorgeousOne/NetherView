package me.gorgeousone.netherview.wrapper.blocktype;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gorgeousone.netherview.utils.FacingUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Mushroom;
import org.bukkit.material.Rails;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A wrapper for material data used before the aquatic update (1.12 and before)
 */
@SuppressWarnings("deprecation")
public class LegacyBlockType extends BlockType {
	
	private final MaterialData materialData;
	
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
	
	public LegacyBlockType(String serialized) {
		
		Material material;
		byte data = 0;
		
		if (serialized.contains(":")) {
			
			String[] fullData = serialized.split(":");
			
			material = Material.valueOf(fullData[0].toUpperCase());
			data = Byte.parseByte(fullData[1]);
			
		} else {
			material = Material.valueOf(serialized.toUpperCase());
		}
		
		materialData = new MaterialData(material, data);
	}
	
	@Override
	public BlockType rotate(int quarterTurns) {
		
		if (quarterTurns == 0) {
			return this;
		}
		
		if (materialData instanceof Directional) {
			
			Directional directional = (Directional) materialData;
			BlockFace facing = directional.getFacing();
			
			//somehow the facing of stairs is always reversed, nothing else, just    stairs
			if (materialData.getItemType().name().contains("STAIRS")) {
				facing = facing.getOppositeFace();
			}
			
			directional.setFacingDirection(FacingUtils.getRotatedFace(facing, quarterTurns));
			
		} else if (materialData instanceof Rails) {
			
			Rails rails = (Rails) materialData;
			BlockFace facing = rails.getDirection();
			rails.setDirection(FacingUtils.getRotatedFace(facing, quarterTurns), rails.isOnSlope());
			
		} else if (materialData instanceof Mushroom) {
			
			Mushroom mushroom = (Mushroom) materialData;
			Set<BlockFace> paintedFaces = new HashSet<>(mushroom.getPaintedFaces());
			
			for (BlockFace paintedFace : paintedFaces) {
				mushroom.setFacePainted(paintedFace, false);
			}
			
			for (BlockFace paintedFace : paintedFaces) {
				mushroom.setFacePainted(FacingUtils.getRotatedFace(paintedFace, quarterTurns), true);
			}
		}
		return this;
	}
	
	@Override
	public WrappedBlockData getWrapped() {
		return WrappedBlockData.createData(materialData.getItemType(), materialData.getData());
	}
	
	@Override
	public boolean isOccluding() {
		return materialData.getItemType().isOccluding();
	}
	
	@Override
	public LegacyBlockType clone() {
		return new LegacyBlockType(materialData.clone());
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof LegacyBlockType)) {
			return false;
		}
		LegacyBlockType blockType = (LegacyBlockType) o;
		return materialData.equals(blockType.materialData);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(materialData);
	}
	
	@Override
	public String toString() {
		return "LegacyBlock{" + materialData.toString() + '}';
	}
}