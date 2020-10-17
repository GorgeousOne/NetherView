package me.gorgeousone.netherview.utils;

import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;

public final class ObjectData {
	
	private ObjectData() {}
	
	private static final Map<EntityType, Integer> minecartData = new HashMap<>();
	private static final Map<BlockFace, Integer> itemFrameData = new HashMap<>();
	
	static {
		minecartData.put(EntityType.MINECART, 00);
		minecartData.put(EntityType.MINECART_TNT, 0);
		minecartData.put(EntityType.MINECART_CHEST, 0);
		minecartData.put(EntityType.MINECART_COMMAND, 0);
		minecartData.put(EntityType.MINECART_FURNACE, 0);
		minecartData.put(EntityType.MINECART_HOPPER, 0);
		minecartData.put(EntityType.MINECART_MOB_SPAWNER, 0);
		
		itemFrameData.put(BlockFace.NORTH, 2);
		itemFrameData.put(BlockFace.SOUTH, 3);
		itemFrameData.put(BlockFace.WEST, 4);
		itemFrameData.put(BlockFace.EAST, 5);
	}
	
	/**
	 * Returns the object data that theoretically is needed to create a spawn packet for not living entities until 1.13 according to ProtocolLib:
	 * https://wiki.vg/Object_Data
	 * But it doesn't work anyway.
	 */
	public static int getObjectData(Entity entity) {
		
		EntityType type = entity.getType();
		
		switch (type) {
			
			case DROPPED_ITEM:
				return 1;
			
			case ITEM_FRAME:
				return itemFrameData.get(((ItemFrame) entity).getAttachedFace());
			
			case FISHING_HOOK:
				return getShooterId(entity);
			
			default:
				
				if (type == EntityType.ARROW || type == EntityType.valueOf("SPECTRAL_ARROW")) {
					return getShooterId(entity) + 1;
				}
				
				return 0;
		}
	}
	
	private static int getShooterId(Entity projectile) {
		
		ProjectileSource shooter = ((Arrow) projectile).getShooter();
		
		if (shooter instanceof Entity) {
			return ((Entity) shooter).getEntityId();
		}
		
		return 0;
	}
}
