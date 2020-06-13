package me.gorgeousone.netherview.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utils for creating and sending multi block change packets via ProtocolLib
 */
public final class DisplayUtils {
	
	private DisplayUtils() {}
	
	public static PacketContainer removeFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		World playerWorld = player.getWorld();
		Map<BlockVec, BlockType> updatedBlockCopies = new HashMap<>();
		
		for (BlockVec blockPos : blockCopies.keySet())
			updatedBlockCopies.put(blockPos.clone(), BlockType.of(blockPos.toBlock(playerWorld)));
		
		return displayFakeBlocks(player, updatedBlockCopies);
	}
	
	public static PacketContainer displayFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		
		World playerWorld = player.getWorld();
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockTypes = getSortedByChunks(blockCopies);
		
		for (Map.Entry<BlockVec, Map<BlockVec, BlockType>> chunkEntry : sortedBlockTypes.entrySet()) {
			
			BlockVec chunkPos = chunkEntry.getKey();
			Map<BlockVec, BlockType> chunkBlockTypes = chunkEntry.getValue();
			
			//create an empty multi block change packet
			PacketContainer fakeBlocksPacket = protocolManager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
			fakeBlocksPacket.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunkPos.getX(), chunkPos.getZ()));
			
			MultiBlockChangeInfo[] blockInfoArray = new MultiBlockChangeInfo[chunkBlockTypes.size()];
			int i = 0;
			
			for (Map.Entry<BlockVec, BlockType> entry : chunkBlockTypes.entrySet()) {
				
				Location blockLoc = entry.getKey().toLocation(playerWorld);
				blockInfoArray[i] = new MultiBlockChangeInfo(blockLoc, entry.getValue().getWrapped());
				i++;
			}
			
			fakeBlocksPacket.getMultiBlockChangeInfoArrays().write(0, blockInfoArray);
			
			try {
				protocolManager.sendServerPacket(player, fakeBlocksPacket);
				return fakeBlocksPacket;
			
			} catch (InvocationTargetException e) {
				throw new RuntimeException("Failed to send packet " + fakeBlocksPacket, e);
			}
		}
		
		return null;
	}
	
	private static Map<BlockVec, Map<BlockVec, BlockType>> getSortedByChunks(Map<BlockVec, BlockType> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockCopies = new HashMap<>();
		
		for (Map.Entry<BlockVec, BlockType> entry : blockCopies.entrySet()) {
			
			BlockVec blockPos = entry.getKey();
			BlockVec chunkPos = new BlockVec(blockPos.getX() >> 4, 0, blockPos.getZ() >> 4);
			
			sortedBlockCopies.putIfAbsent(chunkPos, new HashMap<>());
			sortedBlockCopies.get(chunkPos).put(blockPos, entry.getValue());
		}
		
		return sortedBlockCopies;
	}
}
