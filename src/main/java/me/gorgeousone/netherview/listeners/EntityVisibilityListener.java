package me.gorgeousone.netherview.listeners;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.handlers.ViewHandler;

public class EntityVisibilityListener {
	
	private final NetherViewPlugin main;
	private final ViewHandler viewHandler;
	private final ProtocolManager protocolManager;
	
	public EntityVisibilityListener(NetherViewPlugin main,
	                                ViewHandler viewHandler) {
		this.main = main;
		this.viewHandler = viewHandler;
		this.protocolManager = ProtocolLibrary.getProtocolManager();

	}

//	private void addEntityMoveInterception() {
//
//		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_EQUIPMENT) {
//			@Override
//			public void onPacketSending(PacketEvent event) {
//
//				PacketContainer packet = event.getPacket();
//
//				if (VersionUtils.serverIsAtOrAbove("1.16.0")) {
//					packet.getSlotStackPairLists().read(0).forEach(pair -> event.getPlayer().sendMessage(pair.getFirst() + " with " + pair.getSecond()));
//
//				} else if (VersionUtils.serverIsAtOrAbove("1.9.0")) {
//
//				}else {
//					event.getPlayer().sendMessage("slot " + packet.getIntegers().read(1) + " - " + packet.getItemModifier().read(0));
//				}
//			}
//		});
//	}
//
}