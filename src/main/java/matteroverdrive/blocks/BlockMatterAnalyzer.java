
package matteroverdrive.blocks;

import javax.annotation.Nonnull;

import matteroverdrive.blocks.includes.MOMatterEnergyStorageBlock;
import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.machines.analyzer.ComponentTaskProcessingAnalyzer;
import matteroverdrive.machines.analyzer.TileEntityMachineMatterAnalyzer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockMatterAnalyzer extends MOMatterEnergyStorageBlock<TileEntityMachineMatterAnalyzer> {

	public static final PropertyBool ACTIVE = PropertyBool.create("active");

	public BlockMatterAnalyzer(Material material, String name) {
		super(material, name, true, true);
		setHasRotation();
		setHardness(20.0F);
		setLightOpacity(2);
		this.setResistance(5.0f);
		this.setHarvestLevel("pickaxe", 2);
		this.setDefaultState(this.blockState.getBaseState()
				.withProperty(PROPERTY_DIRECTION, EnumFacing.NORTH)
				.withProperty(ACTIVE, false));
		this.setTranslationKey("matter_analyzer");
		setHasGui(true);
	}

	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, PROPERTY_DIRECTION, ACTIVE);
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer,
			ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
	}

	/**
	 * Encode both ACTIVE and facing into the 4-bit block metadata.
	 * Facing uses bits 0-2 (EnumFacing.getIndex(): N=2, S=3, W=4, E=5).
	 * ACTIVE uses bit 3.
	 */
	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(PROPERTY_DIRECTION).getIndex() | (state.getValue(ACTIVE) ? 8 : 0);
	}

	@Nonnull
	@Override
	@Deprecated
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState()
				.withProperty(PROPERTY_DIRECTION, EnumFacing.byIndex(meta & 7))
				.withProperty(ACTIVE, (meta & 8) != 0);
	}

	/**
	 * Flips the ACTIVE block-state property at pos without replacing the block or
	 * disturbing the tile entity. No-ops when pos is not a BlockMatterAnalyzer or
	 * the state already matches. Returns true if the state was actually changed.
	 */
	public static boolean setActive(boolean active, World world, BlockPos pos) {
		IBlockState current = world.getBlockState(pos);
		if (!(current.getBlock() instanceof BlockMatterAnalyzer)) return false;
		if (current.getValue(ACTIVE) == active) return false;
		world.setBlockState(pos, current.withProperty(ACTIVE, active), 3);
		return true;
	}

	@Override
	@Deprecated
	public boolean isOpaqueCube(IBlockState state) {
		return true;
	}

	@Override
	public boolean canPlaceTorchOnTop(IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
		return true;
	}

	@Override
	public boolean isSideSolid(IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side) {
		return true;
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		super.onConfigChanged(config);
		config.initMachineCategory(getTranslationKey());
		TileEntityMachineMatterAnalyzer.ENERGY_CAPACITY = config.getMachineInt(getTranslationKey(), "storage.energy",
				512000, "Maximum energy storage capacity of the Matter Analyzer in RF");
		ComponentTaskProcessingAnalyzer.ANALYZE_SPEED = config.getMachineInt(getTranslationKey(), "speed.analyze",
				800, "Ticks per analysis cycle (lower = faster; affected by Speed upgrades)");
		ComponentTaskProcessingAnalyzer.ENERGY_DRAIN_PER_ITEM = config.getMachineInt(getTranslationKey(), "cost.analyze",
				64000, "Total energy cost in RF per item analyzed");
		ComponentTaskProcessingAnalyzer.PROGRESS_AMOUNT_PER_ITEM = config.getMachineInt(getTranslationKey(), "progress.per_scan",
				20, "Pattern progress percentage added per successful scan (5 scans needed to fully unlock at default of 20)");
		ComponentTaskProcessingAnalyzer.FAIL_CHANCE = config.getMachineDouble(getTranslationKey(), "chance.fail",
				0.10, "Base probability (0.0-1.0) that a scan fails and the item is consumed without progress");
	}

	@Override
	public Class<TileEntityMachineMatterAnalyzer> getTileEntityClass() {
		return TileEntityMachineMatterAnalyzer.class;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		return new TileEntityMachineMatterAnalyzer();
	}
}
