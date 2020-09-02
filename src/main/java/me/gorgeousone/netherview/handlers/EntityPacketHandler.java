package me.gorgeousone.netherview.handlers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class EntityPacketHandler {
	
	private final ProtocolManager protocolManager;
	private final Set<Integer> customPacketIds;
	
	public EntityPacketHandler() {
		
		protocolManager = ProtocolLibrary.getProtocolManager();
		customPacketIds = new HashSet<>();
	}
	
	/**
	 * Returns true if the packets system ID matches any packet's ID sent by nether view for hiding entities behind a portal.
	 * The method will delete matching packets from the custom packet list, so this method only works once!
	 */
	public boolean isCustomPacket(PacketContainer packet) {
		
		int packetId = System.identityHashCode(packet.getHandle());
		
		if (customPacketIds.contains(packetId)) {
			customPacketIds.remove(packetId);
			return true;
		}
		
		return false;
	}
	
	public void hideEntities(Player player, Set<Entity> entities) {
		
		int[] entityIds = new int[entities.size()];
		int i = 0;
		
		for (Entity entity : entities) {
			entityIds[i] = entity.getEntityId();
			++i;
		}
		
		PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		destroyPacket.getIntegerArrays().write(0, entityIds);
		int packetId = System.identityHashCode(destroyPacket.getHandle());
		
		try {
			customPacketIds.add(packetId);
			protocolManager.sendServerPacket(player, destroyPacket);
			
		} catch (InvocationTargetException e) {
			
			customPacketIds.remove(packetId);
			throw new RuntimeException("Cannot send server packet.", e);
		}
	}
	
	public void showEntities(Player player, Set<Entity> visibleEntities) {
	
		
	}
}