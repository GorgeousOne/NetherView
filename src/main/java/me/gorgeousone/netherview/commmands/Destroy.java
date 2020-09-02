package me.gorgeousone.netherview.commmands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import me.gorgeousone.netherview.cmdframework.command.BasicCommand;
import me.gorgeousone.netherview.cmdframework.command.ParentCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class Destroy extends BasicCommand {
	
	private final ProtocolManager protocolManager;
	
	public Destroy(ParentCommand parent) {
	
		super("destroy", null, true, parent);
		protocolManager = ProtocolLibrary.getProtocolManager();
	}
	
	@Override
	protected void onCommand(CommandSender sender, String[] arguments) {
		
		Player player = (Player) sender;
		Collection<Entity> ents = player.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2);
		
		for (Entity entity : ents) {
			
			if (entity.getType() == EntityType.PLAYER) {
				continue;
			}
		}
		
//			PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
//
//			System.out.println("uid " + entity.getUniqueId());
//			System.out.println("eid " + entity.getEntityId());
//
//			destroy.getIntegerArrays().write(0, new int[] {entity.getEntityId()});
////			destroy.getIntegers().write(0, entity.getEntityId());
//			player.sendMessage(ChatColor.GRAY + "destroyed " + entity.getType().name().toLowerCase().replace("_", " ") + ".");
//
//			try {
//				protocolManager.sendServerPacket(player, destroy);
//			} catch (InvocationTargetException e) {
//				throw new RuntimeException("Cannot send server packet.", e);
//			}
//		}
	}
}