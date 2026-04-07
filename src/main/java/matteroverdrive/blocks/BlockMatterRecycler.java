package matteroverdrive.blocks;

import matteroverdrive.blocks.includes.MOMatterEnergyStorageBlock;
import matteroverdrive.tile.TileEntityMachineMatterRecycler;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockMatterRecycler extends MOMatterEnergyStorageBlock<TileEntityMachineMatterRecycler> {

	public static final PropertyBool ACTIVE = PropertyBool.create("active");

	public BlockMatterRecycler(Material material, String name) {
		super(material, name, true, true);
		setHasRotation();
		setHardness(20.0F);
		this.setResistance(9.0f);
		this.setHarvestLevel("pickaxe", 2);
		this.setDefaultState(this.blockState.getBaseState()
				.withProperty(PROPERTY_DIRECTION, EnumFacing.NORTH)
				.withProperty(ACTIVE, false));
		this.setTranslationKey("matter_recycler");
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
	 * disturbing the tile entity. No-ops when pos is not a BlockMatterRecycler or
	 * the state already matches. Returns true if the state was actually changed.
	 */
	public static boolean setActive(boolean active, World world, BlockPos pos) {
		IBlockState current = world.getBlockState(pos);
		if (!(current.getBlock() instanceof BlockMatterRecycler)) return false;
		if (current.getValue(ACTIVE) == active) return false;
		world.setBlockState(pos, current.withProperty(ACTIVE, active), 3);
		return true;
	}

	@Override
	public Class<TileEntityMachineMatterRecycler> getTileEntityClass() {
		return TileEntityMachineMatterRecycler.class;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState meta) {
		return new TileEntityMachineMatterRecycler();
	}
}
