package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockCopy;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLink;
import me.gorgeousone.netherview.threedstuff.AxisAlignedRect;
import me.gorgeousone.netherview.viewfrustum.ViewingFrustum;
import me.gorgeousone.netherview.viewfrustum.ViewingFrustumFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ViewingHandler {
	
	private PortalHandler portalHandler;
	private Map<UUID, Set<BlockCopy>> playerViews;
	
	public ViewingHandler(PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
		playerViews = new HashMap<>();
	}
	
	public Set<BlockCopy> getViewSession(Player player) {
		
		UUID uuid = player.getUniqueId();
		
		if(!playerViews.containsKey(uuid))
			playerViews.put(uuid, new HashSet<>());
		
		return playerViews.get(uuid);
	}
	
	public void displayPortal(Player player, Portal portal) {
		
		PortalLink link = portalHandler.getPortalLink(portal);
		
		if(link == null) {
			player.sendMessage(ChatColor.GRAY + "Portal is not linked.");
			return;
		}
		
		Portal counterPortal = link.getCounterPortal();
		
		if(!counterPortal.equalsInSize(portal)) {
			Bukkit.broadcastMessage("portals are not the same size");
			return;
		}
		
		BlockCache cache = portalHandler.getBlockCache(link.getCounterPortal());
		Location playerEyeLoc = player.getEyeLocation();
		ViewingFrustum playerFrustum = ViewingFrustumFactory.createFrustum2(playerEyeLoc.toVector(), portal.getPortalRect());
		
//		Set<BlockCopy> visibleBlocks = getAllBlocks(portal, cache, link.getTransform());
		Set<BlockCopy> visibleBlocks = getBlocksInFrustum(player, cache, playerFrustum, link.getTransform());
		
		for(Block block : portal.getPortalBlocks()) {
			BlockCopy air = new BlockCopy(block);
			air.setData(Material.AIR.createBlockData());
			visibleBlocks.add(air);
		}
//
//		for(Block block : portal.getFrameBlocks()) {
//			BlockCopy glass = new BlockCopy(block);
//			glass.setData(Material.PURPLE_STAINED_GLASS.createBlockData());
//			visibleBlocks.add(glass);
//		}
		
		displayFrustum(player, playerFrustum);
		displayBlocks(player, visibleBlocks);
	}
	
	private Set<BlockCopy> getAllBlocks(Portal portal, BlockCache blocks, Transform blockTransform) {

		BlockCache transformedCache = BlockCacheFactory.getTransformed(blocks, blockTransform);
		Set<BlockCopy> allBlocks = new HashSet<>();

		BlockVec min = transformedCache.getMin();
		BlockVec max = transformedCache.getMax();

		for(int x = min.getX(); x < max.getX(); x++) {
			for(int y = min.getY(); y < max.getY(); y++) {
				for(int z = min.getZ(); z < max.getZ(); z++) {

					BlockVec blockLoc = new BlockVec(x, y, z);
					BlockCopy block = transformedCache.getCopyAt(blockLoc);

					if(block != null) {
						allBlocks.add(block);
					}
				}
			}
		}

		return allBlocks;
	}
	
	private Set<BlockCopy> getBlocksInFrustum(Player player, BlockCache cache, ViewingFrustum frustum, Transform blockTransform) {
		
		Set<BlockCopy> blocksInFrustum = new HashSet<>();
		BlockCache transformedCache = BlockCacheFactory.getTransformed(cache, blockTransform);
		
		BlockVec min = transformedCache.getMin();
		BlockVec max = transformedCache.getMax();
		
		for (int x = min.getX(); x < max.getX(); x++) {
			for (int y = min.getY(); y < max.getY(); y++) {
				for (int z = min.getZ(); z < max.getZ(); z++) {
					
					BlockVec corner = new BlockVec(x, y, z);
					
					if (!frustum.contains(corner.toVector()))
						continue;
					
//					player.spawnParticle(
//							Particle.FLAME,
//							corner.getX(),
//							corner.getY(),
//							corner.getZ(),
//							0, 0, 0, 0);
					
					for (BlockCopy blockCopy : transformedCache.getCopiesAround(new BlockVec(x, y, z))) {
						blocksInFrustum.add(blockCopy);
					}
				}
			}
		}
		
		return blocksInFrustum;
	}
	
	private void displayFrustum(Player player, ViewingFrustum frustum) {
		
		AxisAlignedRect nearPlane = frustum.getNearPlaneRect();
		World world = player.getWorld();
		
		player.spawnParticle(Particle.FLAME, nearPlane.getMin().toLocation(world), 0, 0, 0, 0);
		player.spawnParticle(Particle.FLAME, nearPlane.getMax().toLocation(world), 0, 0, 0, 0);
	}
	
	private void displayBlocks(Player player, Set<BlockCopy> blocksToDisplay) {
		
		World playerWorld = player.getWorld();
		Set<BlockCopy> viewSession = getViewSession(player);
		Iterator<BlockCopy> iterator = viewSession.iterator();
		
		while (iterator.hasNext()) {
			BlockCopy nextCopy = iterator.next();
			
			if (!blocksToDisplay.contains(nextCopy)) {
				refreshBlock(player, nextCopy);
				iterator.remove();
			}
		}
		
		for (BlockCopy copy : blocksToDisplay) {
			if(viewSession.add(copy))
				player.sendBlockChange(copy.getPosition().toLocation(playerWorld), copy.getBlockData());
		}
		
		for(BlockCopy copy : blocksToDisplay) {
			player.sendBlockChange(copy.getPosition().toLocation(player.getWorld()), copy.getBlockData());
		}
	}
	
	private void refreshBlock(Player player, BlockCopy blockCopy) {
		Location blockLoc = blockCopy.getPosition().toLocation(player.getWorld());
		player.sendBlockChange(blockLoc, blockLoc.getBlock().getBlockData());
	}
}
