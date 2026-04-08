
package matteroverdrive.blocks;

import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.tile.TileEntityMachineContractMarket;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockContractMarket extends BlockMonitor<TileEntityMachineContractMarket> {
	public BlockContractMarket(Material material, String name) {
		super(material, name);
		setHardness(20.0F);
		this.setResistance(9.0f);
		this.setHarvestLevel("pickaxe", 2);
		setBoundingBox(new AxisAlignedBB(0, 1, 0, 1, 11 / 16d, 1));
		setHasGui(true);
	}

	@Override
	public Class<TileEntityMachineContractMarket> getTileEntityClass() {
		return TileEntityMachineContractMarket.class;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		return new TileEntityMachineContractMarket();
	}

	@Nonnull
	@Override
	public TileEntityMachineContractMarket createNewTileEntity(World world, int meta) {
		return new TileEntityMachineContractMarket();
	}

	@Override
	protected boolean hasMachineSound() {
		return false;
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		super.onConfigChanged(config);
		// Base delay in minutes before a new contract is generated
		TileEntityMachineContractMarket.QUEST_GENERATE_DELAY_MIN = config.getMachineInt(getTranslationKey(),
				"delay.min", 30,
				"The base delay in minutes before a new contract is generated") * 20 * 60;
		// Additional delay in minutes added per already-filled contract slot
		TileEntityMachineContractMarket.QUEST_GENERATE_DELAY_PER_SLOT = config.getMachineInt(getTranslationKey(),
				"delay.per_slot", 5,
				"Additional delay in minutes added per filled contract slot") * 20 * 60;
	}
}
