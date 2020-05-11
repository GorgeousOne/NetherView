package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockCopy;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockListener implements Listener {
	
	private Main main;
	private PortalHandler portalHandler;
	private ViewingHandler viewingHandler;
	
	public BlockListener(Main main,
	                     PortalHandler portalHandler,
	                     ViewingHandler viewingHandler) {
		this.main = main;
		this.portalHandler = portalHandler;
		this.viewingHandler = viewingHandler;
	}
	
	private void removeDamagedPortals(Block block) {
		
		World blockWorld = block.getWorld();
		
		if (!main.canBeViewed(blockWorld))
			return;
		
		BlockVec blockLoc = new BlockVec(block);
		
		for (Portal portal : new HashSet<>(portalHandler.getPortals(blockWorld))) {
			if (portal.contains(blockLoc))
				portalHandler.removePortal(portal);
		}
	}
	
	private void updateBlockCaches(Block block, BlockData newBlockData, boolean blockWasOccluding) {
		
		World blockWorld = block.getWorld();
		
		if (!main.canBeViewed(blockWorld))
			return;
		
		BlockVec blockPos = new BlockVec(block);
		
		//TODO filter caches by world
		for (Portal portal : portalHandler.getPortals(blockWorld)) {
			
			Map.Entry<BlockCache, BlockCache> caches = portal.getBlockCaches();
			BlockCache front = caches.getKey();
			BlockCache back = caches.getValue();
			
			if (front.contains(blockPos)) {
				Set<BlockCopy> updatedCopies = BlockCacheFactory.updateBlockInCache(front, block, newBlockData, blockWasOccluding);
				
				if (updatedCopies.isEmpty())
					viewingHandler.updateProjections(front, updatedCopies);
			}
			
			if (back.contains(blockPos)) {
				Set<BlockCopy> updatedCopies = BlockCacheFactory.updateBlockInCache(back, block, newBlockData, blockWasOccluding);
				
				if (updatedCopies.isEmpty())
					viewingHandler.updateProjections(back, updatedCopies);
			}
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
	
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		
		Block block = event.getBlock();
		updateBlockCaches(block, Material.AIR.createBlockData(), block.getType().isOccluding());
		
		Material blockType = block.getType();
		if (blockType == Material.OBSIDIAN || blockType == Material.NETHER_PORTAL)
			removeDamagedPortals(block);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		
		Block block = event.getBlock();
		updateBlockCaches(block, block.getBlockData(), false);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		
		for (Block block : event.blockList()) {
			if(block.getType() == Material.NETHER_PORTAL)
				removeDamagedPortals(block);
		}
		
		for (Block block : event.blockList())
			updateBlockCaches(block, Material.AIR.createBlockData(), block.getType().isOccluding());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockExplode(EntityExplodeEvent event) {
		for (Block block : event.blockList())
			updateBlockCaches(block, Material.AIR.createBlockData(), block.getType().isOccluding());
	}
	
	//water, lava, dragon eggs
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockSpill(BlockFromToEvent event) {
		Block block = event.getToBlock();
		updateBlockCaches(block, event.getBlock().getBlockData(), false);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		Block block = event.getBlock();
		updateBlockCaches(block, Material.AIR.createBlockData(), block.getType().isOccluding());
	}
	
	private void onAnyGrowEvent(BlockGrowEvent event) {
		Block block = event.getBlock();
		updateBlockCaches(block, event.getNewState().getBlockData(), block.getType().isOccluding());
	}
	
	//pumpkin/melon growing
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockGrow(BlockGrowEvent event) {
		onAnyGrowEvent(event);
	}
	
	//grass, mycelium spreading
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent event) {
		onAnyGrowEvent(event);
	}
	
	//obsidian, concrete
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockForm(BlockFormEvent event) {
		onAnyGrowEvent(event);
	}
	
	//ice melting
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
		Block block = event.getBlock();
		updateBlockCaches(block, event.getNewState().getBlockData(), block.getType().isOccluding());
	}
	
	//falling sand and maybe endermen (actually also sheeps but that doesn't work)
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		Block block = event.getBlock();
		updateBlockCaches(block, event.getBlockData(), false);
	}
}