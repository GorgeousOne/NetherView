package me.gorgeousone.netherview.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.handlers.PacketHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class EntityVisibilityListener implements Listener {
	
	private final NetherViewPlugin main;
	private final ViewHandler viewHandler;
	private final PacketHandler packetHandler;
	private final ProtocolManager protocolManager;
	
	public EntityVisibilityListener(NetherViewPlugin main,
	                                ViewHandler viewHandler,
	                                PacketHandler packetHandler) {
		this.main = main;
		this.viewHandler = viewHandler;
		this.packetHandler = packetHandler;
		this.protocolManager = ProtocolLibrary.getProtocolManager();
		
		addUndeadEntitySpawnListener(protocolManager);
		addLivingEntitySpawnListener(protocolManager);
		addPaintingSpawnListener(protocolManager);
		addEntityDestroyListener(protocolManager);
	}


//	private void addEntityMoveInterception(ProtocolManager protocolManager) {
//
//		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.REL_ENTITY_MOVE) {
//			@Override
//			public void onPacketSending(PacketEvent event) {
//
//				PacketContainer packet = event.getPacket();
//			}
//		});
//	}
	
	private void addLivingEntitySpawnListener(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
			@Override
			public void onPacketSending(PacketEvent event) {
				registerNearbyEntity(event.getPacket(), event.getPlayer());
			}
		});
	}
	
	private void addUndeadEntitySpawnListener(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.SPAWN_ENTITY) {
			@Override
			public void onPacketSending(PacketEvent event) {
				registerNearbyEntity(event.getPacket(), event.getPlayer());
			}
		});
	}
	
	private void addPaintingSpawnListener(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.SPAWN_ENTITY_PAINTING) {
			@Override
			public void onPacketSending(PacketEvent event) {
				registerNearbyEntity(event.getPacket(), event.getPlayer());
			}
		});
	}
	
	private void addEntityDestroyListener(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_DESTROY) {
			@Override
			public void onPacketSending(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
				Player player = event.getPlayer();
				World world = player.getWorld();
				
				if (packetHandler.isCustomPacket(packet) ||
				    !main.canCreatePortalViews(world) ||
				    !player.hasPermission(NetherViewPlugin.VIEW_PERM)) {
					return;
				}
				
				for (int entityId : packet.getIntegerArrays().read(0)) {
					viewHandler.removeNearbyEntity(player, entityId);
				}
			}
		});
	}
	
	private void registerNearbyEntity(PacketContainer packet, Player player) {
		
		World world = player.getWorld();
		
		if (!main.canCreatePortalViews(world) ||
		    !player.hasPermission(NetherViewPlugin.VIEW_PERM)) {
			return;
		}
		
		Entity entity = protocolManager.getEntityFromID(world, packet.getIntegers().read(0));
		
		if (entity != null) {
			viewHandler.addNearbyEntity(player, entity);
		}
	}
}