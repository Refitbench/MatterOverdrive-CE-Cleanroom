
package matteroverdrive.blocks;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import matteroverdrive.api.IScannable;
import matteroverdrive.blocks.includes.MOBlockContainer;
import matteroverdrive.data.world.GenPositionWorldData;
import matteroverdrive.data.world.WorldPosition2D;
import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.tile.TileEntityGravitationalAnomaly;
import matteroverdrive.util.IConfigSubscriber;
import matteroverdrive.world.MOWorldGen;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockGravitationalAnomaly extends MOBlockContainer<TileEntityGravitationalAnomaly>
		implements IScannable, IConfigSubscriber {
	public BlockGravitationalAnomaly(Material material, String name) {
		super(material, name);
		setBoundingBox(new AxisAlignedBB(0.3f, 0.3f, 0.3f, 0.6f, 0.6f, 0.6f));
		setBlockUnbreakable();
		setResistance(6000000.0F);
		disableStats();
	}

	@Override
	@Deprecated
	public boolean isNormalCube(IBlockState blockState) {
		return false;
	}
	
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
	  if(placer instanceof EntityPlayer) {
			GenPositionWorldData data = MOWorldGen.getWorldPositionData(world);
			data.addPosition("gravitational_anomaly", new WorldPosition2D(pos.getX(), pos.getZ()));
	  }
	}

	@Override
	@Deprecated
	public RayTraceResult collisionRayTrace(IBlockState state, @Nonnull World world, @Nonnull BlockPos pos,
			@Nonnull Vec3d start, @Nonnull Vec3d end) {
		return super.collisionRayTrace(state, world, pos, start, end);
	}

	@Override
	public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
		return true;
	}

	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		TileEntityGravitationalAnomaly tileEntity = getTileEntity(source, pos);
		if (tileEntity != null) {
			double range = tileEntity.getEventHorizon();
			range = Math.max(range, 0.4);
			float rangeMin = (float) (0.5 - (range / 2));
			float rangeMax = (float) (0.5 + (range / 2));
			return new AxisAlignedBB(rangeMin, rangeMin, rangeMin, rangeMax, rangeMax, rangeMax);
		}
		return super.getBoundingBox(state, source, pos);
	}

	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
		return Block.NULL_AABB;
	}

	@Override
	@Deprecated
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	@Deprecated
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public Class<TileEntityGravitationalAnomaly> getTileEntityClass() {
		return TileEntityGravitationalAnomaly.class;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		return new TileEntityGravitationalAnomaly();
	}

	@Override
	public void addInfo(World world, double x, double y, double z, List<String> infos) {
		TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));

		if (tileEntity instanceof TileEntityGravitationalAnomaly) {
			((TileEntityGravitationalAnomaly) tileEntity).addInfo(world, x, y, z, infos);
		}
	}

	@Override
	public void onScan(World world, double x, double y, double z, EntityPlayer player, ItemStack scanner) {

	}

	@Override
	public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
		return false;
	}

	@Nonnull
	@Override
	@Deprecated
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.INVISIBLE;
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		TileEntityGravitationalAnomaly.BLOCK_ENTETIES = config.getBool(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_BLOCK_ENTITIES,
				ConfigurationHandler.CATEGORY_SERVER + "." + getTranslationKey().substring(5), true,
				"Should the blocks drop entities or be directly consumed when destroyed by the gravitational anomaly");
		TileEntityGravitationalAnomaly.FALLING_BLOCKS = config.getBool(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_FALLING_BLOCKS,
				ConfigurationHandler.CATEGORY_SERVER + "." + getTranslationKey().substring(5), true,
				"Should blocks be turned into falling blocks when broken");
		TileEntityGravitationalAnomaly.VANILLA_FLUIDS = config.getBool(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_VANILLA_FLUIDS,
				ConfigurationHandler.CATEGORY_SERVER + "." + getTranslationKey().substring(5), true,
				"Should vanilla fluid block such as water and lava be consumed by the anomaly");
		TileEntityGravitationalAnomaly.FORGE_FLUIDS = config.getBool(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_FORGE_FLUIDS,
				ConfigurationHandler.CATEGORY_SERVER + "." + getTranslationKey().substring(5), true,
				"Should other mod fluid blocks be consumed by the anomaly");
		TileEntityGravitationalAnomaly.BLOCK_DESTRUCTION = config.getBool("block destruction",
				ConfigurationHandler.CATEGORY_SERVER + "." + getTranslationKey().substring(5), true,
				"Should the gravitational anomaly destroy blocks");
		TileEntityGravitationalAnomaly.GRAVITATION = config.getBool("gravitational pull",
				ConfigurationHandler.CATEGORY_SERVER + "." + getTranslationKey().substring(5), true,
				"Should the gravitational entity pull entities towards it");
		TileEntityGravitationalAnomaly.SOUND = config.getBool("gravitational anomaly souund",
				ConfigurationHandler.CATEGORY_CLIENT, true,
				"Should the gravitational anomaly have sound?");
		String cat = ConfigurationHandler.CATEGORY_SERVER + "." + getTranslationKey().substring(5);
		TileEntityGravitationalAnomaly.SCAN_BATCH_SIZE = config.getInt(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_SCAN_BATCH_SIZE, cat, 128,
				"How many blocks the anomaly checks per tick while sweeping its array. \n"
				+ "The anomaly divides its full sphere into distance sorted array \n"
				+ "For example, a sphere of radius 12 contains ~7200 positions; \n"
				+ "at 128 checks/tick that is ~56 ticks (~2.8s) per full sweep. \n"
				+ "Higher = faster sweeps but more block lookups aka more CPU usage.");
		TileEntityGravitationalAnomaly.BLOCKS_PER_BATCH = config.getInt(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_BLOCKS_PER_BATCH, cat, 1,
				"How many blocks the anomaly breaks each time the break batch runs.");
		TileEntityGravitationalAnomaly.BATCH_TICK_RATE = config.getInt(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_BATCH_TICK_RATE, cat, 4,
				"How often (in ticks) the anomaly runs a break batch.");
		TileEntityGravitationalAnomaly.IDLE_SCAN_TICKS = config.getInt(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_IDLE_SCAN_TICKS, cat, 60,
				"How many ticks the anomaly waits before re-scanning after a full sweep found nothing to break. "
				+ "Default 60 (~3 seconds). Reduces CPU when the anomaly has consumed all nearby blocks.");
		TileEntityGravitationalAnomaly.ENTITY_SCAN_RATE = config.getInt(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_ENTITY_SCAN_RATE, cat, 10,
				"How often (in ticks) the anomaly rescans for nearby entities. Force is still applied every tick. "
				+ "Higher values reduce CPU cost at the expense of slightly delayed pickup of newly entered entities.");
		TileEntityGravitationalAnomaly.DEBUG_SCAN_WIREFRAME = config.getBool(
				ConfigurationHandler.KEY_GRAVITATIONAL_ANOMALY_DEBUG_SCAN_WIREFRAME,
				ConfigurationHandler.CATEGORY_DEBUG, false,
				"Draw wireframe spheres for each scan band radius around every loaded anomaly. Client-side debug only.");
		TileEntityGravitationalAnomaly.EQUALIZER_DAMAGE_RATE = config.getInt(
				ConfigurationHandler.KEY_EQUALIZER_DAMAGE_RATE, cat, 200,
				"How often (in ticks) an equipped Space-Time Equalizer loses 1 durability while resisting an anomaly. Set to 0 to disable durability drain.");
	}
}
