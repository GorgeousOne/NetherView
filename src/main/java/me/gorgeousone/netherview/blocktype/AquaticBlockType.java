package me.gorgeousone.netherview.blocktype;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gorgeousone.netherview.blocktype.rotation.AquaticRailUtils;
import me.gorgeousone.netherview.blocktype.rotation.RotationUtils;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.RedstoneWire;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AquaticBlockType extends BlockType {
	
	private BlockData blockData;
	
	public AquaticBlockType(Material material) {
		blockData = material.createBlockData();
	}
	
	public AquaticBlockType(Block block) {
		blockData = block.getBlockData().clone();
	}
	
	public AquaticBlockType(BlockState state) {
		blockData = state.getBlockData().clone();
	}
	
	public AquaticBlockType(BlockData data) {
		blockData = data.clone();
	}
	
	public AquaticBlockType(String serialized) {
		blockData = Material.valueOf(serialized.toUpperCase()).createBlockData();
	}
	
	@Override
	public BlockType rotate(int quarterTurns) {
		
		if (quarterTurns == 0) {
			return this;
		}
		
		//e.g. logs
		if (blockData instanceof Orientable) {
			
			if (quarterTurns % 2 == 0) {
				return this;
			}
			
			Orientable orientable = (Orientable) blockData;
			
			if (orientable.getAxis() != Axis.Y) {
				orientable.setAxis(orientable.getAxis() == Axis.X ? Axis.Z : Axis.X);
			}
			
			//e.g. furnaces, hoppers
		} else if (blockData instanceof Directional) {
			
			Directional directional = (Directional) blockData;
			directional.setFacing(RotationUtils.getRotatedFace(directional.getFacing(), quarterTurns));
			
			//e.g. signs
		} else if (blockData instanceof Rotatable) {
			
			Rotatable rotatable = (Rotatable) blockData;
			rotatable.setRotation(RotationUtils.getRotatedFace(rotatable.getRotation(), quarterTurns));
			
			//e.g. fences
		} else if (blockData instanceof MultipleFacing) {
			
			MultipleFacing multiFacing = (MultipleFacing) blockData;
			Set<BlockFace> facings = new HashSet<>(multiFacing.getFaces());
			
			for (BlockFace face : multiFacing.getAllowedFaces()) {
				multiFacing.setFace(face, false);
			}
			
			for (BlockFace face : facings) {
				multiFacing.setFace(RotationUtils.getRotatedFace(face, quarterTurns), true);
			}
			
		} else if (blockData instanceof RedstoneWire) {
			
			RedstoneWire wire = (RedstoneWire) blockData;
			Map<BlockFace, RedstoneWire.Connection> connections = new HashMap<>();
			
			for (BlockFace face : wire.getAllowedFaces()) {
				connections.put(face, wire.getFace(face));
			}
			
			for (BlockFace face : connections.keySet())
				wire.setFace(RotationUtils.getRotatedFace(face, quarterTurns), connections.get(face));
			
		} else if (blockData instanceof Rail) {
			
			Rail rail = (Rail) blockData;
			rail.setShape(AquaticRailUtils.getRotatedRail(rail.getShape(), quarterTurns));
		}
		
		return this;
	}
	
	@Override
	public WrappedBlockData getWrapped() {
		return WrappedBlockData.createData(blockData);
	}
	
	@Override
	public boolean isOccluding() {
		return blockData.getMaterial().isOccluding();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(blockData);
	}
	
	@Override
	public AquaticBlockType clone() {
		return new AquaticBlockType(blockData);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AquaticBlockType)) {
			return false;
		}
		AquaticBlockType blockType = (AquaticBlockType) o;
		return blockData.equals(blockType.blockData);
	}
}