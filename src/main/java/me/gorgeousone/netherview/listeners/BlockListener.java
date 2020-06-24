package me.gorgeousone.netherview.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.handlers.PacketHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import org.bukkit.event.world.StructureGrowEvent;

import java.util.HashSet;
import java.util.Map;

/**
 * Listens to block changes in portal block caches to let the ViewHandler update portal animations live.
 */
public class BlockListener implements Listener {
	
	private NetherView main;
	private PortalHandler portalHandler;
	private ViewHandler viewHandler;
	private PacketHandler packetHandler;
	
	private Material portalMaterial;
	
	public BlockListener(NetherView main,
	                     PortalHandler portalHandler,
	                     ViewHandler viewHandler, PacketHandler packetHandler, Material portalMaterial) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
		this.packetHandler = packetHandler;
		this.portalMaterial = portalMaterial;
		
		addBlockUpdateInterceptor();
	}
	
	/**
	 * Prevents
	 */
	private void addBlockUpdateInterceptor() {
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		
		protocolManager.addPacketListener(
				new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.BLOCK_CHANGE) {
					
					@Override
					public void onPacketSending(PacketEvent event) {
						
						Player player = event.getPlayer();
						
						if (event.isCancelled() || !viewHandler.isViewingAPortal(player)) {
							return;
						}
						
						BlockPosition blockPos = event.getPacket().getBlockPositionModifier().getValues().get(0);
						BlockVec blockPosVec = new BlockVec(blockPos);
						
						//execute some light weight bounding box checks before searching the block in the huge map of displayed blocks? Does that save time?
						if (!viewHandler.getViewedProjection(player).contains(blockPosVec) && !viewHandler.getViewedPortal(player).contains(blockPosVec)) {
							return;
						}
						
						Map<BlockVec, BlockType> viewSession = viewHandler.getViewSession(player);
						
						if (viewSession.containsKey(new BlockVec(blockPos))) {
							event.setCancelled(true);
						}
					}
				}
		);
		
		//intercepts multi block edits any changed block data inside a portal animation back to the animation block data
		protocolManager.addPacketListener(
				new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
					
					@Override
					public void onPacketSending(PacketEvent event) {
						
						PacketContainer packet = event.getPacket();
						
						if (packetHandler.isCustomViewPacket(packet) || event.isCancelled()) {
							return;
						}
						
						Player player = event.getPlayer();
						
						if (!viewHandler.isViewingAPortal(player)) {
							return;
						}
						
						MultiBlockChangeInfo[] blockInfoArray = packet.getMultiBlockChangeInfoArrays().getValues().get(0);
						
						Portal viewedPortal = viewHandler.getViewedPortal(player);
						ProjectionCache viewedProjection = viewHandler.getViewedProjection(player);
						Map<BlockVec, BlockType> viewSession = viewHandler.getViewSession(player);
						
						ChunkCoordIntPair chunkCoords = packet.getChunkCoordIntPairs().getValues().get(0);
						int worldX = chunkCoords.getChunkX() << 4;
						int worldZ = chunkCoords.getChunkZ() << 4;
						
						//filter all block changes that are happening inside a projection
						for (MultiBlockChangeInfo blockInfo : blockInfoArray) {
							
							BlockVec blockPos = new BlockVec(
									blockInfo.getX() + worldX,
									blockInfo.getY(),
									blockInfo.getZ() + worldZ);
							
							if (viewSessionContainsVec(blockPos, viewedPortal, viewedProjection, viewSession)) {
								blockInfo.setData(viewSession.get(blockPos).getWrapped());
							}
						}
						
						packet.getMultiBlockChangeInfoArrays().write(0, blockInfoArray);
					}
				}
		);
	}
	
	private boolean viewSessionContainsVec(BlockVec blockPos,
	                                       Portal viewedPortal,
	                                       ProjectionCache viewedCache,
	                                       Map<BlockVec, BlockType> viewSession) {
		return (viewedPortal.contains(blockPos) || viewedCache.contains(blockPos)) && viewSession.containsKey(blockPos);
	}
	
	private void removeDamagedPortals(Block block) {
		
		World blockWorld = block.getWorld();
		
		if (!portalHandler.hasPortals(blockWorld)) {
			return;
		}
		
		BlockVec blockLoc = new BlockVec(block);
		
		for (Portal portal : new HashSet<>(portalHandler.getPortals(blockWorld))) {
			
			if (portal.contains(blockLoc)) {
				viewHandler.removePortal(portal);
				portalHandler.removePortal(portal);
			}
		}
	}
	
	private void updateBlockCaches(Block block, BlockType newBlockType, boolean blockWasOccluding) {
		
		World blockWorld = block.getWorld();
		
		if (!portalHandler.hasPortals(blockWorld)) {
			return;
		}
		
		BlockVec blockPos = new BlockVec(block);
		
		for (BlockCache cache : portalHandler.getBlockCaches(blockWorld)) {
			
			if (!cache.contains(blockPos)) {
				continue;
			}
			
			Map<BlockVec, BlockType> updatedCopies = BlockCacheFactory.updateBlockInCache(cache, block, newBlockType, blockWasOccluding);
			
			if (!updatedCopies.isEmpty()) {
				viewHandler.updateProjections(cache, updatedCopies);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockInteract(PlayerInteractEvent event) {
		
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		
		Player player = event.getPlayer();
		
		if (!viewHandler.isViewingAPortal(player)) {
			return;
		}
		
		Map<BlockVec, BlockType> viewSession = viewHandler.getViewSession(player);
		BlockVec blockPos = new BlockVec(event.getClickedBlock());
		
		if (viewSession.containsKey(blockPos)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		
		Block block = event.getBlock();
		Material blockType = block.getType();
		
		updateBlockCaches(block, BlockType.of(Material.AIR), block.getType().isOccluding());
		
		if (blockType == Material.OBSIDIAN || blockType == portalMaterial) {
			removeDamagedPortals(block);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		
		Block block = event.getBlock();
		updateBlockCaches(block, BlockType.of(block), false);
		
		Player player = event.getPlayer();
		
		if (!viewHandler.isViewingAPortal(player)) {
			return;
		}
		
		Map<BlockVec, BlockType> viewSession = viewHandler.getViewSession(player);
		BlockVec blockPos = new BlockVec(block);
		
		if (viewSession.containsKey(blockPos)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		
		for (Block block : event.blockList()) {
			if (block.getType() == portalMaterial) {
				removeDamagedPortals(block);
			}
		}
		
		for (Block block : event.blockList())
			updateBlockCaches(block, BlockType.of(Material.AIR), block.getType().isOccluding());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockExplode(EntityExplodeEvent event) {
		
		for (Block block : event.blockList()) {
			if (block.getType() == portalMaterial) {
				removeDamagedPortals(block);
			}
		}
		
		for (Block block : event.blockList())
			updateBlockCaches(block, BlockType.of(Material.AIR), block.getType().isOccluding());
	}
	
	//water, lava, dragon eggs
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockSpill(BlockFromToEvent event) {
		
		Block block = event.getToBlock();
		updateBlockCaches(block, BlockType.of(event.getBlock()), false);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		
		Block block = event.getBlock();
		updateBlockCaches(block, BlockType.of(Material.AIR), block.getType().isOccluding());
	}
	
	private void onAnyGrowEvent(BlockGrowEvent event) {
		
		Block block = event.getBlock();
		updateBlockCaches(block, BlockType.of(event.getNewState()), block.getType().isOccluding());
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
		updateBlockCaches(block, BlockType.of(event.getNewState()), block.getType().isOccluding());
	}
	
	//falling sand and maybe endermen (actually also sheeps but that doesn't work)
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		
		//TODO check what 1.8 uses instead of event.getBlockData()
		Block block = event.getBlock();
		updateBlockCaches(block, BlockType.of(event.getBlock()), false);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlantGrow(StructureGrowEvent event) {
		
		for (BlockState state : event.getBlocks()) {
			updateBlockCaches(state.getBlock(), BlockType.of(state), false);
		}
	}
}