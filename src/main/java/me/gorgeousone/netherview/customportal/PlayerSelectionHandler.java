package me.gorgeousone.netherview.customportal;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSelectionHandler {
	
	private final Map<UUID, PlayerCuboidSelection> cuboidSelections;
	
	public PlayerSelectionHandler() {
		this.cuboidSelections = new HashMap<>();
	}
	
	public boolean hasCuboidSelection(Player player) {
		return cuboidSelections.containsKey(player.getUniqueId());
	}
	
	public PlayerCuboidSelection getSelection(Player player) {
		return cuboidSelections.get(player.getUniqueId());
	}
	
	public PlayerCuboidSelection getOrCreateCuboidSelection(Player player) {
		
		UUID playerId = player.getUniqueId();
		PlayerCuboidSelection selection = cuboidSelections.get(playerId);
		
		if (selection == null || selection.getWorld() != player.getWorld()) {
			selection = new PlayerCuboidSelection(player);
			cuboidSelections.put(playerId, selection);
		}
		
		return selection;
	}
}