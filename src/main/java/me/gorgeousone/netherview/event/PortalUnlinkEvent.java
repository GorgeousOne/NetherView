package me.gorgeousone.netherview.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.gorgeousone.netherview.portal.Portal;

public class PortalUnlinkEvent extends Event {
	
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Portal fromPortal;
	private final Portal toPortal;
	private final UnlinkReason reason;
	
	public PortalUnlinkEvent(Portal portal, Portal toPortal, UnlinkReason reason) {
		
		this.fromPortal = portal;
		this.toPortal = toPortal;
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
