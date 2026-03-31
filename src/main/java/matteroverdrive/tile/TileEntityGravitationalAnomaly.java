
package matteroverdrive.tile;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;

import org.apache.logging.log4j.Level;
import org.lwjgl.util.vector.Vector3f;

import matteroverdrive.api.IScannable;
import matteroverdrive.api.events.anomaly.MOEventGravitationalAnomalyConsume;
import matteroverdrive.api.gravity.AnomalySuppressor;
import matteroverdrive.api.gravity.IGravitationalAnomaly;
import matteroverdrive.api.gravity.IGravityEntity;
import matteroverdrive.client.sound.GravitationalAnomalySound;
import matteroverdrive.entity.player.MOPlayerCapabilityProvider;
import matteroverdrive.fx.GravitationalAnomalyParticle;
import matteroverdrive.init.MatterOverdriveSounds;
import matteroverdrive.init.OverdriveBioticStats;
import matteroverdrive.items.SpacetimeEqualizer;
import matteroverdrive.machines.MachineNBTCategory;
import matteroverdrive.util.MOLog;
import matteroverdrive.util.MatterHelper;
import matteroverdrive.util.Vector3;
import matteroverdrive.util.math.MOMathHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityGravitationalAnomaly extends MOTileEntity implements IScannable, IGravitationalAnomaly
{
    public static boolean FALLING_BLOCKS = true;
    public static boolean BLOCK_ENTETIES = true;
    public static boolean VANILLA_FLUIDS = true;
    public static boolean FORGE_FLUIDS = true;
    public static boolean BLOCK_DESTRUCTION = true;
    public static boolean GRAVITATION = true;
    public static boolean SOUND = true;
    public static final float MAX_VOLUME = 0.5f;
    public static int SCAN_BATCH_SIZE = 256;
    public static int BLOCKS_PER_BATCH = 1;
    public static int BATCH_TICK_RATE = 2;
    public static int ENTITY_SCAN_RATE = 10;
    public static int IDLE_SCAN_TICKS = 40;
    public static int SCAN_BAND_INNER_PCT = 15; // % of SCAN_BATCH_SIZE scanned per tick for inner band
    public static int SCAN_BAND_MID_PCT   = 30; // % of SCAN_BATCH_SIZE scanned per tick for mid band
                                                // outer band gets the remaining (100 - inner - mid)%
    public static final double STREHGTH_MULTIPLYER = 0.00001;
    public static final double G = 6.67384;
    public static final double G2 = G * 2;
    public static final double C = 2.99792458;
    public static final double CC = C * C;

    private static final Map<Integer, List<BlockPos>> SPHERE_OFFSET_CACHE = new HashMap<>();

    @SideOnly(Side.CLIENT)
    private GravitationalAnomalySound sound;
    private long mass;
    List<AnomalySuppressor> supressors;
    private float suppression = 1.0f;
    private boolean derivedMassCacheDirty = true;
    private double cachedRealMassUnsuppressed;
    private double cachedRealMass;
    private float cachedBaseBreakStrength;
    private AxisAlignedBB cachedGravitationBB;
    private double cachedRangeSq;
    private Vec3d cachedCenter;

    private BlockPos blockPos;
    private int scanRange = -1;
    private int breakBatchTimer = 0;
    private List<Entity> cachedEntityList = Collections.emptyList();
    private int entityScanTimer = 0;
    private List<BlockPos> currentOffsets = Collections.emptyList();
    // band scanner state: A=inner(0-25%), B=mid(25-55%), C=outer(55-100%)
    private int bandSplitA = 0;
    private int bandSplitB = 0;
    private int bandCursorA = 0;
    private int bandCursorB = 0;
    private int bandCursorC = 0;
    private int bandIdleA = 0;
    private int bandIdleB = 0;
    private int bandIdleC = 0;
    private List<ScanEntry> bandBufferA = new ArrayList<>();
    private List<ScanEntry> bandBufferB = new ArrayList<>();
    private List<ScanEntry> bandBufferC = new ArrayList<>();
    private Queue<ScanEntry> bandQueueA = new ArrayDeque<>();
    private Queue<ScanEntry> bandQueueB = new ArrayDeque<>();
    private Queue<ScanEntry> bandQueueC = new ArrayDeque<>();

    //region Constructors
    public TileEntityGravitationalAnomaly()
    {
        this.mass = 2048 + Math.round(Math.random() * 8192);
        supressors = new ArrayList<>();
    }

    public TileEntityGravitationalAnomaly(int mass)
    {
        this();
        this.mass = mass;
    }
    //endregion

    //region Updates
    @Override
    public void update()
    {
        if (world.isRemote)
        {
            spawnParticles(world);
            manageSound();
            manageClientEntityGravitation(world);
        }
        else
        {
            if (!supressors.isEmpty()) {
                float tmpSuppression = calculateSuppression();
                setSuppression(tmpSuppression);
            }

            manageEntityGravitation(world);
            scanBlockLayer(world);
            if (++breakBatchTimer >= BATCH_TICK_RATE) {
                breakBatchTimer = 0;
                breakNextQueuedBlocks(world);
            }
        }
    }
    //endregion

	@SideOnly(Side.CLIENT)
	public void spawnParticles(World world) {
		double radius = (float) getBlockBreakRange();
		Vector3f point = MOMathHelper.randomSpherePoint(getPos().getX() + 0.5, getPos().getY() + 0.5,
				getPos().getZ() + 0.5, new Vec3d(radius, radius, radius), world.rand);
		GravitationalAnomalyParticle particle = new GravitationalAnomalyParticle(world, point.x, point.y, point.z,
				new Vec3d(getPos().getX() + 0.5f, getPos().getY() + 0.5f, getPos().getZ() + 0.5f));
		Minecraft.getMinecraft().effectRenderer.addEffect(particle);
	}

	@SideOnly(Side.CLIENT)
	public void manageClientEntityGravitation(World world) {
		if (!GRAVITATION) {
			return;
		}

		double rangeSq = getMaxRange() + 1;
		rangeSq *= rangeSq;
		Vec3d blockPos = new Vec3d(getPos());
		blockPos.add(0.5, 0.5, 0.5);
		Vec3d entityPos = Minecraft.getMinecraft().player.getPositionVector();

		double distanceSq = entityPos.squareDistanceTo(blockPos);
		if (distanceSq < rangeSq) {
			if ((!Minecraft.getMinecraft().player.inventory.armorItemInSlot(2).isEmpty()
					&& Minecraft.getMinecraft().player.inventory.armorItemInSlot(2)
					.getItem() instanceof SpacetimeEqualizer)
					|| Minecraft.getMinecraft().player.capabilities.isCreativeMode
					|| Minecraft.getMinecraft().player.isSpectator()
					|| MOPlayerCapabilityProvider.GetAndroidCapability(Minecraft.getMinecraft().player)
					.isUnlocked(OverdriveBioticStats.equalizer, 0))
				return;

			double acceleration = getAcceleration(distanceSq);
			Vec3d dir = blockPos.subtract(entityPos).normalize();
			Minecraft.getMinecraft().player.addVelocity(dir.x * acceleration, dir.y * acceleration,
					dir.z * acceleration);
			Minecraft.getMinecraft().player.velocityChanged = true;
		}
	}


	public void manageEntityGravitation(World world) {
		if (!GRAVITATION) return;
		if (cachedGravitationBB == null) return;

		double eventHorizon = getEventHorizon(); // ensures cache is fresh
		if (++entityScanTimer >= ENTITY_SCAN_RATE) {
			entityScanTimer = 0;
			cachedEntityList = world.getEntitiesWithinAABB(Entity.class, cachedGravitationBB);
		}

		for (Entity entity : cachedEntityList) {
			if (entity.isDead) continue;
			if (entity instanceof IGravityEntity) {
				if (!((IGravityEntity) entity).isAffectedByAnomaly(this)) continue;
			}

			Vec3d entityPos = entity.getPositionVector();
			double distanceSq = entityPos.squareDistanceTo(cachedCenter);
			if (distanceSq > cachedRangeSq) continue; // sphere cull: skip cube-corner entities

			double acceleration = getAcceleration(distanceSq);
			Vec3d scaledDir = cachedCenter.subtract(entityPos).normalize();
			scaledDir = new Vec3d(scaledDir.x * acceleration, scaledDir.y * acceleration, scaledDir.z * acceleration);

			if (intersectsAnomaly(entityPos, scaledDir, cachedCenter, eventHorizon)) {
				consume(entity);
			}

			applyGravitationToEntity(entity, scaledDir);
		}
	}

	private void applyGravitationToEntity(Entity entity, Vec3d scaledDir) {
		if (entity instanceof EntityPlayer) return; // Players handle velocity clientside

		if (entity instanceof EntityLivingBase) {
			boolean hasEqualizer = false;
			for (ItemStack i : entity.getArmorInventoryList()) {
				if (!i.isEmpty() && i.getItem() instanceof SpacetimeEqualizer) {
					hasEqualizer = true;
					break;
				}
			}
			if (hasEqualizer) return;
		}

		entity.addVelocity(scaledDir.x, scaledDir.y, scaledDir.z);
	}

	boolean intersectsAnomaly(Vec3d origin, Vec3d dir, Vec3d anomaly, double radius) {
		if (origin.distanceTo(anomaly) <= radius) {
			return true;
		} else {
			Vec3d intersectDir = origin.subtract(anomaly);
			double c = intersectDir.length();
			double v = intersectDir.dotProduct(dir);
			double d = radius * radius - (c * c - v * v);

			return d >= 0;
		}
	}

    //region Sounds
    @SideOnly(Side.CLIENT)
    public void stopSounds()
    {
        if (sound != null)
        {
            sound.stopPlaying();
            FMLClientHandler.instance().getClient().getSoundHandler().stopSound(sound);
            sound = null;
        }
    }

	@SideOnly(Side.CLIENT)
	public void playSounds() {
		if (sound == null) {
			sound = new GravitationalAnomalySound(MatterOverdriveSounds.windy, SoundCategory.BLOCKS, getPos(), 0.2f,
					getMaxRange());
			FMLClientHandler.instance().getClient().getSoundHandler().playSound(sound);
		} else if (!FMLClientHandler.instance().getClient().getSoundHandler().isSoundPlaying(sound)) {
			stopSounds();
			sound = new GravitationalAnomalySound(MatterOverdriveSounds.windy, SoundCategory.BLOCKS, getPos(), 0.2f,
					getMaxRange());
			FMLClientHandler.instance().getClient().getSoundHandler().playSound(sound);
		}
	}

    @SideOnly(Side.CLIENT)
    public void manageSound()
    {
        if (sound == null)
        {
            playSounds();
        }else
        {
            sound.setVolume(Math.min(MAX_VOLUME,getBreakStrength(0,(float)getMaxRange()) * 0.1f));
            sound.setRange(getMaxRange());
        }
    }
    //endregion

    //region Super Events
	@Override
	public void onAdded(World world, BlockPos pos, IBlockState state) {

	}

    @Override
    public void onPlaced(World world, EntityLivingBase entityLiving) {

    }

	@Override
	public void onDestroyed(World worldIn, BlockPos pos, IBlockState state) {

	}

	@Override
	public void onNeighborBlockChange(IBlockAccess world, BlockPos pos, IBlockState state, Block neighborBlock) {

	}

    @Override
    public void writeToDropItem(ItemStack itemStack) {

    }

    @Override
    public void readFromPlaceItem(ItemStack itemStack) {

    }

    @Override
    public void onScan(World world, double x, double y, double z, EntityPlayer player, ItemStack scanner) {

    }

	public void onChunkUnload() {
		super.onChunkUnload();
		if (world.isRemote) {
			stopSounds();
		}
	}

    @Override
    protected void onAwake(Side side) {

    }

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound syncData = new NBTTagCompound();
		writeCustomNBT(syncData, MachineNBTCategory.ALL_OPTS, false);
		return new SPacketUpdateTileEntity(getPos(), 1, syncData);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound syncData = pkt.getNbtCompound();
		if (syncData != null) {
			readCustomNBT(syncData, MachineNBTCategory.ALL_OPTS);
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (world.isRemote) {
			stopSounds();
		}
	}
    //endregion

    //region Events
	private boolean onEntityConsume(Entity entity, boolean pre) {
		if (entity instanceof IGravityEntity) {
			((IGravityEntity) entity).onEntityConsumed(this);
		}
		if (pre) {
			MinecraftForge.EVENT_BUS.post(new MOEventGravitationalAnomalyConsume.Pre(entity, getPos()));
		} else {
			MinecraftForge.EVENT_BUS.post(new MOEventGravitationalAnomalyConsume.Post(entity, getPos()));
		}

		return true;
	}
    //endregion

	private static List<BlockPos> buildSphereOffsets(int range) {
		return SPHERE_OFFSET_CACHE.computeIfAbsent(range, r -> {
			List<BlockPos> offsets = new ArrayList<>();
			int rangeSq = r * r;
			for (int x = -r; x <= r; x++) {
				int xx = x * x;
				for (int z = -r; z <= r; z++) {
					int xxzz = xx + z * z;
					if (xxzz > rangeSq) continue;
					for (int y = -r; y <= r; y++) {
						if (xxzz + y * y <= rangeSq) {
							offsets.add(new BlockPos(x, y, z));
						}
					}
				}
			}
			offsets.sort((a, b) -> {
				int da = a.getX() * a.getX() + a.getY() * a.getY() + a.getZ() * a.getZ();
				int db = b.getX() * b.getX() + b.getY() * b.getY() + b.getZ() * b.getZ();
				return Integer.compare(da, db);
			});
			return Collections.unmodifiableList(offsets);
		});
	}

	private void scanBlockLayer(World world) {
		if (!BLOCK_DESTRUCTION) return;

		int range = (int) Math.floor(getBlockBreakRange());
		if (range != scanRange) {
			scanRange = range;
			currentOffsets = buildSphereOffsets(range);
			int sz = currentOffsets.size();
			double innerLimitSq = range * 0.25 * range * 0.25;
			double midLimitSq   = range * 0.55 * range * 0.55;
			bandSplitA = 0;
			for (int i = 0; i < sz; i++) {
				BlockPos o = currentOffsets.get(i);
				int dSq = o.getX() * o.getX() + o.getY() * o.getY() + o.getZ() * o.getZ();
				if (dSq <= innerLimitSq) bandSplitA = i + 1; else break;
			}
			bandSplitB = bandSplitA;
			for (int i = bandSplitA; i < sz; i++) {
				BlockPos o = currentOffsets.get(i);
				int dSq = o.getX() * o.getX() + o.getY() * o.getY() + o.getZ() * o.getZ();
				if (dSq <= midLimitSq) bandSplitB = i + 1; else break;
			}
			bandCursorA = 0;         bandIdleA = 0; bandBufferA.clear(); bandQueueA.clear();
			bandCursorB = bandSplitA; bandIdleB = 0; bandBufferB.clear(); bandQueueB.clear();
			bandCursorC = bandSplitB; bandIdleC = 0; bandBufferC.clear(); bandQueueC.clear();
		}
		if (range <= 0) return;

		List<BlockPos> offsets = currentOffsets;
		int size = offsets.size();
		if (size == 0) return;

		double eventHorizon = getEventHorizon();
		int ax = getPos().getX(), ay = getPos().getY(), az = getPos().getZ();
		int innerBatch = Math.max(1, SCAN_BATCH_SIZE * SCAN_BAND_INNER_PCT / 100);
		int midBatch   = Math.max(1, SCAN_BATCH_SIZE * SCAN_BAND_MID_PCT / 100);
		int outerBatch = Math.max(1, SCAN_BATCH_SIZE * (100 - SCAN_BAND_INNER_PCT - SCAN_BAND_MID_PCT) / 100);
		int midIdle    = Math.max(1, IDLE_SCAN_TICKS / 2);  // mid re-scans at half rate
		int outerIdle  = IDLE_SCAN_TICKS;                   // outer uses full idle

		// Band A: inner [0, bandSplitA) — no idle, scans continuously
		if (bandSplitA > 0) {
			int end = Math.min(bandCursorA + innerBatch, bandSplitA);
			scanOffsetRange(offsets, bandCursorA, end, ax, ay, az, eventHorizon, world, bandBufferA, range);
			bandCursorA = end;
			if (bandCursorA >= bandSplitA) {
				if (!bandBufferA.isEmpty()) { bandQueueA = new ArrayDeque<>(bandBufferA); }
				bandBufferA.clear();
				bandCursorA = 0;
			}
		}

		// Band B: mid [bandSplitA, bandSplitB)
		if (bandSplitB > bandSplitA) {
			if (bandIdleB > 0) {
				bandIdleB--;
			} else {
				int end = Math.min(bandCursorB + midBatch, bandSplitB);
				scanOffsetRange(offsets, bandCursorB, end, ax, ay, az, eventHorizon, world, bandBufferB, range);
				bandCursorB = end;
				if (bandCursorB >= bandSplitB) {
					if (bandBufferB.isEmpty()) { bandIdleB = midIdle; }
					else { bandQueueB = new ArrayDeque<>(bandBufferB); }
					bandBufferB.clear();
					bandCursorB = bandSplitA;
				}
			}
		}

		// Band C: outer [bandSplitB, size)
		if (size > bandSplitB) {
			if (bandIdleC > 0) {
				bandIdleC--;
			} else {
				int end = Math.min(bandCursorC + outerBatch, size);
				scanOffsetRange(offsets, bandCursorC, end, ax, ay, az, eventHorizon, world, bandBufferC, range);
				bandCursorC = end;
				if (bandCursorC >= size) {
					if (bandBufferC.isEmpty()) { bandIdleC = outerIdle; }
					else { bandQueueC = new ArrayDeque<>(bandBufferC); }
					bandBufferC.clear();
					bandCursorC = bandSplitB;
				}
			}
		}
	}

	private void scanOffsetRange(List<BlockPos> offsets, int from, int to,
			int ax, int ay, int az, double eventHorizon, World world, List<ScanEntry> buffer, int range) {
		for (int i = from; i < to; i++) {
			BlockPos offset = offsets.get(i);
			BlockPos scanPos = new BlockPos(ax + offset.getX(), ay + offset.getY(), az + offset.getZ());
			IBlockState blockState = world.getBlockState(scanPos);
			Block block = blockState.getBlock();
			if (block == Blocks.AIR) continue;
			int ox = offset.getX(), oy = offset.getY(), oz = offset.getZ();
			double distance = Math.sqrt(ox * ox + oy * oy + oz * oz);
			float hardness = blockState.getBlockHardness(world, scanPos);
			if (block instanceof IFluidBlock || block instanceof BlockLiquid) hardness = 1;
			float strength = getBreakStrength((float) distance, range);
			if (hardness >= 0 && (distance < eventHorizon || hardness < strength)) {
				buffer.add(new ScanEntry(scanPos, blockState));
			}
		}
	}

	private void breakNextQueuedBlocks(World world) {
		if (bandQueueA.isEmpty() && bandQueueB.isEmpty() && bandQueueC.isEmpty()) return;

		int range = (int) Math.floor(getBlockBreakRange());
		double eventHorizon = getEventHorizon();
		int broken = 0;
		broken = drainBreakQueue(bandQueueA, world, range, eventHorizon, broken);
		broken = drainBreakQueue(bandQueueB, world, range, eventHorizon, broken);
		drainBreakQueue(bandQueueC, world, range, eventHorizon, broken);
	}

	private int drainBreakQueue(Queue<ScanEntry> queue, World world, int range, double eventHorizon, int broken) {
		while (broken < BLOCKS_PER_BATCH && !queue.isEmpty()) {
			ScanEntry entry = queue.poll();
			IBlockState current = world.getBlockState(entry.pos);
			if (current.getBlock() == Blocks.AIR) continue;
			if (current.getBlock() != entry.scannedState.getBlock()) continue;
			if (cleanFlowingLiquids(current, entry.pos)) { broken++; continue; }
			if (cleanLiquids(current, entry.pos)) { broken++; continue; }
			try {
				double distance = Math.sqrt(entry.pos.distanceSq(getPos()));
				float strength = getBreakStrength((float) distance, range);
				breakBlock(world, entry.pos, strength, eventHorizon, range);
			} catch (Exception e) {
				MOLog.log(Level.ERROR, e, "There was a problem while trying to brake block %s",
						current.getBlock());
			}
			broken++;
		}
		return broken;
	}

    //region Consume Type Handlers
    public void consume(Entity entity) {

        if (!entity.isDead && onEntityConsume(entity,true)) {

            boolean consumedFlag = false;

            if (entity instanceof EntityItem) {
                consumedFlag |= consumeEntityItem((EntityItem)entity);
            } else if (entity instanceof EntityFallingBlock) {
                consumedFlag |= consumeFallingBlock((EntityFallingBlock)entity);
            } else if (entity instanceof EntityLivingBase)
            {
                consumedFlag |= consumeLivingEntity((EntityLivingBase) entity,getBreakStrength((float) entity.getDistance(getPos().getX(), getPos().getY(), getPos().getZ()),
								(float) getMaxRange()));
            }

            if (consumedFlag)
            {
                onEntityConsume(entity, false);
            }
        }
    }

	private boolean consumeEntityItem(EntityItem entityItem) {
		ItemStack itemStack = entityItem.getItem();
		if (!itemStack.isEmpty()) {
			try {
					setMass(Math.addExact(mass,
						(long) MatterHelper.getMatterAmountFromItem(itemStack) * (long) itemStack.getCount()));
				markDirty();
			} catch (ArithmeticException e) {
				return false;
			}

			entityItem.setDead();
			world.removeEntity(entityItem);

			if (entityItem.getItem().getItem() == Items.NETHER_STAR) {
				collapse();
			}
			// Just for darkosto
			if (entityItem.getItem().getItem().getRegistryName().toString().equalsIgnoreCase("extendedcrafting:storage")
					&& entityItem.getItem().getMetadata() == 2) {
				collapse();
			} else if (entityItem.getItem().getItem().getItemStackDisplayName(entityItem.getItem()).toLowerCase()
					.contains("nether star")) {
				collapse();
			}
			return true;
		}
		return false;
	}

	private boolean consumeFallingBlock(EntityFallingBlock fallingBlock) {
		ItemStack itemStack = new ItemStack(fallingBlock.getBlock().getBlock(), 1,
				fallingBlock.getBlock().getBlock().damageDropped(fallingBlock.getBlock()));
		if (!itemStack.isEmpty()) {
			try {
					setMass(Math.addExact(mass,
						(long) MatterHelper.getMatterAmountFromItem(itemStack) * (long) itemStack.getCount()));
				markDirty();
			} catch (ArithmeticException e) {
				return false;
			}

			fallingBlock.setDead();
			world.removeEntity(fallingBlock);
			return true;
		}
		return false;
	}

	private boolean consumeLivingEntity(EntityLivingBase entity, float strength) {
		try {
			setMass(Math.addExact(mass, (long) Math.min(entity.getHealth(), strength)));
			markDirty();
		} catch (ArithmeticException e) {
			return false;
		}

		if (entity.getHealth() <= strength && !(entity instanceof EntityPlayer)) {
			entity.setDead();
			world.removeEntity(entity);
		}

		DamageSource damageSource = new DamageSource("blackHole");
		entity.attackEntityFrom(damageSource, strength);
		return true;
	}
    //endregion

	public boolean breakBlock(World world, BlockPos pos, float strength, double eventHorizon, int range) {
    	IBlockState blockState = world.getBlockState(pos);
		if (blockState.getBlock().isAir(blockState, world, pos)) {
			return true;
		}
		float hardness = blockState.getBlockHardness(world, pos);
		double distance = Math.sqrt(pos.distanceSq(getPos()));
        if (distance <= range && hardness >= 0 && (distance < eventHorizon || hardness < strength))
        {
            if (BLOCK_ENTETIES) {

                if (FALLING_BLOCKS)
                {
                	EntityFallingBlock fallingBlock = new EntityFallingBlock(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, blockState);
                    fallingBlock.fallTime = 1;
                    fallingBlock.noClip = true;
                    world.spawnEntity(fallingBlock);
                }
                else {
                	ItemStack bStack = blockState.getBlock().getPickBlock(blockState, null, world, pos, null);
                    if (bStack != null)
                    {
                    	EntityItem item = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,  bStack);
                        world.spawnEntity(item);
                    }
                }

                blockState.getBlock().breakBlock(world, pos, blockState);
                world.playBroadcastSound(2001, pos, Block.getIdFromBlock(blockState.getBlock()));
                world.setBlockToAir(pos);
                return true;
            }else
            {
                int matter = 0;

				if (blockState.getBlock().canSilkHarvest(world, pos, blockState, null)) {
					matter += MatterHelper.getMatterAmountFromItem(
							blockState.getBlock().getPickBlock(blockState, null, world, pos, null));
				} else {
					for (ItemStack stack : blockState.getBlock().getDrops(world, pos, blockState, 0)) {
						matter += MatterHelper.getMatterAmountFromItem(stack);
					}
				}

                world.playBroadcastSound(2001, pos, Block.getIdFromBlock(blockState.getBlock()));

                List<EntityItem> result = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2, pos.getX() + 3,pos.getY() + 3, pos.getZ() + 3));
                for (EntityItem entityItem : result) {
                    consumeEntityItem(entityItem);
                }

                try {
                    setMass(Math.addExact(mass,matter));
                }
                catch (ArithmeticException e)
                {
                    return false;
                }

                world.setBlockToAir(pos);
                return true;
            }
        }

        return false;
    }
	
    //region Helper Methods
    protected ItemStack createStackedBlock(Block block,int meta)
    {
        if (block != null) {
            Item item = Item.getItemFromBlock(block);
            if (item != null) {
                if (item.getHasSubtypes()) {
                    return new ItemStack(item, 1, meta);
                }
                return new ItemStack(item, 1, 0);
            }
        }
        return null;
    }

	public boolean cleanLiquids(IBlockState blockState, BlockPos pos) {
		if (blockState.getBlock() instanceof IFluidBlock && FORGE_FLUIDS) {
			if (((IFluidBlock) blockState.getBlock()).canDrain(world, pos)) {
				if (FALLING_BLOCKS) {
					EntityFallingBlock fallingBlock = new EntityFallingBlock(world, pos.getX() + 0.5, pos.getY() + 0.5,
							pos.getZ() + 0.5, blockState);
					// fallingBlock.field_145812_b = 1;
					fallingBlock.noClip = true;
					world.spawnEntity(fallingBlock);
				}

				((IFluidBlock) blockState.getBlock()).drain(world, pos, true);
				return true;
			}

		} else if (blockState.getBlock() instanceof BlockLiquid && VANILLA_FLUIDS) {
			IBlockState state = world.getBlockState(pos);
			if (world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2)) {
				if (FALLING_BLOCKS) {
					EntityFallingBlock fallingBlock = new EntityFallingBlock(world, pos.getX() + 0.5, pos.getY() + 0.5,
							pos.getZ() + 0.5, state);
					// fallingBlock.field_145812_b = 1;
					fallingBlock.noClip = true;
					world.spawnEntity(fallingBlock);
				}
				return true;
			}
		}

		return false;
	}

	public boolean cleanFlowingLiquids(IBlockState block, BlockPos pos) {
		if (VANILLA_FLUIDS) {
			if (block.getBlock() == Blocks.FLOWING_WATER || block.getBlock() == Blocks.FLOWING_LAVA) {
				return world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
			}
		}
		return false;
	}

    //endregion

	public void collapse() {
		world.setBlockToAir(getPos());
		world.createExplosion(null, getPos().getX(), getPos().getY(), getPos().getZ(),
				(float) getRealMassUnsuppressed(), true);
	}

    @Override
    public void addInfo(World world, double x, double y, double z, List<String> infos)
    {
        DecimalFormat format = new DecimalFormat("#.##");
        infos.add("Mass: " + mass);
        infos.add("Range: " + format.format(getMaxRange()));
        infos.add("Brake Range: " + format.format(getBlockBreakRange()));
        infos.add("Horizon: " + format.format(getEventHorizon()));
        infos.add("Brake Lvl: " + format.format(getBreakStrength()));
    }

    public void suppress(AnomalySuppressor suppressor)
    {
        for (AnomalySuppressor s : supressors)
        {
            if (s.update(suppressor))
            {
                return;
            }
        }

        supressors.add(suppressor);
    }

    private float calculateSuppression()
    {
        float suppression = 1;
        Iterator<AnomalySuppressor> iterator = supressors.iterator();
        while (iterator.hasNext())
        {
            AnomalySuppressor s = iterator.next();
            s.tick();
            if (!s.isValid())
            {
                iterator.remove();
                continue;
            }
            suppression *= s.getAmount();
        }
        return suppression;
    }

    //region NBT
    @Override
    public void writeCustomNBT(NBTTagCompound nbt, EnumSet<MachineNBTCategory> categories, boolean toDisk)
    {
        if (categories.contains(MachineNBTCategory.DATA)) {
            nbt.setLong("Mass", mass);
            nbt.setFloat("Suppression", suppression);
            if (toDisk)
            {
                NBTTagList suppressors = new NBTTagList();
                for (AnomalySuppressor s : this.supressors)
                {
                    NBTTagCompound suppressorTag = new NBTTagCompound();
                    s.writeToNBT(suppressorTag);
                    suppressors.appendTag(suppressorTag);
                }
                nbt.setTag("suppressors", suppressors);
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbt, EnumSet<MachineNBTCategory> categories)
    {
        if (categories.contains(MachineNBTCategory.DATA)) {
            this.supressors.clear();
            setMass(nbt.getLong("Mass"));
            setSuppression(nbt.getFloat("Suppression"));
            NBTTagList suppressors = nbt.getTagList("suppressors", Constants.NBT.TAG_COMPOUND);
            for (int i = 0;i < suppressors.tagCount();i++)
            {
                NBTTagCompound suppressorTag = suppressors.getCompoundTagAt(i);
                AnomalySuppressor s = new AnomalySuppressor(suppressorTag);
                this.supressors.add(s);
            }
        }
    }
    //endregion

    //region Getters and Setters
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared()
    {
        return Math.max(Math.pow(getMaxRange(),3),2048);
    }

    public Block getBlock(World world,int x,int y,int z)
    {
    	return world.getBlockState(blockPos).getBlock();
    }

    public double getEventHorizon()
    {
        return Math.max((G2 * getRealMass()) / CC,0.5);
    }

    public double getBlockBreakRange()
    {
        return getMaxRange() / 2;
    }

    public double getMaxRange()
    {
        return Math.sqrt(getRealMass()*(G/0.01));
    }

    public double getAcceleration(double distanceSq)
    {
        return G * (getRealMass() / Math.max(distanceSq,0.0001f));
    }

    public double getRealMass() {
        updateDerivedMassCache();
        return cachedRealMass;
    }

    public double getRealMassUnsuppressed()
    {
        updateDerivedMassCache();
        return cachedRealMassUnsuppressed;
    }

    public float getBreakStrength(float distance,float maxRange)
    {
        updateDerivedMassCache();
        return cachedBaseBreakStrength * getDistanceFalloff(distance,maxRange);
    }

    public float getDistanceFalloff(float distance,float maxRange)
    {
        return (1 - (distance / maxRange));
    }

    public float getBreakStrength()
    {
        updateDerivedMassCache();
        return cachedBaseBreakStrength;
    }

    private void setMass(long newMass)
    {
        if (mass != newMass)
        {
            mass = newMass;
            derivedMassCacheDirty = true;
        }
    }

    private void setSuppression(float newSuppression)
    {
        if (Float.compare(suppression, newSuppression) != 0)
        {
            suppression = newSuppression;
            derivedMassCacheDirty = true;
            scanRange = -1;
            bandBufferA.clear(); bandQueueA.clear();
            bandBufferB.clear(); bandQueueB.clear();
            bandBufferC.clear(); bandQueueC.clear();
            markDirty();
        }
    }

    private void updateDerivedMassCache()
    {
        if (!derivedMassCacheDirty)
        {
            return;
        }
        cachedRealMassUnsuppressed = Math.log1p(Math.max(mass, 0) * STREHGTH_MULTIPLYER);
        cachedRealMass = cachedRealMassUnsuppressed * suppression;
        cachedBaseBreakStrength = (float) cachedRealMass * 4 * suppression;
        double maxRange = Math.sqrt(cachedRealMass * (G / 0.01));
        cachedRangeSq = maxRange * maxRange;
        BlockPos p = getPos();
        if (p != null) {
            double cx = p.getX() + 0.5, cy = p.getY() + 0.5, cz = p.getZ() + 0.5;
            cachedCenter = new Vec3d(cx, cy, cz);
            cachedGravitationBB = new AxisAlignedBB(
                    cx - maxRange - 1, cy - maxRange - 1, cz - maxRange - 1,
                    cx + maxRange + 1, cy + maxRange + 1, cz + maxRange + 1);
        }
        cachedEntityList = Collections.emptyList();
        entityScanTimer = ENTITY_SCAN_RATE; // force rescan on next tick after mass/suppression change
        derivedMassCacheDirty = false;
    }

    //endregion

    //region Sub Classes
    private static class ScanEntry
    {
        final BlockPos pos;
        final IBlockState scannedState;

        ScanEntry(BlockPos pos, IBlockState scannedState)
        {
            this.pos = pos;
            this.scannedState = scannedState;
        }
    }
    //endregion

	@Override
	public BlockPos getPosition() {
		// TODO Auto-generated method stub
		return null;
	}
}