package me.gorgeousone.netherview.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.handlers.EntityPacketHandler;
import org.bukkit.ChatColor;

public class EntityMoveListener {
	
	private final NetherViewPlugin main;
	private final EntityPacketHandler entityPacketHandler;
	
	public EntityMoveListener(NetherViewPlugin main, EntityPacketHandler packetHandler) {
		
		this.main = main;
		this.entityPacketHandler = packetHandler;
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		addEntityMoveInterception(protocolManager);
		addEntityInterception(protocolManager);
//		addEntitySpawnInterception(protocolManager);
		addPaintingInterception(protocolManager);
		addPaintingDeathInterception(protocolManager);
	}
	
	private void addEntityMoveInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.REL_ENTITY_MOVE) {
			@Override
			public void onPacketSending(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
			}
		});
	}
	
	private void addEntitySpawnInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
			@Override
			public void onPacketSending(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
				
				if (entityPacketHandler.isCustomPacket(packet)) {
					return;
				}
				event.getPlayer().sendMessage("IT'S ALIVE!!! " + packet.getIntegers().read(0));
			}
		});
	}
	
	private void addEntityInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.SPAWN_ENTITY) {
			@Override
			public void onPacketSending(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
				
				if (entityPacketHandler.isCustomPacket(packet)) {
					return;
				}
				event.getPlayer().sendMessage("spawn dead " + packet.getIntegers().read(0));
			}
		});
	}
	
	private void addPaintingInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.SPAWN_ENTITY_PAINTING) {
			@Override
			public void onPacketSending(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
				
				if (entityPacketHandler.isCustomPacket(packet)) {
					return;
				}
				
				event.getPlayer().sendMessage("spawned french girl " + packet.getIntegers().read(0));
			}
		});
	}
	private void addPaintingDeathInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_DESTROY) {
			@Override
			public void onPacketSending(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
				
				if (entityPacketHandler.isCustomPacket(packet)) {
					return;
				}
				
				event.getPlayer().sendMessage(ChatColor.GRAY + "removed whomever " + packet.getIntegerArrays().read(0)[0]);
			}
		});
	}
}
