package me.gorgeousone.netherview.api;

import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PortalUnlinkEvent extends Event {
	
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Portal fromPortal;
	private final Portal toPortal;
	private final UnlinkReason reason;
	
	public PortalUnlinkEvent(Portal portal, Portal linkedPortal, UnlinkReason reason) {
		
		this.fromPortal = portal;
		this.toPortal = linkedPortal;
		this.reason = reason;
	}
	
	public Portal getFromPortal() {
		return fromPortal;
	}
	
	public Portal getToPortal() {
		return toPortal;
	}
	
	public UnlinkReason getReason() {
		return reason;
	}
	
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
