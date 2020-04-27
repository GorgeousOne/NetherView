package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.blockcache.BlockCache;
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

import java.util.HashSet;
import java.util.Set;

public class ViewingHandler {
	
	private PortalHandler portalHandler;
	
	public ViewingHandler(PortalHandler portalHandler) {
		this.portalHandler = portalHandler;
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
		
		for(Block block : portal.getFrameBlocks()) {
			BlockCopy glass = new BlockCopy(block);
			glass.setData(Material.PURPLE_STAINED_GLASS.createBlockData());
			visibleBlocks.add(glass);
		}
		
//		displayFrustum(player, playerFrustum);
		displayBlocks(player, visibleBlocks);
	}
	
	private Set<BlockCopy> getAllBlocks(Portal portal, BlockCache blocks, Transform blockTransform) {
		
		Set<BlockCopy> allBlocks = new HashSet<>();
		
		BlockVec min = blocks.getMin();
		BlockVec max = blocks.getMax();
		
		for(int x = min.getX(); x < max.getX(); x++) {
			for(int y = min.getY(); y < max.getY(); y++) {
				for(int z = min.getZ(); z < max.getZ(); z++) {
					
					BlockVec blockLoc = new BlockVec(x, y, z);
					BlockCopy block = blocks.getCopyAt(blockLoc);
					
					if(block != null) {
						block.setPosition(blockTransform.getTransformedVec(blockLoc));
						allBlocks.add(block);
					}
				}
			}
		}
		
		for(Block block : portal.getPortalBlocks()) {
			BlockCopy air = new BlockCopy(block);
			air.setData(Material.AIR.createBlockData());
			allBlocks.add(air);
		}
		
		return allBlocks;
	}
	
	private Set<BlockCopy> getBlocksInFrustum(Player player, BlockCache cache, ViewingFrustum frustum, Transform blockTransform) {
		
		Set<BlockCopy> blocksInFrustum = new HashSet<>();
		
		BlockVec min = cache.getMin();
		BlockVec max = cache.getMax();
		
		for (int x = min.getX(); x < max.getX(); x++) {
			for (int y = min.getY(); y < max.getY(); y++) {
				for (int z = min.getZ(); z < max.getZ(); z++) {
					
					BlockVec transformedCorner = blockTransform.getTransformedVec(new BlockVec(x, y, z));
					
					if (!frustum.contains(transformedCorner.toVector()))
						continue;
					
					player.spawnParticle(
							Particle.FLAME,
							transformedCorner.getX(),
					        transformedCorner.getY(),
							transformedCorner.getZ(),
							0, 0, 0, 0);
					
					for (BlockCopy blockCopy : cache.getCopiesAround(new BlockVec(x, y, z))) {
						BlockCopy copy = blockCopy.clone();
						copy.setPosition(blockTransform.getTransformedVec(copy.getPosition()));
						
						blocksInFrustum.add(copy);
					}
				}
			}
		}
		
		return blocksInFrustum;
	}
	
	private void displayFrustum(Player player, ViewingFrustum frustum) {
		
		AxisAlignedRect nearPlane = frustum.getNearPlane();
		World world = player.getWorld();
		
		player.spawnParticle(Particle.FLAME, nearPlane.getMin().toLocation(world), 0, 0, 0, 0);
		player.spawnParticle(Particle.FLAME, nearPlane.getMax().toLocation(world), 0, 0, 0, 0);
	}
	
	private void displayBlocks(Player player, Set<BlockCopy> blocks) {
	
		for(BlockCopy copy : blocks) {
			player.sendBlockChange(copy.getPosition().toLocation(player.getWorld()), copy.getBlockData());
		}
	}
}
