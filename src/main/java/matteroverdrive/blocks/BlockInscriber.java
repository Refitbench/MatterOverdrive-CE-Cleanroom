
package matteroverdrive.blocks;

import matteroverdrive.blocks.includes.MOBlockMachine;
import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.tile.TileEntityInscriber;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;

public class BlockInscriber extends MOBlockMachine<TileEntityInscriber> {
	public static final PropertyBool ACTIVE = PropertyBool.create("active");
	public static final PropertyBool CTM    = PropertyBool.create("ctm");

	public BlockInscriber(Material material, String name) {
		super(material, name);
		setHasRotation();
		setBoundingBox(new AxisAlignedBB(0, 0, 0, 1, 12 / 16d, 1));
		setHardness(20.0F);
		this.setResistance(9.0f);
		this.setHarvestLevel("pickaxe", 2);
		setHasGui(true);
		setDefaultState(getDefaultState().withProperty(ACTIVE, false));
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		return super.getActualState(state, worldIn, pos).withProperty(CTM, Loader.isModLoaded("ctm"));
	}

	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, PROPERTY_DIRECTION, ACTIVE, CTM);
	}

	@Override
	public Class<TileEntityInscriber> getTileEntityClass() {
		return TileEntityInscriber.class;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		return new TileEntityInscriber();
	}

	@Nonnull
	@Override
	public TileEntityInscriber createNewTileEntity(World world, int meta) {
		return new TileEntityInscriber();
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		super.onConfigChanged(config);
		TileEntityInscriber.ENERGY_CAPACITY = config.getMachineInt(getTranslationKey(), "storage.energy", 512000,
				"How much energy the inscriber can store");
		TileEntityInscriber.ENERGY_TRANSFER = config.getMachineInt(getTranslationKey(), "transfer.energy", 16000,
				"Max energy per tick the inscriber can receive and extract");
		TileEntityInscriber.ENERGY_USAGE_MULTIPLIER = config.getMachineDouble(getTranslationKey(), "usage.multiplier", 1.0,
				"Multiplier applied to the energy cost of each inscription recipe");
	}

	/*
	 * @Override
	 * 
	 * @SideOnly(Side.CLIENT) public void registerBlockIcons(IIconRegister
	 * p_149651_1_) { return; }
	 * 
	 * @Override public int getRenderType() { return
	 * RendererBlockInscriber.renderID; }
	 */

	@Override
	@Deprecated
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
		return state.getValue(ACTIVE) ? 10 : 0;
	}

	/** Flips the ACTIVE property in the stored block state, triggering automatic
	 *  chunk light recalculation and client sync via setBlockState. Server-side only. */
	public static void setActive(boolean active, World world, BlockPos pos) {
		IBlockState current = world.getBlockState(pos);
		if (!(current.getBlock() instanceof BlockInscriber)) return;
		if (current.getValue(ACTIVE) == active) return;
		world.setBlockState(pos, current.withProperty(ACTIVE, active), 3);
	}
}