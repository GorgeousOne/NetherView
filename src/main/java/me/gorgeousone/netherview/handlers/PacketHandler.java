package me.gorgeousone.netherview.handlers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handler class for creating and managing multi block change packets via ProtocolLib
 */
public class PacketHandler {
	
	private final ProtocolManager protocolManager;
	private final Set<Integer> customPacketIDs;
	
	public PacketHandler() {
		
		protocolManager = ProtocolLibrary.getProtocolManager();
		customPacketIDs = new HashSet<>();
	}
	
	/**
	 * Returns true if the packets system ID matches any packet's ID sent by nether view for viewing a portal.
	 * The method will delete matching packets from the custom packet list, so this method only works once!
	 */
	public boolean isCustomViewPacket(PacketContainer packet) {
		
		int packetID = System.identityHashCode(packet.getHandle());
		
		if (customPacketIDs.contains(packetID)) {
			customPacketIDs.remove(packetID);
			return true;
		}
		
		return false;
	}
	
	public void removeFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		World playerWorld = player.getWorld();
		Map<BlockVec, BlockType> updatedBlockCopies = new HashMap<>();
		
		for (BlockVec blockPos : blockCopies.keySet())
			updatedBlockCopies.put(blockPos.clone(), BlockType.of(blockPos.toBlock(playerWorld)));
		
		displayFakeBlocks(player, updatedBlockCopies);
	}
	
	public void displayFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockTypes = getSortedByChunks(blockCopies);
		
		for (Map.Entry<BlockVec, Map<BlockVec, BlockType>> chunkEntry : sortedBlockTypes.entrySet()) {
			
			BlockVec chunkPos = chunkEntry.getKey();
			
			PacketContainer fakeBlocksPacket = protocolManager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
			fakeBlocksPacket.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunkPos.getX(), chunkPos.getZ()));
			fakeBlocksPacket.getMultiBlockChangeInfoArrays().write(0, createBlockInfoArray(chunkEntry.getValue(), player.getWorld()));
			
			int packetID = System.identityHashCode(fakeBlocksPacket.getHandle());
			
			try {
				customPacketIDs.add(packetID);
				protocolManager.sendServerPacket(player, fakeBlocksPacket);
				
			} catch (InvocationTargetException e) {
				
				customPacketIDs.remove(packetID);
				throw new RuntimeException("Failed to send packet " + fakeBlocksPacket, e);
			}
		}
	}
	
	/**
	 * Returns the passed block copies sorted by their chunks so multi block change packets can be created with them.
	 */
	private Map<BlockVec, Map<BlockVec, BlockType>> getSortedByChunks(Map<BlockVec, BlockType> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockCopies = new HashMap<>();
		
		for (Map.Entry<BlockVec, BlockType> entry : blockCopies.entrySet()) {
			
			BlockVec blockPos = entry.getKey();
			BlockVec chunkPos = new BlockVec(blockPos.getX() >> 4, 0, blockPos.getZ() >> 4);
			
			sortedBlockCopies.putIfAbsent(chunkPos, new HashMap<>());
			sortedBlockCopies.get(chunkPos).put(blockPos, entry.getValue());
		}
		
		return sortedBlockCopies;
	}
	
	private MultiBlockChangeInfo[] createBlockInfoArray(Map<BlockVec, BlockType> blocksTypesInChunk, World world) {
		
		MultiBlockChangeInfo[] blockInfoArray = new MultiBlockChangeInfo[blocksTypesInChunk.size()];
		int i = 0;
		
		for (Map.Entry<BlockVec, BlockType> entry : blocksTypesInChunk.entrySet()) {
			
			Location blockLoc = entry.getKey().toLocation(world);
			blockInfoArray[i] = new MultiBlockChangeInfo(blockLoc, entry.getValue().getWrapped());
			i++;
		}
		
		return blockInfoArray;
	}
}