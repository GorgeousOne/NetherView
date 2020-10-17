package me.gorgeousone.netherview.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public final class TeleportUtils {
	
	private TeleportUtils() {}
	
	private static Field FIELD_PLAYER_ABILITIES;
	private static Field FIELD_IS_INVULNERABLE;
	
	static {
		
		try {
			FIELD_PLAYER_ABILITIES = NmsUtils.getNmsClass("EntityPlayer").getField("abilities");
			FIELD_IS_INVULNERABLE = NmsUtils.getNmsClass("PlayerAbilities").getField("isInvulnerable");
			
		} catch (ClassNotFoundException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}
	
	public static void setTemporarilyInvulnerable(Player player, JavaPlugin plugin, long duration) {
		
		try {
			Object nmsPlayer = NmsUtils.getHandle(player);
			Object playerAbilities = FIELD_PLAYER_ABILITIES.get(nmsPlayer);
			FIELD_IS_INVULNERABLE.setBoolean(playerAbilities, true);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					
					try {
						FIELD_IS_INVULNERABLE.setBoolean(playerAbilities, false);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}.runTaskLater(plugin, duration);
			
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}