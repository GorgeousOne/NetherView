package me.gorgeousone.netherview.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.PacketConstructor;
import me.gorgeousone.netherview.utils.NmsUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import org.bukkit.entity.Entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class EntitySpawnPacketFactory1_13 {
	
	private EntitySpawnPacketFactory1_13() {}
	
	private static Constructor<?> CONSTRUCTOR_TRACKER_ENTRY;
	private static Method TRACKER_ENTRY_CREATE_PACKET;
	
	private static Field NMS_SPAWN_PACKET_ENTITY_ID;
	private static Field NMS_SPAWN_PACKET_ENTITY_DATA;
	
	private static PacketConstructor CONSTRUCTOR_SPAWN_PACKET;
	
	static {
		
		if (!VersionUtils.serverIsAtOrAbove("1.14")) {
			
			try {
				
				Class<?> trackerEntryClass = NmsUtils.getNmsClass("EntityTrackerEntry");
				CONSTRUCTOR_TRACKER_ENTRY = trackerEntryClass.getConstructor(NmsUtils.getNmsClass("Entity"), int.class, int.class, int.class, boolean.class);
				TRACKER_ENTRY_CREATE_PACKET = trackerEntryClass.getDeclaredMethod("e");
				
				boolean serverIs1_8 = !VersionUtils.serverIsAtOrAbove("1.9");
				
				Class<?> entitySpawnPacket = NmsUtils.getNmsClass("PacketPlayOutSpawnEntity");
				NMS_SPAWN_PACKET_ENTITY_ID = entitySpawnPacket.getDeclaredField(serverIs1_8 ? "j" : "k");
				NMS_SPAWN_PACKET_ENTITY_DATA = entitySpawnPacket.getDeclaredField(serverIs1_8 ? "k" : "l");
				
				CONSTRUCTOR_SPAWN_PACKET = ProtocolLibrary.getProtocolManager().createPacketConstructor(PacketType.Play.Server.SPAWN_ENTITY, NmsUtils.getNmsClass("Entity"), int.class, int.class);
				
			} catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Creates a spawn packet container for <1.14 non living entities.
	 * It first creates a nms spawn packet with a method from EntityTrackerEntry in order to
	 * read entity object data values from it (which are difficult to replicate by hand).
	 */
	public static PacketContainer createPacket(Entity entity) {
		
		try {
			TRACKER_ENTRY_CREATE_PACKET.setAccessible(true);
			NMS_SPAWN_PACKET_ENTITY_ID.setAccessible(true);
			NMS_SPAWN_PACKET_ENTITY_DATA.setAccessible(true);
			
			Object trackerEntry = CONSTRUCTOR_TRACKER_ENTRY.newInstance(NmsUtils.getHandle(entity), 0, 0, 0, false);
			Object nmsSpawnPacket = TRACKER_ENTRY_CREATE_PACKET.invoke(trackerEntry);
			
			int entityId = NMS_SPAWN_PACKET_ENTITY_ID.getInt(nmsSpawnPacket);
			int entityData = NMS_SPAWN_PACKET_ENTITY_DATA.getInt(nmsSpawnPacket);
			
			TRACKER_ENTRY_CREATE_PACKET.setAccessible(false);
			NMS_SPAWN_PACKET_ENTITY_ID.setAccessible(false);
			NMS_SPAWN_PACKET_ENTITY_DATA.setAccessible(false);
			
			return CONSTRUCTOR_SPAWN_PACKET.createPacket(NmsUtils.getHandle(entity), entityId, entityData);
			
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			
			e.printStackTrace();
			return null;
		}
	}
}

