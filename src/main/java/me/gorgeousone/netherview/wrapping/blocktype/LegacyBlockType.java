package me.gorgeousone.netherview.wrapping.blocktype;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gorgeousone.netherview.wrapping.rotation.RotationUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Mushroom;
import org.bukkit.material.Rails;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
			
			directional.setFacingDirection(RotationUtils.getRotatedFace(facing, quarterTurns));
			
		} else if (materialData instanceof Rails) {
			
			Rails rails = (Rails) materialData;
			BlockFace facing = rails.getDirection();
			rails.setDirection(RotationUtils.getRotatedFace(facing, quarterTurns), rails.isOnSlope());
			
		} else if (materialData instanceof Mushroom) {
			
			Mushroom mushroom = (Mushroom) materialData;
			Set<BlockFace> paintedFaces = new HashSet<>(mushroom.getPaintedFaces());
			
			for (BlockFace paintedFace : paintedFaces) {
				mushroom.setFacePainted(paintedFace, false);
			}
			
			for (BlockFace paintedFace : paintedFaces) {
				mushroom.setFacePainted(RotationUtils.getRotatedFace(paintedFace, quarterTurns), true);
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
		return OCCLUDING_TYPES.contains(materialData.getItemType().name());
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
	
	private final static List<String> OCCLUDING_TYPES = Arrays.asList(
			"STONE",
			"GRASS",
			"DIRT",
			"COBBLESTONE",
			"WOOD",
			"BEDROCK",
			"LAVA",
			"SAND",
			"GRAVEL",
			"GOLD_ORE",
			"IRON_ORE",
			"COAL_ORE",
			"LOG",
			"SPONGE",
			"LAPIS_ORE",
			"LAPIS_BLOCK",
			"DISPENSER",
			"SANDSTONE",
			"NOTE_BLOCK",
			"WOOL",
			"GOLD_BLOCK",
			"IRON_BLOCK",
			"DOUBLE_STEP",
			"BRICK",
			"BOOKSHELF",
			"MOSSY_COBBLESTONE",
			"OBSIDIAN",
			"MOB_SPAWNER",
			"DIAMOND_ORE",
			"DIAMOND_BLOCK",
			"WORKBENCH",
			"FURNACE",
			"BURNING_FURNACE",
			"REDSTONE_ORE",
			"GLOWING_REDSTONE_ORE",
			"SNOW_BLOCK",
			"CLAY",
			"JUKEBOX",
			"PUMPKIN",
			"NETHERRACK",
			"SOUL_SAND",
			"GLOWSTONE",
			"JACK_O_LANTERN",
			"MONSTER_EGGS",
			"SMOOTH_BRICK",
			"HUGE_MUSHROOM_1",
			"HUGE_MUSHROOM_2",
			"MELON_BLOCK",
			"MYCEL",
			"NETHER_BRICK",
			"ENDER_STONE",
			"REDSTONE_LAMP_OFF",
			"REDSTONE_LAMP_ON",
			"WOOD_DOUBLE_STEP",
			"EMERALD_ORE",
			"EMERALD_BLOCK",
			"COMMAND",
			"QUARTZ_ORE",
			"QUARTZ_BLOCK",
			"DROPPER",
			"STAINED_CLAY",
			"LOG_2",
			"SLIME_BLOCK",
			"BARRIER",
			"PRISMARINE",
			"SEA_LANTERN",
			"HAY_BLOCK",
			"HARD_CLAY",
			"COAL_BLOCK",
			"PACKED_ICE",
			"RED_SANDSTONE",
			"DOUBLE_STONE_SLAB2",
			"PURPUR_BLOCK",
			"PURPUR_PILLAR",
			"PURPUR_DOUBLE_SLAB",
			"END_BRICKS",
			"COMMAND_REPEATING",
			"COMMAND_CHAIN",
			"MAGMA",
			"NETHER_WART_BLOCK",
			"RED_NETHER_BRICK",
			"BONE_BLOCK",
			"WHITE_GLAZED_TERRACOTTA",
			"ORANGE_GLAZED_TERRACOTTA",
			"MAGENTA_GLAZED_TERRACOTTA",
			"LIGHT_BLUE_GLAZED_TERRACOTTA",
			"YELLOW_GLAZED_TERRACOTTA",
			"LIME_GLAZED_TERRACOTTA",
			"PINK_GLAZED_TERRACOTTA",
			"GRAY_GLAZED_TERRACOTTA",
			"SILVER_GLAZED_TERRACOTTA",
			"CYAN_GLAZED_TERRACOTTA",
			"PURPLE_GLAZED_TERRACOTTA",
			"BLUE_GLAZED_TERRACOTTA",
			"BROWN_GLAZED_TERRACOTTA",
			"GREEN_GLAZED_TERRACOTTA",
			"RED_GLAZED_TERRACOTTA",
			"BLACK_GLAZED_TERRACOTTA",
			"CONCRETE",
			"CONCRETE_POWDER",
			"STRUCTURE_BLOCK"
	);
}