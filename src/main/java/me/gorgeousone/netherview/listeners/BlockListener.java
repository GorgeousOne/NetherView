package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.Main;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.portal.Portal;
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
	
	private void updateBlockCaches(Block block) {
		
		BlockVec vec = new BlockVec(block);
		for(BlockCache cache : portalHandler.getSourceCaches()) {
			
			if(cache.contains(vec)) {
				cache.updateCopy(block);
			}
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
	
	
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		removeDamagedPortals(event.getBlock());
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		//		event.get
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		
		for (Block block : event.blockList()) {
		}
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