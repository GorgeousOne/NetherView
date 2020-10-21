package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.customportal.PlayerSelectionHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
	
	private final ViewHandler viewHandler;
	private final PlayerSelectionHandler selectionHandler;
	
	public PlayerQuitListener(ViewHandler viewHandler,
	                          PlayerSelectionHandler selectionHandler) {
		this.viewHandler = viewHandler;
		this.selectionHandler = selectionHandler;
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		
		Player player = event.getPlayer();
		viewHandler.unregisterPlayer(player);
		selectionHandler.removeSelection(player);
	}
}