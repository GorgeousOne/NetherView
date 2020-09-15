package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.wrapper.WrappedBoundingBox;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerViewSession {
	
	private final UUID playerId;
	
	private Portal viewedPortal;
	private ProjectionCache viewedPortalSide;
	private ViewFrustum lastViewFrustum;
	
	private final Map<BlockVec, BlockType> projectedBlocks;
	private final Set<Entity> hiddenEntities;
	private final Map<Entity, Location> projectedEntities;
	
	public PlayerViewSession(Player player) {
		
		this.playerId = player.getUniqueId();
		this.projectedBlocks = new HashMap<>();
		this.hiddenEntities = new HashSet<>();
		this.projectedEntities = new HashMap<>();
	}
	
	public UUID getPlayerId() {
		return playerId;
	}
	
	public Player getPlayer() {
		return Bukkit.getPlayer(playerId);
	}
	
	public Portal getViewedPortal() {
		return viewedPortal;
	}
	
	public void setViewedPortal(Portal viewedPortal) {
		this.viewedPortal = viewedPortal;
	}
	
	/**
	 * Returns the one of the two projection caches of a portal that is being displayed to the player in the portal view.
	 */
	public ProjectionCache getViewedPortalSide() {
		return viewedPortalSide;
	}
	
	public void setViewedPortalSide(ProjectionCache viewedPortalSide) {
		this.viewedPortalSide = viewedPortalSide;
	}
	
	public ViewFrustum getLastViewFrustum() {
		return lastViewFrustum;
	}
	
	/**
	 * Returns the latest view frustum that was used to calculate the projection blocks for the player's projection.
	 * Returns null if player is not nearby any portal or no blocks were displayed (due to steep view angles)
	 */
	public void setLastViewFrustum(ViewFrustum lastViewFrustum) {
		this.lastViewFrustum = lastViewFrustum;
	}
	
	/**
	 * Returns a Map of BlockTypes linked to their location that are currently displayed with fake blocks to the player.
	 * The method is being used very frequently so it does not check for the player's permission to view portal projections.
	 * Returns (and internally adds) an empty Map if no projected blocks found.
	 */
	public Map<BlockVec, BlockType> getProjectedBlocks() {
		return projectedBlocks;
	}
	
	public Set<Entity> getHiddenEntities() {
		return hiddenEntities;
	}
	
	public Map<Entity, Location> getProjectedEntities() {
		return projectedEntities;
	}
	
	public boolean isHiddenBehindProjection(Entity entity) {
		
		if (lastViewFrustum == null) {
			return false;
		}
		
		WrappedBoundingBox boundingBox = WrappedBoundingBox.of(entity, entity.getLocation());
		return boundingBox.intersectsBlockCache(viewedPortalSide) && boundingBox.intersectsFrustum(lastViewFrustum);
	}
	
	public boolean isVisibleInProjection(Entity entity) {
		
		if (lastViewFrustum == null) {
			return false;
		}
		
		Location projectionLoc = viewedPortalSide.getLinkTransform().transformLoc(entity.getLocation());
		WrappedBoundingBox boundingBox = WrappedBoundingBox.of(entity, projectionLoc);
		
		return boundingBox.intersectsBlockCache(viewedPortalSide) && boundingBox.intersectsFrustum(lastViewFrustum);
	}
}