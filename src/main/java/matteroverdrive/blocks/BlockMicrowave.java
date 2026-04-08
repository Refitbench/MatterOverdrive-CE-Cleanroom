package matteroverdrive.blocks;

import matteroverdrive.blocks.includes.MOBlockMachine;
import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.tile.TileEntityMicrowave;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockMicrowave extends MOBlockMachine<TileEntityMicrowave> {
	public BlockMicrowave(Material material, String name) {
		super(material, name);
		setHasRotation();
		setHardness(20.0F);
		this.setResistance(9.0f);
		this.setHarvestLevel("pickaxe", 2);
		setHasGui(true);
	}

	@Override
	public Class<TileEntityMicrowave> getTileEntityClass() {
		return TileEntityMicrowave.class;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState meta) {
		return new TileEntityMicrowave();
	}

	@Nonnull
	@Override
	public TileEntityMicrowave createNewTileEntity(World world, int meta) {
		return new TileEntityMicrowave();
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		super.onConfigChanged(config);
		TileEntityMicrowave.ENERGY_CAPACITY = config.getMachineInt(getTranslationKey(), "storage.energy", 512000,
				"How much energy the microwave can store");
		TileEntityMicrowave.ENERGY_TRANSFER = config.getMachineInt(getTranslationKey(), "transfer.energy", 512000,
				"Max energy per tick the microwave can receive and extract");
		TileEntityMicrowave.ENERGY_COST = config.getMachineInt(getTranslationKey(), "cost.energy", 1000,
				"Energy consumed per cooking operation");
		TileEntityMicrowave.COOK_SPEED = config.getMachineInt(getTranslationKey(), "speed.cook", 10,
				"Ticks per cooking operation (lower is faster)");
	}

	@Override
	public boolean isFullBlock(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}
}
