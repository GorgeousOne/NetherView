package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.BlockCopy;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewingHandler;
import me.gorgeousone.netherview.portal.Portal;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockListener implements Listener {
	
	private NetherView main;
	private PortalHandler portalHandler;
	private ViewingHandler viewingHandler;
	
	public BlockListener(NetherView main,
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
			
			if (portal.contains(blockLoc)) {
				viewingHandler.removePortal(portal);
				portalHandler.removePortal(portal);
			}
		}
	}
	
	private void updateBlockCaches(Block block, BlockData newBlockData, boolean blockWasOccluding) {
		
		World blockWorld = block.getWorld();
		
		if (!main.canBeViewed(blockWorld))
			return;
		
		BlockVec blockPos = new BlockVec(block);
		
		for (BlockCache cache : portalHandler.getBlockCaches(blockWorld)) {
			
			if (!cache.contains(blockPos))
				continue;
				
			Set<BlockCopy> updatedCopies = BlockCacheFactory.updateBlockInCache(cache, block, newBlockData, blockWasOccluding);
			
			if (!updatedCopies.isEmpty())
				viewingHandler.updateProjections(cache, updatedCopies);
		}
		
		refreshProjections(block);
	}
	
	private void refreshProjections(Block block) {
		
		World blockWorld = block.getWorld();
		
		if (!main.canViewOtherWorlds(blockWorld))
			return;
		
		BlockVec blockPos = new BlockVec(block);
		
		for(ProjectionCache projection : portalHandler.getProjectionCaches(blockWorld)) {
			
			if(!projection.contains(blockPos))
				continue;
			
			viewingHandler.refreshProjection(projection.getPortal(), projection.getCopyAt(new BlockVec(block)));
		}
	}
	
//	@EventHandler
//	public void onInteract(PlayerInteractEvent event) {
//
//		Action action = event.getAction();
//
//		if(action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK)
//			return;
//
//		Player player = event.getPlayer();
//
//		if (!viewingHandler.hasViewSession(player))
//			return;
//
//		Set<BlockCopy> viewSession = viewingHandler.getViewSession(player);
//		BlockCopy clickedBlock = new BlockCopy(event.getClickedBlock());
//
//		if (viewSession.contains(clickedBlock)) {
//			new BukkitRunnable() {
//				@Override
//				public void run() {
//
//					for(BlockCopy copy : viewSession) {
//						if(copy.equals(clickedBlock)) {
//							viewingHandler.displayBlockCopy(player, copy);
//							return;
//						}
//					}
//				}
//			}.runTask(main);
//		}
//	}
	
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