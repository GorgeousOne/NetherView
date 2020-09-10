package me.gorgeousone.netherview.handlers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.utils.NmsUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import me.gorgeousone.netherview.wrapping.blocktype.BlockType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler class for creating and managing multi block change packets via ProtocolLib
 */
public class PacketHandler {
	
	private static Field FIELD_PLAYER_CONNECTION;
	private static Method METHOD_PLAYER_CONNECTION_SEND_PACKET;
	
	private static Constructor PACKET_SPAWN_EX_ORB;
	private static Constructor PACKET_SPAWN_PAINTING;
	private static Constructor PACKET_SPAWN_PLAYER;
	private static Constructor PACKET_SPAWN_ENTITY_LIVING;
	private static Constructor PACKET_SPAWN_ENTITY;
	
	private static Method METHOD_ENTITY_GET_DATA_WATCHER;
	private static Method METHOD_DATA_WATCHER_A;
	private static Constructor PACKET_ENTITY_METADATA;
	
	static {
		
		try {
			FIELD_PLAYER_CONNECTION = NmsUtils.getNmsClass("EntityPlayer").getField("playerConnection");
			METHOD_PLAYER_CONNECTION_SEND_PACKET = NmsUtils.getNmsClass("PlayerConnection").getMethod("sendPacket", NmsUtils.getNmsClass("Packet"));
			
			Class nmsEntityClass = NmsUtils.getNmsClass("Entity");
			PACKET_SPAWN_EX_ORB = NmsUtils.getNmsClass("PacketPlayOutSpawnEntityExperienceOrb").getConstructor(NmsUtils.getNmsClass("EntityExperienceOrb"));
			PACKET_SPAWN_PAINTING = NmsUtils.getNmsClass("PacketPlayOutSpawnEntityPainting").getConstructor(NmsUtils.getNmsClass("EntityPainting"));
			PACKET_SPAWN_PLAYER = NmsUtils.getNmsClass("PacketPlayOutNamedEntitySpawn").getConstructor(NmsUtils.getNmsClass("EntityHuman"));
			PACKET_SPAWN_ENTITY_LIVING = NmsUtils.getNmsClass("PacketPlayOutSpawnEntityLiving").getConstructor(NmsUtils.getNmsClass("EntityLiving"));
			PACKET_SPAWN_ENTITY = NmsUtils.getNmsClass("PacketPlayOutSpawnEntity").getConstructor(nmsEntityClass, int.class);
			
			Class DATA_WATCHER = NmsUtils.getNmsClass("DataWatcher");
			METHOD_DATA_WATCHER_A = DATA_WATCHER.getMethod("a");
			METHOD_ENTITY_GET_DATA_WATCHER = nmsEntityClass.getMethod("getDataWatcher");
			PACKET_ENTITY_METADATA = NmsUtils.getNmsClass("PacketPlayOutEntityMetadata").getConstructor(int.class, DATA_WATCHER, boolean.class);
			
		} catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}
	
	private final ProtocolManager protocolManager;
	private final Set<Integer> markedPacketIds;
	
	public PacketHandler() {
		
		protocolManager = ProtocolLibrary.getProtocolManager();
		markedPacketIds = new HashSet<>();
	}
	
