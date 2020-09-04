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
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.handlers.PacketHandler;
import me.gorgeousone.netherview.handlers.PortalHandler;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.utils.VersionUtils;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * Listens to block changes in portal block caches to let the ViewHandler update portal animations live.
 */
public class BlockChangeListener implements Listener {
	
	private final NetherViewPlugin main;
	private final PortalHandler portalHandler;
	private final ViewHandler viewHandler;
	private final PacketHandler packetHandler;
	private final Material portalMaterial;
	
	public BlockChangeListener(NetherViewPlugin main,
	                           PortalHandler portalHandler,
	                           ViewHandler viewHandler, PacketHandler packetHandler, Material portalMaterial) {
		
		this.main = main;
		this.portalHandler = portalHandler;
		this.viewHandler = viewHandler;
		this.packetHandler = packetHandler;
		this.portalMaterial = portalMaterial;
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		addBlockUpdateInterception(protocolManager);
		addMultiBlockUpdateInterception(protocolManager);
		addBlockDigInterception(protocolManager);
	}
	
	/**
	 * Prevents players from breaking a portal projection when they are breaking or stopping to break a block.
	 * (Still feels like im missing a some packet or event that is fired before)
	 */
	private void addBlockDigInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Client.BLOCK_DIG) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
				Player player = event.getPlayer();
				
				if (packetHandler.isCustomPacket(packet) || !viewHandler.isViewingAPortal(player)) {
					return;
				}
				
				EnumWrappers.PlayerDigType digType = packet.getPlayerDigTypes().read(0);
				
				if (digType != EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK &&
				    digType != EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
					return;
				}
				
				BlockPosition blockPos = packet.getBlockPositionModifier().read(0);
				BlockType projectedBlockType = getProjectedBlockType(player, new BlockVec(blockPos));
				
				if (projectedBlockType == null) {
					return;
				}
				
				event.setCancelled(true);
				
				if (digType == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
					packetHandler.refreshFakeBlock(player, blockPos, projectedBlockType);
				}
			}
		});
	}
	
	private void addBlockUpdateInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(
				new PacketAdapter(main, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE) {
					
					@Override
					public void onPacketSending(PacketEvent event) {
						
						PacketContainer packet = event.getPacket();
						Player player = event.getPlayer();
						
						if (packetHandler.isCustomPacket(packet) || !viewHandler.isViewingAPortal(player)) {
							return;
						}
						
						BlockPosition blockPos = packet.getBlockPositionModifier().read(0);
						BlockType projectedBlockType = getProjectedBlockType(player, new BlockVec(blockPos));
						
						if (projectedBlockType != null) {
							packet.getBlockData().write(0, projectedBlockType.getWrapped());
						}
					}
				}
		);
	}
	
	/**
	 * Intercepts multi block changes and edits any changed block data inside a portal animation back to the animation block data
	 */
	private void addMultiBlockUpdateInterception(ProtocolManager protocolManager) {
		
		protocolManager.addPacketListener(
				new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
					
					@Override
					public void onPacketSending(PacketEvent event) {
						
						PacketContainer packet = event.getPacket();
						Player player = event.getPlayer();
						
						//call the custom packet check first so the packet handler will definitely flush the packet from the list
						if (packetHandler.isCustomPacket(packet) || !viewHandler.isViewingAPortal(player)) {
							return;
						}
						
						Portal viewedPortal = viewHandler.getViewedPortal(player);
						ProjectionCache viewedCache = viewHandler.getViewedPortalSide(player);
						Map<BlockVec, BlockType> viewSession = viewHandler.getProjectedBlocks(player);
						
						if (VersionUtils.serverIsAtOrAbove("1.16.2")) {
							rewriteProjectionBlockTypes1_16_2(packet, viewedPortal, viewedCache, viewSession);
						} else {
							rewriteProjectionBlockTypes(packet, viewedPortal, viewedCache, viewSession);
						}
					}
				}
		);
	}
	
	private void rewriteProjectionBlockTypes(PacketContainer packet,
	                                         Portal viewedPortal,
	                                         ProjectionCache viewedCache,
	                                         Map<BlockVec, BlockType> viewSession) {
		
		ChunkCoordIntPair chunkLoc = packet.getChunkCoordIntPairs().read(0);
		int chunkWorldX = chunkLoc.getChunkX() << 4;
		int chunkWorldZ = chunkLoc.getChunkZ() << 4;
		
		Object[] blockInfoArray = packet.getMultiBlockChangeInfoArrays().getValues().get(0);
		
		for (Object object : blockInfoArray) {
			
			MultiBlockChangeInfo blockInfo = (MultiBlockChangeInfo) object;
			
			BlockVec blockPos = new BlockVec(
					blockInfo.getX() + chunkWorldX,
					blockInfo.getY(),
					blockInfo.getZ() + chunkWorldZ);
			
			if (getProjectedBlockType(blockPos, viewedPortal, viewedCache, viewSession) != null) {
				blockInfo.setData(viewSession.get(blockPos).getWrapped());
			}
		}
		
		packet.getMultiBlockChangeInfoArrays().write(0, Arrays.copyOf(blockInfoArray, blockInfoArray.length, MultiBlockChangeInfo[].class));
	}
	
	private void rewriteProjectionBlockTypes1_16_2(PacketContainer packet,
	                                               Portal viewedPortal,
	                                               ProjectionCache viewedCache,
	                                               Map<BlockVec, BlockType> viewSession) {
		
		//it's somehow a Object array and now a WrappedBlockData[] array, don't ask me
		Object[] blockTypes = packet.getBlockDataArrays().readSafely(0);
		short[] blockLocs = packet.getShortArrays().read(0);
		BlockVec chunkLoc = new BlockVec(packet.getSectionPositions().read(0)).multiply(16);
		int x = 0;
		
		for (int i = 0; i < blockLocs.length; i++) {
			
			BlockVec blockPos = new BlockVec(blockLocs[i]).add(chunkLoc);
			
			if (getProjectedBlockType(blockPos, viewedPortal, viewedCache, viewSession) != null) {
				blockTypes[i] = viewSession.get(blockPos).getWrapped();
				x++;
			}
		}
		
		//have to copy wrapped block data into WrappedBlockData[] manually here
		packet.getBlockDataArrays().write(0, Arrays.copyOf(blockTypes, blockTypes.length, WrappedBlockData[].class));
	}
	
	/**
	 * Returns the BlockType that is displayed the player in the projection at the block position.
	 * Returns null if no block is being displayed at the position.
	 */
	private BlockType getProjectedBlockType(BlockVec blockPos,
	                                        Portal viewedPortal,
	                                        ProjectionCache viewedCache,
	                                        Map<BlockVec, BlockType> viewSession) {
		
		return (viewedPortal.contains(blockPos) || viewedCache.contains(blockPos)) ? viewSession.get(blockPos) : null;
	}
	
	private BlockType getProjectedBlockType(Player player, BlockVec blockPos) {
		
		if (viewHandler.getViewedPortal(player).contains(blockPos) ||
		    viewHandler.getViewedPortalSide(player).contains(blockPos)) {
			
			return viewHandler.getProjectedBlocks(player).get(blockPos);
		}
		
		return null;
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
		
		Map<BlockVec, BlockType> viewSession = viewHandler.getProjectedBlocks(player);
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
		
		Map<BlockVec, BlockType> viewSession = viewHandler.getProjectedBlocks(player);
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
	
	//falling sand and maybe endermen (actually also sheep but that doesn't work)
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