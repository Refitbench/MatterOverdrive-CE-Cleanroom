package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.Reference;
import matteroverdrive.tile.TileEntityNewTritaniumCrate;
import matteroverdrive.util.MOLog;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public final class LegacyTritaniumCrateMigration {

	/**
	 * Forge data-fixer version tag for this mod. Chunks with a stored version >= this
	 * value have already had their tile-entity ids renamed and will not be processed again.
	 */
	private static final int DATA_VERSION = 1;

	/**
	 * Populated during {@link #onMissingBlockMappings}: old integer block id → color
	 * index (matching {@link matteroverdrive.blocks.BlockNewTritaniumCrate.Color} ordinals).
	 * Empty for worlds that never contained the legacy per-color blocks — every event
	 * handler starts with an {@code isEmpty()} guard, so the overhead is negligible.
	 */
	private static final Map<Integer, Integer> LEGACY_ID_TO_COLOR = new HashMap<>();

	private LegacyTritaniumCrateMigration() {}

	/**
	 * Registers the DataFixer (always) and, when migration is enabled in config,
	 * the event bus handlers. Must be called during pre-init after block registration.
	 */
	public static void init() {
		registerDataFixer();
		MinecraftForge.EVENT_BUS.register(LegacyTritaniumCrateMigration.class);
	}

	// -------------------------------------------------------------------------
	// DataFixer — runs on raw NBT before loadEntities(), once per chunk
	// -------------------------------------------------------------------------

	private static void registerDataFixer() {
		CompoundDataFixer fixer = FMLCommonHandler.instance().getDataFixer();
		ModFixs fixes = fixer.init(Reference.MOD_ID, DATA_VERSION);
		fixes.registerFix(FixTypes.CHUNK, new net.minecraft.util.datafix.IFixableData() {
			@Override
			public int getFixVersion() { return DATA_VERSION; }

			@Override
			@javax.annotation.Nonnull
			public NBTTagCompound fixTagCompound(@javax.annotation.Nonnull NBTTagCompound root) {
				NBTTagCompound level = root.getCompoundTag("Level");
				if (!level.hasKey("TileEntities", Constants.NBT.TAG_LIST)) return root;
				NBTTagList teList = level.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
				for (int i = 0; i < teList.tagCount(); i++) {
					NBTTagCompound te = teList.getCompoundTagAt(i);
					String id = te.getString("id");
					if (!id.startsWith("matteroverdrive:tritanium_crate")) continue;
					Integer colorIndex = resolveLegacyCrateColor(
							new net.minecraft.util.ResourceLocation(id).getPath());
					te.setString("id", "matteroverdrive:new_tritanium_crate");
					if (colorIndex != null && colorIndex != 0) {
						te.setInteger("Color", colorIndex);
					}
					MOLog.info("[LegacyMigration] Renamed TE '%s' -> new_tritanium_crate at (%d,%d,%d) color=%d",
							id, te.getInteger("x"), te.getInteger("y"), te.getInteger("z"),
							colorIndex != null ? colorIndex : 0);
				}
				return root;
			}
		});
	}

	// -------------------------------------------------------------------------
	// Event handlers — registered only when migration is enabled
	// -------------------------------------------------------------------------

	/**
	 * Intercepts Forge's "missing block" event for the old per-color crate registry
	 * names, suppresses their default warning output, and records each old integer
	 * block id → color index so {@link #onChunkDataLoad} can scan for them.
	 */
	@SubscribeEvent
	public static void onMissingBlockMappings(RegistryEvent.MissingMappings<Block> event) {
		if (!MatterOverdrive.CONFIG_HANDLER.legacyTritaniumCrateMigrationEnabled) return;
		for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getMappings()) {
			if (!Reference.MOD_ID.equals(mapping.key.getNamespace())) continue;
			String path = mapping.key.getPath();
			Integer colorIndex = resolveLegacyCrateColor(path);
			if (colorIndex == null) continue;
			LEGACY_ID_TO_COLOR.put(mapping.id, colorIndex);
			mapping.ignore();
			MOLog.info("[LegacyMigration] Old block matteroverdrive:%s (world id %d, color %d) queued for migration",
					path, mapping.id, colorIndex);
		}
	}

	/**
	 * On each chunk load, scans the raw block-id byte arrays for old crate ids,
	 * places {@code new_tritanium_crate} with the original facing directly into
	 * {@link ExtendedBlockStorage}, then re-adds the DataFixer-renamed TEs so
	 * inventories and colors are fully restored.
	 *
	 * <p>For worlds with no legacy blocks {@link #LEGACY_ID_TO_COLOR} is empty and
	 * this method returns after a single field check.
	 */
	@SubscribeEvent
	public static void onChunkDataLoad(ChunkDataEvent.Load event) {
		if (LEGACY_ID_TO_COLOR.isEmpty()) return;

        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        if (world == null) return; 

		NBTTagCompound level = event.getData().getCompoundTag("Level");
		NBTTagList sectionList = level.getTagList("Sections", Constants.NBT.TAG_COMPOUND);
		if (sectionList.tagCount() == 0) return;

		int chunkBlockX = chunk.x << 4;
		int chunkBlockZ = chunk.z << 4;
		int dimId = world.provider.getDimension();

		ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();
		boolean hasSkyLight = world.provider.hasSkyLight();

		Map<BlockPos, Integer> legacyPositions = new HashMap<>();

		for (int s = 0; s < sectionList.tagCount(); s++) {
			NBTTagCompound section = sectionList.getCompoundTagAt(s);
			if (!section.hasKey("Blocks")) continue;

			byte[] blockBytes = section.getByteArray("Blocks");
			byte[] addBytes   = section.hasKey("Add")  ? section.getByteArray("Add")  : null;
			// "Data" nibble array holds block metadata. MOBlock.getMetaFromState()
			// stored facing.getIndex() here, so getStateFromMeta(meta) restores facing.
			byte[] dataBytes  = section.hasKey("Data") ? section.getByteArray("Data") : null;
			int sectionY     = section.getByte("Y") & 0xFF;
			int sectionBaseY = sectionY << 4;

			for (int i = 0; i < 4096; i++) {
				int blockId = blockBytes[i] & 0xFF;
				if (addBytes != null) {
					blockId |= ((addBytes[i >> 1] >> ((i & 1) << 2)) & 0xF) << 8;
				}

				Integer colorIndex = LEGACY_ID_TO_COLOR.get(blockId);
				if (colorIndex == null) continue;

				int meta = dataBytes != null ? (dataBytes[i >> 1] >> ((i & 1) << 2)) & 0xF : 0;
				@SuppressWarnings("deprecation")
                IBlockState restoredState = MatterOverdrive.BLOCKS.new_tritanium_crate_base.getStateFromMeta(meta);

				int lx = i & 0xF;
				int lz = (i >> 4) & 0xF;
				int ly = (i >> 8) & 0xF;

				ExtendedBlockStorage storage = storageArray[sectionY];
				if (storage == null) {
					storage = new ExtendedBlockStorage(sectionBaseY, hasSkyLight);
					storageArray[sectionY] = storage;
				}
				storage.set(lx, ly, lz, restoredState);

				legacyPositions.put(new BlockPos(chunkBlockX + lx, sectionBaseY + ly, chunkBlockZ + lz), colorIndex);
			}
		}

		if (legacyPositions.isEmpty()) return;

		// Populate PENDING_COLORS before re-adding TEs. onLoad() will then consume
		// the entry, apply the color, and call markDirty() to send a TE sync packet
		// to the client — making the color visible without a manual update.
		for (Map.Entry<BlockPos, Integer> entry : legacyPositions.entrySet()) {
			BlockPos p = entry.getKey();
			TileEntityNewTritaniumCrate.PENDING_COLORS.put(
				TileEntityNewTritaniumCrate.pendingKey(dimId, p.getX(), p.getY(), p.getZ()),
				entry.getValue());
		}

		// Re-add the TEs. loadEntities() already ran but rejected them because their
		// positions were still air at that point. The block state is now correct so
		// addTileEntity() will accept them, restoring inventory data.
		NBTTagList teList = level.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
		for (int t = 0; t < teList.tagCount(); t++) {
			NBTTagCompound te = teList.getCompoundTagAt(t);
			if (!"matteroverdrive:new_tritanium_crate".equals(te.getString("id"))) continue;
			BlockPos tePos = new BlockPos(te.getInteger("x"), te.getInteger("y"), te.getInteger("z"));
			if (!legacyPositions.containsKey(tePos)) continue;
			TileEntity tileEntity = TileEntity.create(world, te);
			if (tileEntity != null) {
				chunk.addTileEntity(tileEntity);
				MOLog.info("[LegacyMigration] Restored crate TE at (%d,%d,%d) color=%d",
						tePos.getX(), tePos.getY(), tePos.getZ(), legacyPositions.get(tePos));
			}
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Maps an old block registry path (e.g. {@code tritanium_crate_red}) to a
	 * {@link matteroverdrive.blocks.BlockNewTritaniumCrate.Color} ordinal.
	 * Returns {@code null} if the path is not a recognized legacy crate block.
	 */
	private static Integer resolveLegacyCrateColor(String path) {
		if (!path.contains("tritanium")) return null;

		// Two-word color suffixes must be checked before the single-underscore split.
		if (path.endsWith("light_gray"))   return 7;
		if (path.endsWith("light_blue"))   return 12;
		if (path.endsWith("second_white")) return 15;

		int lastUs = path.lastIndexOf('_');
		String tail = lastUs >= 0 ? path.substring(lastUs + 1) : path;
		switch (tail) {
			case "crate": case "chest": case "base": case "storage": return 0;
			case "red":     return 1;
			case "green":   return 2;
			case "brown":   return 3;
			case "blue":    return 4;
			case "purple":  return 5;
			case "cyan":    return 6;
			case "silver":  return 7; // old dye name for light gray
			case "gray":    return 8;
			case "pink":    return 9;
			case "lime":    return 10;
			case "yellow":  return 11;
			case "magenta": return 13;
			case "orange":  return 14;
			case "black":   return 16;
			case "white":   return 17;
			default: break;
		}

		// Bare base name with no color suffix.
		if (path.endsWith("crate") || path.endsWith("chest") || path.endsWith("storage")) return 0;
		return null;
	}
}
