package matteroverdrive.blocks;

import matteroverdrive.blocks.includes.MOMatterEnergyStorageBlock;
import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.machines.decomposer.TileEntityMachineDecomposer;
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

import javax.annotation.Nonnull;

public class BlockDecomposer extends MOMatterEnergyStorageBlock<TileEntityMachineDecomposer> {

	public static final PropertyBool ACTIVE = PropertyBool.create("active");

	public BlockDecomposer(Material material, String name) {
		super(material, name, true, true);
		setHasRotation();
		setHardness(20.0F);
		this.setResistance(9.0f);
		this.setHarvestLevel("pickaxe", 2);
		this.setDefaultState(this.blockState.getBaseState()
				.withProperty(PROPERTY_DIRECTION, EnumFacing.NORTH)
				.withProperty(ACTIVE, false));
		this.setTranslationKey("decomposer");
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
	 * disturbing the tile entity. No-ops when pos is not a BlockDecomposer or the
	 * state already matches. Returns true if the state was actually changed.
	 */
	public static boolean setActive(boolean active, World world, BlockPos pos) {
		IBlockState current = world.getBlockState(pos);
		if (!(current.getBlock() instanceof BlockDecomposer)) return false;
		if (current.getValue(ACTIVE) == active) return false;
		world.setBlockState(pos, current.withProperty(ACTIVE, active), 3);
		return true;
	}

	@Override
	public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos) {
		return true;
	}

	@Override
	public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return true;
	}

	@Override
	public Class<TileEntityMachineDecomposer> getTileEntityClass() {
		return TileEntityMachineDecomposer.class;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		return new TileEntityMachineDecomposer();
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		super.onConfigChanged(config);
		config.initMachineCategory(getTranslationKey());
		TileEntityMachineDecomposer.MATTER_STORAGE = config.getMachineInt(getTranslationKey(), "storage.matter", 1024,
				String.format("Maximum matter storage capacity (%s)", getLocalizedName()));
		TileEntityMachineDecomposer.ENERGY_CAPACITY = config.getMachineInt(getTranslationKey(), "storage.energy",
				512000, String.format("Maximum energy storage capacity (%s)", getLocalizedName()));
		TileEntityMachineDecomposer.DECEOPOSE_SPEED_PER_MATTER = config.getMachineInt(getTranslationKey(),
				"speed.decompose", 80, "Number of ticks per matter unit decomposed (lower = faster)");
		TileEntityMachineDecomposer.DECOMPOSE_ENERGY_PER_MATTER = config.getMachineInt(getTranslationKey(),
				"cost.decompose", 6000, "Energy cost in RF per 1 unit of matter decomposed");
	}

}
