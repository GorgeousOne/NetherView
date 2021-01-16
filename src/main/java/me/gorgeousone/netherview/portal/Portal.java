package me.gorgeousone.netherview.portal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.Cuboid;
import me.gorgeousone.netherview.wrapper.Axis;

/**
 * A class containing information about a located portal structure in a world.
 */
public class Portal {
	
	private final World world;
	private final AxisAlignedRect portalRect;
	
	private final Set<Block> portalBlocks;
	
	private HashMap<Player, Portal> counterPortals;
	private HashMap<Player, Transform> tpTransforms;

	private HashMap<Player, Map.Entry<BlockCache, BlockCache>> blockCaches;
	private HashMap<Player, Map.Entry<ProjectionCache, ProjectionCache>> projectionCaches;
	
	private final Cuboid frameShape;
	private final Cuboid innerShape;
	
	private boolean isViewFlipped;
	
	public Portal(World world,
	              AxisAlignedRect portalRect,
	              Cuboid frameShape,
	              Cuboid innerShape,
	              Set<Block> portalBlocks) {
		
		this.world = world;
		this.portalRect = portalRect.clone();
		this.frameShape = frameShape.clone();
		this.innerShape = innerShape.clone();
		this.portalBlocks = portalBlocks;

		counterPortals = new HashMap<Player, Portal>();
		tpTransforms = new HashMap<Player, Transform>();

		blockCaches = new HashMap<Player, Map.Entry<BlockCache, BlockCache>>();
		projectionCaches = new HashMap<Player, Map.Entry<ProjectionCache, ProjectionCache>>();
	}
	
	public World getWorld() {
		return world;
	}
	
	public Location getLocation() {
		return portalRect.getMin().toLocation(world);
	}
	
	public BlockVec getMaxBlockAtFloor() {
		
		BlockVec maxBlock = frameShape.getMax().clone().add(-1, 0, -1);
		maxBlock.setY(frameShape.getMin().getY());
		return maxBlock;
	}
	
	public Cuboid getFrame() {
		return frameShape;
	}
	
	public Cuboid getInner() {
		return innerShape;
	}
	
	public int width() {
		return getAxis() == Axis.X ? frameShape.getWidthX() : frameShape.getWidthZ();
	}
	
	public int height() {
		return frameShape.getHeight();
	}
	
	public AxisAlignedRect getPortalRect() {
		return portalRect.clone();
	}
	
	public Axis getAxis() {
		return portalRect.getAxis();
	}
	
	public Set<Block> getPortalBlocks() {
		return new HashSet<>(portalBlocks);
	}
	
	public boolean equalsInSize(Portal other) {
		
		AxisAlignedRect otherRect = other.getPortalRect();
		
		return portalRect.width() == otherRect.width() &&
		       portalRect.height() == otherRect.height();
	}
	
	public Portal getCounterPortal(Player player) {
		if (counterPortals.containsKey(player)) return counterPortals.get(player);
		return counterPortals.get(null);
	}
	
	public HashMap<Player, Portal> getCounterPortals() {
		return counterPortals;
	}
	
	public void setTpTransform(Player player, Transform tpTransform) {
		tpTransforms.put(player, tpTransform);
	}
	
	public Transform getTpTransform(Player player) {
		if (tpTransforms.containsKey(player)) return tpTransforms.get(player);
		return tpTransforms.get(null);
	}
	
	public void setLinkedTo(Player player, Portal counterPortal) {
		counterPortals.put(player, counterPortal);
	}
	
	public void removeLink(Player player) {

		this.counterPortals.remove(player);
		this.tpTransforms.remove(player);
		removeProjectionCaches(player);
	}
	
	@SuppressWarnings("unchecked")
	public void removeLinks() {
		for (Player player : ((HashMap<Player, Portal>) counterPortals.clone()).keySet()) {
			this.counterPortals.remove(player);
		}
		for (Player player : ((HashMap<Player, Portal>) tpTransforms.clone()).keySet()) {
			this.tpTransforms.remove(player);
		}
		for (Player player : ((HashMap<Player, Portal>) projectionCaches.clone()).keySet()) {
			removeProjectionCaches(player);
		}
	}
	
	public boolean isLinked(Player player) {
		return counterPortals.containsKey(player);
	}
	
	public boolean hasLinks() {
		return counterPortals.size()!=0;
	}
	
	public void setBlockCaches(Player player, Map.Entry<BlockCache, BlockCache> blockCaches) {
		this.blockCaches.put(player, blockCaches);
	}
	
	public void removeBlockCaches(Player player) {
		blockCaches.remove(player);
	}
	
	public boolean blockCachesAreLoaded(Player player) {
		return blockCaches.containsKey(player);
	}
	
	public BlockCache getFrontCache(Player player) {
		return blockCaches.get(player).getKey();
	}
	
	public BlockCache getBackCache(Player player) {
		return blockCaches.get(player).getValue();
	}
	
	/**
	 * Sets the front and back projection caches for this portal.
	 *
	 * @param projectionCaches where the key is referred to as front projection and value as back projection
	 */
	public void setProjectionCaches(Player player, Map.Entry<ProjectionCache, ProjectionCache> projectionCaches) {
		this.projectionCaches.put(player, projectionCaches);
	}
	
	public void removeProjectionCaches(Player player) {
		this.projectionCaches.remove(player);
	}
	
	public boolean projectionsAreLoaded(Player player) {
		return projectionCaches.containsKey(player);
	}
	
	public ProjectionCache getFrontProjection(Player player) {
		return projectionCaches.get(player).getKey();
	}
	
	public ProjectionCache getBackProjection(Player player) {
		return projectionCaches.get(player).getValue();
	}
	
	public HashMap<Player, Entry<ProjectionCache, ProjectionCache>> getProjectionCaches() {
		return projectionCaches;
	}
	
	/**
	 * Returns true if the the 2 projections of the portal have been switched with each other for aesthetic reasons.
	 */
	public boolean isViewFlipped() {
		return isViewFlipped;
	}
	
	/**
	 * Sets whether the 2 projections of the portal are switched with each other or not.
	 * The {@link ProjectionCache}s have to be set again to realize this change.
	 */
	public void setViewFlipped(boolean isViewFlipped) {
		this.isViewFlipped = isViewFlipped;
	}
	
	public void flipView() {
		isViewFlipped = !isViewFlipped;
	}
	
	@Override
	public String toString() {
		return BlockVec.toSimpleString(getLocation());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getLocation());
	}
}