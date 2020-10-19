package me.gorgeousone.netherview.event;

import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PortalLinkEvent extends Event implements Cancellable {
	
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private boolean isCancelled;
	
	private final Player player;
	private final Portal fromPortal;
	private final Portal toPortal;
	
	public PortalLinkEvent(Portal fromPortal, Portal toPortal, Player player) {
		
		this.player = player;
		this.fromPortal = fromPortal;
		this.toPortal = toPortal;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public Portal getFromPortal() {
		return fromPortal;
	}
	
	public Portal getToPortal() {
		return toPortal;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}
	
	@Override
	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
	
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
