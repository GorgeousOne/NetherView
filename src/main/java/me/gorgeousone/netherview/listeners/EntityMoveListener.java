package me.gorgeousone.netherview.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.gorgeousone.netherview.NetherViewPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class EntityMoveListener {
	
	private final NetherViewPlugin main;
	
	public EntityMoveListener(NetherViewPlugin main) {
		
		this.main = main;
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		addEntityMoveInterception(protocolManager);
	}
	
	long stamp = -1;
	
	private void addEntityMoveInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.REL_ENTITY_MOVE) {
			@Override
			public void onPacketSending(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
				
				double dx = packet.getShorts().read(0) / 4096d;
				double dy = packet.getShorts().read(1) / 4096d;
				double dz = packet.getShorts().read(2) / 4096d;
				
				double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
				event.getPlayer().sendMessage("" + (int) (100 * dist) / 100d);
				
				if (stamp != -1) {
					event.getPlayer().sendMessage(" dt " + (System.currentTimeMillis() - stamp));
					event.getPlayer().sendMessage(ChatColor.GRAY + "-----------");
				}
				
				stamp = System.currentTimeMillis();
			}
		});
	}
}
