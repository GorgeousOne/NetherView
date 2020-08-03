package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.utils.MessageUtils;
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
	
	private NetherView main;
	private PortalHandler portalHandler;
	private ViewHandler viewHandler;
	
	public TeleportListener(NetherView main,
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
		if (from.getWorld() == to.getWorld() && player.hasPermission(NetherView.VIEW_PERM)) {
			viewHandler.displayClosestPortalTo(player, to.clone().add(0, player.getEyeHeight(), 0));
			return;
		}
		
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL ||
		    !player.hasPermission(NetherView.LINK_PERM)) {
			return;
		}
		
		boolean createdNewPortalView = createPortalView(event);
		
		if (createdNewPortalView && (player.getGameMode() == GameMode.CREATIVE || main.cancelTeleportWhenLinkingPortalsEnabled())) {
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
			MessageUtils.printDebug("No portal found at starting block " + new BlockVec(from).toString());
			return false;
		}
		
		Block counterPortalBlock = PortalLocator.getNearbyPortalBlock(to);
		
		if (counterPortalBlock == null) {
			MessageUtils.printDebug("No portal found at destination block " + new BlockVec(to).toString());
			return false;
		}
		
		try {
			Portal portal = portalHandler.getPortalByBlock(portalBlock);
			Portal counterPortal = portalHandler.getPortalByBlock(counterPortalBlock);
			
			if (portal.getCounterPortal() == counterPortal) {
				event.setTo(createTpDestinationWithFixedYaw(from, to, portal.getTpTransform()));
				return false;
			}
			
			if (portal.isLinked()) {
				portal.removeLink();
				portalHandler.linkPortalTo(portal, counterPortal);
				return false;
			}
			
			portalHandler.linkPortalTo(portal, counterPortal);
			player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The veil between the two worlds has lifted a little bit!");
			return true;
			
		} catch (IllegalArgumentException | IllegalStateException e) {
			
			MessageUtils.sendWarning(player, e.getMessage());
			return false;
		}
	}
	
	/**
	 * Makes sure that the yaw of a nether portal teleport changes according to transform that was calculated to
	 * create the view animation of a portal. E.g. portals leaned against walls have the tendency to differ
	 * from my basic rotation calculation. <p>
	 * This only work for 1.15 and higher though (I didn't test 1.14 yet)
	 */
	private Location createTpDestinationWithFixedYaw(Location from, Location to, Transform portalTransform) {
		
		Location fixedDestination = to.clone();
		fixedDestination.setYaw(portalTransform.rotateYaw(from.getYaw()));
		return fixedDestination;
	}
}