	public static void sendPacket(Player player, Object packet) {
		
		try {
			Object nmsPlayer = NmsUtils.getHandle(player);
			Object connection = FIELD_PLAYER_CONNECTION.get(nmsPlayer);
			METHOD_PLAYER_CONNECTION_SEND_PACKET.invoke(connection, packet);
			
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends the packet and adds it's system ID to a set of custom packets sent from this class.
	 * Packet listener of this project can check (only once) if the packets they receive are custom packets which should not be altered.
	 */
	private void sendCustomPacket(Player player, PacketContainer packet) {
		
		int packetId = System.identityHashCode(packet.getHandle());
		
		try {
			markedPacketIds.add(packetId);
			protocolManager.sendServerPacket(player, packet);
			
		} catch (InvocationTargetException e) {
			
			markedPacketIds.remove(packetId);
			throw new RuntimeException("Failed to send packet " + packet, e);
		}
	}
	
	/**
	 * Returns true if the packets system ID matches any custom packet sent by this project for viewing a portal.
	 * The method will delete matching packets from the custom packet set, so this method only works once!
	 */
	public boolean isCustomPacket(PacketContainer packet) {
		
		int packetId = System.identityHashCode(packet.getHandle());
		return markedPacketIds.contains(packetId);
	}
	
	public void refreshFakeBlock(Player player, BlockPosition blockPos, BlockType projectedBlockType) {
		
		PacketContainer fakeBlockPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
		
		fakeBlockPacket.getBlockPositionModifier().write(0, blockPos);
		fakeBlockPacket.getBlockData().write(0, projectedBlockType.getWrapped());
		
		sendCustomPacket(player, fakeBlockPacket);
	}
	
	public void removeFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		World playerWorld = player.getWorld();
		Map<BlockVec, BlockType> updatedBlockCopies = new HashMap<>();
		
		for (BlockVec blockPos : blockCopies.keySet())
			updatedBlockCopies.put(blockPos.clone(), BlockType.of(blockPos.toBlock(playerWorld)));
		
		displayFakeBlocks(player, updatedBlockCopies);
	}
	
	public void displayFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		if (VersionUtils.serverIsAtOrAbove("1.16.2")) {
			sendMultipleFakeBlocks1_16_2(player, blockCopies);
		} else {
			sendMultipleFakeBlocks(player, blockCopies);
		}
	}
	
	private void sendMultipleFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockTypes = getSortedByChunks(blockCopies);
		
