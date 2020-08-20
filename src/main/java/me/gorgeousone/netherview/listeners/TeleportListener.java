package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.api.PortalUnlinkEvent;
import me.gorgeousone.netherview.api.UnlinkReason;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Takes care of registering and linking nether portals when players use them.
 */
public class TeleportListener implements Listener {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	private final ViewHandler viewHandler;
	
	public TeleportListener(NetherViewPlugin main,
	                        PortalHandler portalHandler,
	                        ViewHandler viewHandler) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
	}
	
	//I did not use the PlayerPortalEvent because it only give information about where the player should theoretically perfectly teleport to
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event) {
		
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		if (to == null) {
			return;
		}
		
		//updates portal animation for the player if they teleport with e.g. an ender pearl
		if (from.getWorld() == to.getWorld() && player.hasPermission(NetherViewPlugin.VIEW_PERM)) {
			viewHandler.displayClosestPortalTo(player, to.clone().add(0, player.getEyeHeight(), 0));
			return;
		}
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL ||
		    !player.hasPermission(NetherViewPlugin.LINK_PERM)) {
			return;
		}
		
		boolean createdNewPortalView = createPortalView(event);
		
		if (createdNewPortalView && viewHandler.hasPortalViewEnabled(player) &&
		    (player.getGameMode() == GameMode.CREATIVE || main.cancelTeleportWhenLinkingPortalsEnabled())) {
			event.setCancelled(true);
		}
	}
	
	/**
	 * Tries to locates or register portals at the given locations and to link them if they aren't already.
	 */
	private boolean createPortalView(PlayerTeleportEvent event) {
		
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		if (!main.canCreatePortalViews(from.getWorld())) {
			MessageUtils.printDebug("World '" + from.getWorld().getName() + "' not listed in config for portal viewing");
			return false;
		}
		
		Block portalBlock = PortalLocator.getNearbyPortalBlock(from);
		
		//might happen if the player mysteriously moved more than a block away from the portal in split seconds
		if (portalBlock == null) {
			MessageUtils.printDebug("No portal found to teleport from at location [" + from.getWorld().getName() + "," + new BlockVec(from).toString() + "]");
			return false;
		}
		
		Block counterPortalBlock = PortalLocator.getNearbyPortalBlock(to);
		
		if (counterPortalBlock == null) {
			MessageUtils.printDebug("No portal found to teleport to at location [" + to.getWorld().getName() + "," + new BlockVec(to).toString() + "]");
			return false;
		}
		
		try {
			Portal portal = portalHandler.getPortalByBlock(portalBlock);
			Portal counterPortal = portalHandler.getPortalByBlock(counterPortalBlock);
			
			if (portal.getCounterPortal() == counterPortal) {
				return false;
			}
			
			if (portal.isLinked()) {
				
				Bukkit.getPluginManager().callEvent(new PortalUnlinkEvent(portal, portal.getCounterPortal(), UnlinkReason.SWITCHED_TARGET_PORTAL));
				portal.removeLink();
				
				portalHandler.linkPortalTo(portal, counterPortal, player);
				return false;
			}
			
			portalHandler.linkPortalTo(portal, counterPortal, player);
			
			player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The veil between the two worlds has lifted a little bit!");
			
			return true;
			
		} catch (IllegalArgumentException | IllegalStateException e) {
			
			MessageUtils.sendWarning(player, e.getMessage());
			return false;
		}
	}
}