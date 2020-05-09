package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockCopy;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.handlers.BlockCacheHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.Bukkit;
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
import java.util.Set;

public class BlockListener implements Listener {
	
	private Main main;
	private PortalHandler portalHandler;
	private BlockCacheHandler cacheHandler;
	private ViewingHandler viewingHandler;
	
	public BlockListener(Main main,
	                     PortalHandler portalHandler,
	                     BlockCacheHandler cacheHandler,
	                     ViewingHandler viewingHandler) {
		this.main = main;
		this.portalHandler = portalHandler;
		this.cacheHandler = cacheHandler;
		this.viewingHandler = viewingHandler;
	}
	
	private void onBlockChange(Block block, BlockData newData) {
		
		World blockWorld = block.getWorld();
		
		if (!main.canBeViewed(blockWorld))
			return;
		
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
		
		BlockVec blockPos = new BlockVec(block);
		for(BlockCache cache : cacheHandler.getSourceCaches()) {

			if(!cache.contains(blockPos))
				continue;
			
			Set<BlockCopy> updatedCopies = BlockCacheFactory.updateBlockInCache(cache, block, newBlockData, blockWasOccluding);
			
			if(updatedCopies.isEmpty())
				continue;
			
			viewingHandler.updateProjections(cache, updatedCopies);
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		
		Block block = event.getBlock();
		removeDamagedPortals(block);
		updateBlockCaches(block, Material.AIR.createBlockData(), block.getType().isOccluding());
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Bukkit.broadcastMessage("from PROBABLY AIR to " + event.getBlock().getType().name());
		Block block = event.getBlock();
		updateBlockCaches(block, block.getBlockData(), false);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockExplode(EntityExplodeEvent event) {
		
		for (Block block : event.blockList()) {
		}
	}
	
	//water, lava, dragon eggs
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockSpill(BlockFromToEvent event) {
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
	}
	
	//pumpkin/melon growing
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockGrow(BlockGrowEvent event) {
	}
	
	//grass, mycelium spreading
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent event) {
	}
	
	//obsidian, concrete
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockForm(BlockFormEvent event) {
	}
	
	//ice melting
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
	}
	
	//falling sand and maybe endermen
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
	}
}