		for (Map.Entry<BlockVec, Map<BlockVec, BlockType>> chunkEntry : sortedBlockTypes.entrySet()) {
			
			BlockVec chunkPos = chunkEntry.getKey();
			PacketContainer fakeBlocksPacket = protocolManager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
			
			fakeBlocksPacket.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunkPos.getX(), chunkPos.getZ()));
			fakeBlocksPacket.getMultiBlockChangeInfoArrays().write(0, createBlockInfoArray(chunkEntry.getValue(), player.getWorld()));
			sendCustomPacket(player, fakeBlocksPacket);
		}
	}
	
	private void sendMultipleFakeBlocks1_16_2(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockTypes = getSortedBy16x16x16(blockCopies);
		
		for (BlockVec chunkPos : sortedBlockTypes.keySet()) {
			
			Map<BlockVec, BlockType> blockInChunk = sortedBlockTypes.get(chunkPos);
			PacketContainer fakeBlocksPacket = protocolManager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
			
			fakeBlocksPacket.getSectionPositions().write(0, chunkPos.toBlockPos());
			fakeBlocksPacket.getShortArrays().write(0, createChunkLocsArray1_16_2(blockInChunk.keySet()));
			fakeBlocksPacket.getBlockDataArrays().write(0, createBlockInfoArray1_16_2(blockInChunk.values()));
			sendCustomPacket(player, fakeBlocksPacket);
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
	
	/**
	 * Returns the passed block copies sorted by their chunks so multi block change packets can be created with them.
	 */
	private Map<BlockVec, Map<BlockVec, BlockType>> getSortedBy16x16x16(Map<BlockVec, BlockType> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockCopies = new HashMap<>();
		
		for (Map.Entry<BlockVec, BlockType> entry : blockCopies.entrySet()) {
			
			BlockVec blockPos = entry.getKey();
			BlockVec cubePos = new BlockVec(blockPos.getX() >> 4, blockPos.getY() >> 4, blockPos.getZ() >> 4);
			
			sortedBlockCopies.putIfAbsent(cubePos, new HashMap<>());
			sortedBlockCopies.get(cubePos).put(blockPos, entry.getValue());
		}
		
		return sortedBlockCopies;
	}
	
	/**
	 * Creates an array of MultiBlockChangeInfos each representing the location and block data for a block in a
	 * MultiBlockChangePacket.
	 */
	private MultiBlockChangeInfo[] createBlockInfoArray(Map<BlockVec, BlockType> blocksTypesInChunk, World world) {
		
		MultiBlockChangeInfo[] blockInfoArray = new MultiBlockChangeInfo[blocksTypesInChunk.size()];
		int i = 0;
		
		for (Map.Entry<BlockVec, BlockType> entry : blocksTypesInChunk.entrySet()) {
			
			Location blockLoc = entry.getKey().toLocation(world);
			blockInfoArray[i] = new MultiBlockChangeInfo(blockLoc, entry.getValue().getWrapped());
			++i;
		}
		
		return blockInfoArray;
	}
	
	/**
	 * Creates an array of WarppedBlockData each representing the block data for a block in a
	 * MultiBlockChangePacket (1.16.2+).
	 */
	private WrappedBlockData[] createBlockInfoArray1_16_2(Collection<BlockType> blocksTypesInChunk) {
		
		WrappedBlockData[] blockInfoArray = new WrappedBlockData[blocksTypesInChunk.size()];
		int i = 0;
		
		for (BlockType blockType : blocksTypesInChunk) {
			blockInfoArray[i] = blockType.getWrapped();
			++i;
		}
		
		return blockInfoArray;
	}
	
	/**
	 * Creates an array of shorts each representing a block's location relative to it's chunk for the the
	 * MultiBlockChangePacket (1.16.2+).
	 */
	private short[] createChunkLocsArray1_16_2(Collection<BlockVec> blockLocsInChunk) {
		
		short[] chunkLocs = new short[blockLocsInChunk.size()];
		int i = 0;
		
		for (BlockVec loc : blockLocsInChunk) {
			chunkLocs[i] = loc.toChunkShort();
			++i;
		}
		
		return chunkLocs;
	}
	
	public void hideEntities(Player player, Set<Entity> entities) {
		
		if (entities.isEmpty()) {
			return;
		}
		
		player.sendMessage(ChatColor.GRAY  + "hide " + entities.size());
		
		int[] entityIds = new int[entities.size()];
		int i = 0;
		
		for (Entity entity : entities) {
			entityIds[i] = entity.getEntityId();
			++i;
		}
		
		PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		destroyPacket.getIntegerArrays().write(0, entityIds);
		
		try {
			protocolManager.sendServerPacket(player, destroyPacket);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Failed to send packet " + destroyPacket, e);
		}
	}

//	public void showEntities(Player player, Set<Entity> entities) {
//
//		for (Entity entity : entities) {
//			protocolManager.updateEntity(entity, Collections.singletonList(player));
//		}
//	}
	
	public void showEntities(Player player, Set<Entity> visibleEntities) {
		
		if (visibleEntities.isEmpty()) {
			return;
		}
		
		try {
			
			for (Entity entity : visibleEntities) {
				
				if (entity == null || !entity.isValid()) {
					continue;
				}
				
				Object spawnPacket;
				boolean entityHasEquipment = false;
				Object nmsEntity = NmsUtils.getHandle(entity);
				
				if (entity.getType() == EntityType.PAINTING) {
					spawnPacket = PACKET_SPAWN_PAINTING.newInstance(nmsEntity);
					
				} else if (entity.getType() == EntityType.EXPERIENCE_ORB) {
					spawnPacket = PACKET_SPAWN_EX_ORB.newInstance(nmsEntity);
					
				} else if (entity instanceof LivingEntity) {
					
					if (entity.getType() == EntityType.PLAYER) {
						spawnPacket = PACKET_SPAWN_PLAYER.newInstance(nmsEntity);
					} else {
						spawnPacket = PACKET_SPAWN_ENTITY_LIVING.newInstance(nmsEntity);
					}
					
					entityHasEquipment = true;
					
				} else {
					spawnPacket = PACKET_SPAWN_ENTITY.newInstance(nmsEntity, entity.getEntityId());
				}
				
				sendPacket(player, spawnPacket);
				updateEntity(player, entity, nmsEntity);

				if (entityHasEquipment) {
					showEquipment(player, (LivingEntity) entity);
				}
			}
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	private void updateEntity(Player player,
	                          Entity bukkitEntity,
	                          Object nmsEntity) throws IllegalAccessException, InvocationTargetException, InstantiationException {
		
		Object dataWatcher = METHOD_ENTITY_GET_DATA_WATCHER.invoke(nmsEntity);
		player.sendMessage(ChatColor.DARK_GRAY + bukkitEntity.getType().name().toLowerCase() + " updated");

		if ((boolean) METHOD_DATA_WATCHER_A.invoke(dataWatcher)) {
			
			Object metadataPacket = PACKET_ENTITY_METADATA.newInstance(bukkitEntity.getEntityId(), dataWatcher, true);
			sendPacket(player, metadataPacket);
		}
	}

	private void showEquipment(Player player, LivingEntity entity) {

		Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap = getEquipmentList(entity);

		if (VersionUtils.serverIsAtOrAbove("1.16.0")) {
			sendEquipment_1_16(player, entity, equipmentMap);

		} else if (VersionUtils.serverIsAtOrAbove("1.9.0")) {
			sendEquipment(player, entity, equipmentMap);

		} else {
			sendEquipment_1_8(player, entity, equipmentMap);
		}
	}

	private void sendEquipment_1_8(Player player,
	                               Entity entity,
	                               Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap) {

		List<EnumWrappers.ItemSlot> itemSlots = new ArrayList<>(Arrays.asList(EnumWrappers.ItemSlot.values()));

		for (EnumWrappers.ItemSlot slot : equipmentMap.keySet()) {

			ItemStack item = equipmentMap.get(slot);

			if (item.getType() == Material.AIR) {
				continue;
			}

			PacketContainer equipmentPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
			equipmentPacket.getIntegers()
					.write(0, entity.getEntityId())
					.write(1, itemSlots.indexOf(slot) - 1);

			equipmentPacket.getItemModifier().write(0, item);
			sendPacket(player, equipmentPacket);
		}
	}

	private void sendEquipment(Player player,
	                           Entity entity,
	                           Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap) {

		for (EnumWrappers.ItemSlot slot : equipmentMap.keySet()) {

			ItemStack item = equipmentMap.get(slot);

			if (item.getType() == Material.AIR) {
				continue;
			}

			PacketContainer equipmentPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
			equipmentPacket.getIntegers().write(0, entity.getEntityId());
			equipmentPacket.getItemSlots().write(0, slot);
			equipmentPacket.getItemModifier().write(0, item);
			sendCustomPacket(player, equipmentPacket);
		}
	}

	private void sendEquipment_1_16(Player player,
	                                Entity entity,
	                                Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap) {

		List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipmentList = new ArrayList<>();

		for (EnumWrappers.ItemSlot slot : equipmentMap.keySet()) {

			ItemStack item = equipmentMap.get(slot);

			if (item.getType() == Material.AIR) {
				continue;
			}

			equipmentList.add(new Pair<>(slot, item));
		}

		if (equipmentList.isEmpty()) {
			return;
		}

		PacketContainer equipmentPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
		equipmentPacket.getIntegers().write(0, entity.getEntityId());
		equipmentPacket.getSlotStackPairLists().write(0, equipmentList);
		sendCustomPacket(player, equipmentPacket);
	}

	public Map<EnumWrappers.ItemSlot, ItemStack> getEquipmentList(LivingEntity entity) {

		EntityEquipment equipment = entity.getEquipment();
		Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap = new HashMap<>();

		if (VersionUtils.serverIsAtOrAbove("1.9.0")) {
			equipmentMap.put(EnumWrappers.ItemSlot.MAINHAND, equipment.getItemInMainHand());
			equipmentMap.put(EnumWrappers.ItemSlot.OFFHAND, equipment.getItemInOffHand());

		} else {
			equipmentMap.put(EnumWrappers.ItemSlot.OFFHAND, equipment.getItemInHand());
		}

		equipmentMap.put(EnumWrappers.ItemSlot.FEET, equipment.getBoots());
		equipmentMap.put(EnumWrappers.ItemSlot.LEGS, equipment.getLeggings());
		equipmentMap.put(EnumWrappers.ItemSlot.CHEST, equipment.getChestplate());
		equipmentMap.put(EnumWrappers.ItemSlot.HEAD, equipment.getHelmet());
		return equipmentMap;
	}
}