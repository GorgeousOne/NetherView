package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.handlers.ViewHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
	
	private ViewHandler viewHandler;
	
	public PlayerQuitListener(ViewHandler viewHandler) {
		this.viewHandler = viewHandler;
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		
		Player player = event.getPlayer();
		
		if (player.hasPermission(NetherView.VIEW_PERM)) {
			viewHandler.removeVieSession(player);
		}
	}
}