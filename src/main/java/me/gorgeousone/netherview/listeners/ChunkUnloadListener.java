package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.handlers.PlayerViewSession;
import me.gorgeousone.netherview.handlers.ViewHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkUnloadListener implements Listener {
	
	private final ViewHandler viewHandler;
	
	public ChunkUnloadListener(ViewHandler viewHandler) {
		this.viewHandler = viewHandler;
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
	
		for (PlayerViewSession session: viewHandler.getViewSessions()) {
		
			if (session.getViewedPortalSide().getChunks().contains(event.getChunk())) {
				event.setCancelled(true);
			}
		}
	}
}
