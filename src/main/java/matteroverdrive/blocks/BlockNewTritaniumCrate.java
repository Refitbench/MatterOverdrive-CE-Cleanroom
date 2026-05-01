package matteroverdrive.blocks;

import matteroverdrive.api.internal.IItemBlockFactory;
import matteroverdrive.api.wrench.IDismantleable;
import matteroverdrive.blocks.includes.MOBlockMachine;
import matteroverdrive.init.MatterOverdriveSounds;
import matteroverdrive.items.CrateBlockItem;
import matteroverdrive.tile.TileEntityNewTritaniumCrate;
import matteroverdrive.util.MOBlockHelper;
import matteroverdrive.util.MOInventoryHelper;
import matteroverdrive.util.MatterHelper;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class BlockNewTritaniumCrate extends MOBlockMachine<TileEntityNewTritaniumCrate> implements IDismantleable, IItemBlockFactory {
	private static final AxisAlignedBB BOX_NORTH_SOUTH = new AxisAlignedBB(0, 0, 2 / 16d, 1, 12 / 16d, 14 / 16d);
	private static final AxisAlignedBB BOX_EAST_WEST = new AxisAlignedBB(2 / 16d, 0, 0, 14 / 16d, 12 / 16d, 1);

	public static final PropertyEnum<Color> COLOR = PropertyEnum.create("color", Color.class);

	public BlockNewTritaniumCrate(Material material, String name, int metadata) {
		super(material, name);
		setHasRotation();
		setHardness(20.0F);
		this.setResistance(9.0f);
		this.setHarvestLevel("pickaxe", 2);
		setRotationType(MOBlockHelper.RotationType.FOUR_WAY);
	}

	public BlockNewTritaniumCrate(Material material, String name) {
		this(material, name, 0);
	}

	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, PROPERTY_DIRECTION, COLOR);
	}

	@Override
	@Deprecated
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Nonnull
	@Override
	@Deprecated
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		EnumFacing dir = state.getValue(PROPERTY_DIRECTION);
		return dir == EnumFacing.NORTH || dir == EnumFacing.SOUTH ? BOX_NORTH_SOUTH : BOX_EAST_WEST;
	}

	@Override
	public Class<TileEntityNewTritaniumCrate> getTileEntityClass() {
		return TileEntityNewTritaniumCrate.class;
	}

	@Nullable
	@Override
	public TileEntityNewTritaniumCrate createNewTileEntity(World worldIn, int meta) {
		return new TileEntityNewTritaniumCrate();
	}

	/** Color Definitions **/
	public enum Color implements IStringSerializable {
		BASE(0, "base"), RED(1, "red"), GREEN(2, "green"), BROWN(3, "brown"), BLUE(4, "blue"), PURPLE(5, "purple"),
		CYAN(6, "cyan"), LIGHTGRAY(7, "light_gray"), GRAY(8, "gray"), PINK(9, "pink"), LIME(10, "lime"),
		YELLOW(11, "yellow"), LIGHTBLUE(12, "light_blue"), MAGENTA(13, "magenta"), ORANGE(14, "orange"),
		SECOND_WHITE(15, "second_white"), BLACK(16, "black"), WHITE(17, "white");

		private final int metadata;
		private final String name;

		Color(int metadata, String name) {
			this.metadata = metadata;
			this.name = name;
		}

		public int getMetadata() {
			return this.metadata;
		}

		@Nonnull
		@Override
		public String getName() {
			return this.name;
		}
	}

	private static final String[] DYE_ORE_NAMES = {
		"dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple",
		"dyeCyan", "dyeLightGray", "dyeGray", "dyePink", "dyeLime", "dyeYellow",
		"dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"
	};

	private static final Color[] DYE_COLOR_MAP = {
		Color.BLACK, Color.RED, Color.GREEN, Color.BROWN, Color.BLUE, Color.PURPLE,
		Color.CYAN, Color.LIGHTGRAY, Color.GRAY, Color.PINK, Color.LIME, Color.YELLOW,
		Color.LIGHTBLUE, Color.MAGENTA, Color.ORANGE, Color.WHITE
	};

	@Nullable
	private static Color getDyeColor(ItemStack stack) {
		for (int i = 0; i < DYE_ORE_NAMES.length; i++) {
			for (ItemStack oreStack : OreDictionary.getOres(DYE_ORE_NAMES[i])) {
				if (OreDictionary.itemMatches(oreStack, stack, false)) {
					return DYE_COLOR_MAP[i];
				}
			}
		}
		return null;
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
			EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote) {
			return true;
		}

		ItemStack currentitem = playerIn.getHeldItem(EnumHand.MAIN_HAND);

		if (!currentitem.isEmpty()) {
			boolean isBucket = currentitem.getItem().equals(Items.WATER_BUCKET);
			Color dyeColor = isBucket ? Color.BASE : getDyeColor(currentitem);

			if (dyeColor != null) {
				IBlockState coloredState = state.withProperty(COLOR, dyeColor);
				worldIn.setBlockState(pos, coloredState, 3);

				TileEntity tileEntity = worldIn.getTileEntity(pos);
				if (tileEntity instanceof TileEntityNewTritaniumCrate) {
					TileEntityNewTritaniumCrate tentc = (TileEntityNewTritaniumCrate) tileEntity;
					if (tentc.getColor() != dyeColor.getMetadata()) {
						tentc.setColor(dyeColor.getMetadata());
						tentc.markDirty();
						worldIn.markBlockRangeForRenderUpdate(pos, pos);
						worldIn.notifyBlockUpdate(pos, coloredState, coloredState, 3);
					}
				}

				if (!playerIn.capabilities.isCreativeMode) {
					if (isBucket) {
						// I'm worried about a memory leak here. What happens to the item that WAS in
						// the main hand?
						// Buckets are also not stackable, so no need to worry about having more than
						// one in the main hand.
						playerIn.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BUCKET, 1));
					} else {
						currentitem.setCount(currentitem.getCount() - 1);
					}
				}

				return true;
			}
		}

		TileEntity entity = worldIn.getTileEntity(pos);

		if (entity instanceof TileEntityNewTritaniumCrate) {
			// FMLNetworkHandler.openGui(entityPlayer, MatterOverdrive.instance,
			// GuiHandler.TRITANIUM_CRATE, world, x, y, z);
			worldIn.playSound(null, pos.getX(), pos.getY(), pos.getZ(), MatterOverdriveSounds.blocksCrateOpen,
					SoundCategory.BLOCKS, 0.5f, 1);

			playerIn.displayGUIChest(((TileEntityNewTritaniumCrate) entity).getInventory());

			return true;
		}

		return false;
	}

	@Override
	public boolean canDismantle(EntityPlayer player, World world, BlockPos pos) {
		return true;
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		TileEntity tile = worldIn.getTileEntity(pos);
		if (tile instanceof TileEntityNewTritaniumCrate) {
			MatterHelper.dropInventory(worldIn, (TileEntityNewTritaniumCrate) tile, pos);
			((TileEntityNewTritaniumCrate) tile).getInventory().clear();
		}

		super.breakBlock(worldIn, pos, state);
	}

	@Override
	public ArrayList<ItemStack> dismantleBlock(EntityPlayer player, World world, BlockPos pos, boolean returnDrops) {
		ArrayList<ItemStack> items = new ArrayList<>();
		TileEntity tile = world.getTileEntity(pos);

		// Build a block ItemStack with the crate's inventory + color serialized into NBT.
		ItemStack blockItem = getNBTDrop(world, pos,
				tile instanceof TileEntityNewTritaniumCrate ? (TileEntityNewTritaniumCrate) tile : null);
		items.add(blockItem);

		// Clear the live inventory so breakBlock() does not also drop the items
		if (tile instanceof TileEntityNewTritaniumCrate) {
			((TileEntityNewTritaniumCrate) tile).getInventory().clear();
		}

		IBlockState blockState = world.getBlockState(pos);
		boolean flag = blockState.getBlock().removedByPlayer(blockState, world, pos, player, true);
		super.breakBlock(world, pos, blockState);

		if (flag) {
			blockState.getBlock().onPlayerDestroy(world, pos, blockState);
		}

		if (!returnDrops) {
			Block.spawnAsEntity(world, pos, blockItem);
		} else {
			MOInventoryHelper.insertItemStackIntoInventory(player.inventory, blockItem, EnumFacing.DOWN);
		}

		return items;
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

		// Trigger a render update so the placed crate shows the correct color.
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Color")) {
			worldIn.markBlockRangeForRenderUpdate(pos, pos);
			worldIn.notifyBlockUpdate(pos, state, state, 3);
		}
	}

	@Override
	public ItemBlock createItemBlock() {
		return new CrateBlockItem(this);
	}

	@Nonnull
	@Override
	public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		if (worldIn.getTileEntity(pos) instanceof TileEntityNewTritaniumCrate) {
			TileEntityNewTritaniumCrate tentc = (TileEntityNewTritaniumCrate) worldIn.getTileEntity(pos);

			if (tentc != null) {
				return super.getActualState(state, worldIn, pos).withProperty(COLOR, Color.values()[tentc.getColor()]);
			}
		}

		return super.getActualState(state, worldIn, pos);
	}

	@Override
	@Deprecated
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}
}