
package matteroverdrive.blocks;

import javax.annotation.Nonnull;

import matteroverdrive.blocks.includes.MOBlockMachine;
import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.tile.TileEntityMachineGravitationalStabilizer;
import matteroverdrive.util.IConfigSubscriber;
import matteroverdrive.util.MOBlockHelper;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockGravitationalStabilizer extends MOBlockMachine<TileEntityMachineGravitationalStabilizer>
		implements IConfigSubscriber {

	public BlockGravitationalStabilizer(Material material, String name) {
		super(material, name);
		setHasRotation();
		setHardness(20.0F);
		this.setResistance(10.0f);
		this.setHarvestLevel("pickaxe", 2);
		lightValue = 10;
		setRotationType(MOBlockHelper.RotationType.SIX_WAY);
		setHasGui(true);
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		super.onConfigChanged(config);
		String machine = getTranslationKey();
		TileEntityMachineGravitationalStabilizer.ENERGY_CAPACITY = config.getMachineInt(
				machine, ConfigurationHandler.KEY_GRAVITATIONAL_STABILIZER_ENERGY_CAPACITY, 250000,
				"Maximum RF stored by the gravitational stabilizer.");
		TileEntityMachineGravitationalStabilizer.MAX_ENERGY_RECEIVE = config.getMachineInt(
				machine, ConfigurationHandler.KEY_GRAVITATIONAL_STABILIZER_MAX_ENERGY_RECEIVE, 4000,
				"Maximum RF/t the gravitational stabilizer can accept.");
		TileEntityMachineGravitationalStabilizer.BASE_ENERGY_PER_TICK = config.getMachineInt(
				machine, ConfigurationHandler.KEY_GRAVITATIONAL_STABILIZER_BASE_ENERGY_PER_TICK, 200,
				"RF/t consumed at minimum anomaly stress (stress = 0%).");
		TileEntityMachineGravitationalStabilizer.MAX_ENERGY_PER_TICK = config.getMachineInt(
				machine, ConfigurationHandler.KEY_GRAVITATIONAL_STABILIZER_MAX_ENERGY_PER_TICK, 2000,
				"RF/t consumed at maximum anomaly stress (stress = 100%).");
		TileEntityMachineGravitationalStabilizer.RESCAN_RATE = config.getMachineInt(
				machine, ConfigurationHandler.KEY_GRAVITATIONAL_STABILIZER_RESCAN_RATE, 40,
				"How often (in ticks) the stabilizer re-scans the beam length for obstructions. Default 40 (~2s). Lower = more responsive, higher = cheaper.");
	}

	@Override
	public Class<TileEntityMachineGravitationalStabilizer> getTileEntityClass() {
		return TileEntityMachineGravitationalStabilizer.class;
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
		TileEntityMachineGravitationalStabilizer te = getTileEntity(worldIn, pos);
		if (te == null) return 0;
		if (te.getComparatorMode() == 1) {
			// Stability mode: getPercentage() is 0-1 stress; less stress = more stable
			float stress = te.getPercentage();
			if (stress < 0) return 0;
			return Math.round((1.0f - stress) * 15);
		} else {
			// Power mode: energy fill 0-1
			float fill = (float) te.getEnergyStorage().getEnergyStored()
					/ (float) te.getEnergyStorage().getMaxEnergyStored();
			return Math.round(fill * 15);
		}
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		return new TileEntityMachineGravitationalStabilizer();
	}

}