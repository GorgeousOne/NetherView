package me.gorgeousone.netherview.handlers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.PacketConstructor;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.utils.FacingUtils;
import me.gorgeousone.netherview.utils.NmsUtils;
import me.gorgeousone.netherview.utils.VersionUtils;
import me.gorgeousone.netherview.wrapper.WrappedBoundingBox;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
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
	
	private final ProtocolManager protocolManager;
	private final Set<Integer> markedPacketIds;
	
	public PacketHandler() {
		
		protocolManager = ProtocolLibrary.getProtocolManager();
		markedPacketIds = new HashSet<>();
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
	
	private void sendPacket(Player player, PacketContainer packet) {
		
		try {
			protocolManager.sendServerPacket(player, packet);
		} catch (InvocationTargetException e) {
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
		
		int[] entityIds = new int[entities.size()];
		int i = 0;
		
		for (Entity entity : entities) {
			entityIds[i] = entity.getEntityId();
			++i;
		}
		
		PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		destroyPacket.getIntegerArrays().write(0, entityIds);
		sendPacket(player, destroyPacket);
	}
	
	public void showEntities(Player player, Set<Entity> visibleEntities) {
		
		for (Entity entity : visibleEntities) {
			showEntity(player, entity, new Transform());
		}
	}
	
	public void showEntity(Player player,
	                       Entity entity,
	                       Transform transform) {
		
		if (entity == null || entity.isDead()) {
			return;
		}
		
		Location entityLoc = transform.transformLoc(entity.getLocation());
		
		try {
			
			switch (entity.getType()) {
				
				case EXPERIENCE_ORB:
					//I don't like fake experience orbs. They float around and are confusing only.
					return;
				
				case PAINTING:
					sendPacket(player, createPaintingPacket((Painting) entity, entityLoc, transform));
					break;
				
				case PLAYER:
					
					sendPacket(player, createPlayerPacket((HumanEntity) entity, entityLoc));
					sendPacket(player, createHeadRotation(entity, entityLoc.getYaw()));
					showEquipment(player, (LivingEntity) entity);
					break;
				
				default:
					
					if (entity instanceof LivingEntity) {
						
						sendPacket(player, createEntityLivingPacket((LivingEntity) entity, entityLoc));
						sendPacket(player, createHeadRotation(entity, entityLoc.getYaw()));
						showEquipment(player, (LivingEntity) entity);
						
					} else {
						sendPacket(player, createEntityPacket(entity, entityLoc));
					}
			}
			
			sendPacket(player, createMetadataPacket(entity));
			
		} catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to do nms stuff", e);
		}
	}
	
	private PacketContainer createHeadRotation(Entity entity,
	                                           float yaw) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
		
		PacketConstructor packetConstructor = protocolManager.createPacketConstructor(
				PacketType.Play.Server.ENTITY_HEAD_ROTATION,
				NmsUtils.getNmsClass("Entity"), byte.class);
		
		byte byteYaw = (byte) (int) (yaw * 265 / 360);
		return packetConstructor.createPacket(NmsUtils.getHandle(entity), byteYaw);
	}
	
	private PacketContainer createPlayerPacket(HumanEntity player,
	                                           Location entityLoc) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
		
		PacketConstructor packetConstructor = protocolManager.createPacketConstructor(PacketType.Play.Server.NAMED_ENTITY_SPAWN, NmsUtils.getNmsClass("EntityHuman"));
		PacketContainer spawnPacket = packetConstructor.createPacket(NmsUtils.getHandle(player));
		writeEntityPos(spawnPacket, entityLoc, true, false);
		return spawnPacket;
	}
	
	private PacketContainer createEntityPacket(Entity entity,
	                                           Location entityLoc) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
		
		PacketConstructor packetConstructor = protocolManager.createPacketConstructor(PacketType.Play.Server.SPAWN_ENTITY, NmsUtils.getNmsClass("Entity"));
		PacketContainer spawnPacket = packetConstructor.createPacket(NmsUtils.getHandle(entity));
		writeEntityPos(spawnPacket, entityLoc, false, false);
		return spawnPacket;
	}
	
	private PacketContainer createEntityLivingPacket(LivingEntity entity,
	                                                 Location entityLoc) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
		
		PacketConstructor packetConstructor = protocolManager.createPacketConstructor(PacketType.Play.Server.SPAWN_ENTITY_LIVING, NmsUtils.getNmsClass("EntityLiving"));
		PacketContainer spawnPacket = packetConstructor.createPacket(NmsUtils.getHandle(entity));
		writeEntityPos(spawnPacket, entityLoc, true, true);
		return spawnPacket;
	}
	
	private PacketContainer createPaintingPacket(Painting painting,
	                                             Location location,
	                                             Transform transform) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
		
		PacketConstructor packetConstructor = protocolManager.createPacketConstructor(PacketType.Play.Server.SPAWN_ENTITY_PAINTING, NmsUtils.getNmsClass("EntityPainting"));
		PacketContainer spawnPacket = packetConstructor.createPacket(NmsUtils.getHandle(painting));
		
		int halfHeight = (int) (WrappedBoundingBox.of(painting).getHeight() / 2);
		BlockPosition blockPosition = new BlockPosition(location.toVector()).subtract(new BlockPosition(0, halfHeight, 0));
		
		BlockFace rotatedFace = FacingUtils.getRotatedFace(painting.getFacing(), transform.getQuarterTurns());
		EnumWrappers.Direction rotatedDirection = FacingUtils.getBlockFaceToDirection(rotatedFace);
		
		spawnPacket.getBlockPositionModifier().write(0, blockPosition);
		spawnPacket.getDirections().write(0, rotatedDirection);
		
		return spawnPacket;
	}

	public void writeEntityPos(PacketContainer spawnPacket,
	                           Location entityLoc,
	                           boolean writeFacing,
	                           boolean writeHeadYaw) {
		
		if (VersionUtils.serverIsAtOrAbove("1.9.0")) {
			spawnPacket.getDoubles()
					.write(0, entityLoc.getX())
					.write(1, entityLoc.getY())
					.write(2, entityLoc.getZ());
		} else {
			spawnPacket.getIntegers()
					.write(0, entityLoc.getBlockX())
					.write(1, entityLoc.getBlockY())
					.write(2, entityLoc.getBlockZ());
		}
		
		if (!writeFacing) {
			return;
		}
		
		byte yawByte = (byte) (int) (entityLoc.getYaw() * 265 / 360);
		
		spawnPacket.getBytes()
				.write(0, yawByte)
				.write(1, (byte) (int) (entityLoc.getPitch() * 265 / 360));
		
		if (writeHeadYaw) {
			spawnPacket.getBytes().write(2, yawByte);
		}
	}
	
	public void sendEntityMoveLook(Player player,
	                               Entity entity,
	                               Vector relMove,
	                               double newYaw,
	                               double newPitch,
	                               boolean isOnGround) {
		
		PacketContainer moveLookPacket = protocolManager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
		
		moveLookPacket.getIntegers()
				.write(0, entity.getEntityId());
		
		if (VersionUtils.serverIsAtOrAbove("1.14.0")) {
			
			moveLookPacket.getShorts()
					.write(0, (short) (relMove.getX() * 4096))
					.write(1, (short) (relMove.getY() * 4096))
					.write(2, (short) (relMove.getZ() * 4096));
			
		} else {
			
			moveLookPacket.getIntegers()
					.write(1, (int) (relMove.getX() * 4096))
					.write(2, (int) (relMove.getY() * 4096))
					.write(3, (int) (relMove.getZ() * 4096));
		}
		
		moveLookPacket.getBytes()
				.write(0, (byte) (newYaw * 256 / 260))
				.write(1, (byte) (newPitch * 256 / 260));
		
		//no idea what field 1 does, seems to be always true
		moveLookPacket.getBooleans()
				.write(0, isOnGround)
				.write(1, true);
		
		PacketContainer headRotPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
		headRotPacket.getIntegers().write(0, entity.getEntityId());
		headRotPacket.getBytes().write(0, (byte) (int) (newYaw * 265 / 360));
		
		sendPacket(player, moveLookPacket);
		sendPacket(player, headRotPacket);
	}
	
	private PacketContainer createMetadataPacket(Entity entity) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
		
		PacketConstructor constructor = protocolManager.createPacketConstructor(PacketType.Play.Server.ENTITY_METADATA, int.class, NmsUtils.getNmsClass("DataWatcher"), boolean.class);
		PacketContainer metadataPacket = constructor.createPacket(entity.getEntityId(), NmsUtils.getDataWatcher(entity), true);
		return metadataPacket;
	}
	
	private void showEquipment(Player player, LivingEntity entity) {
		
		Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap = getEquipmentList(entity);
		
		if (VersionUtils.serverIsAtOrAbove("1.16.0")) {
			sendEquipment1_16(player, entity, equipmentMap);
			
		} else if (VersionUtils.serverIsAtOrAbove("1.9.0")) {
			sendEquipment(player, entity, equipmentMap);
			
		} else {
			sendEquipment1_8(player, entity, equipmentMap);
		}
	}
	
	private void sendEquipment1_8(Player player,
	                              Entity entity,
	                              Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap) {
		
		List<EnumWrappers.ItemSlot> itemSlots = new ArrayList<>(Arrays.asList(EnumWrappers.ItemSlot.values()));
		
		for (EnumWrappers.ItemSlot slot : equipmentMap.keySet()) {
			
			ItemStack item = equipmentMap.get(slot);
			
			if (item == null || item.getType() == Material.AIR) {
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
			
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			
			PacketContainer equipmentPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
			equipmentPacket.getIntegers().write(0, entity.getEntityId());
			equipmentPacket.getItemSlots().write(0, slot);
			equipmentPacket.getItemModifier().write(0, item);
			sendPacket(player, equipmentPacket);
		}
	}
	
	private void sendEquipment1_16(Player player,
	                               Entity entity,
	                               Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap) {
		
		List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipmentList = new ArrayList<>();
		
		for (EnumWrappers.ItemSlot slot : equipmentMap.keySet()) {
			
			ItemStack item = equipmentMap.get(slot);
			
			if (item == null || item.getType() == Material.AIR) {
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
		sendPacket(player, equipmentPacket);
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