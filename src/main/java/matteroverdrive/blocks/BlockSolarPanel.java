
package matteroverdrive.blocks;

import javax.annotation.Nonnull;

import matteroverdrive.blocks.includes.MOMatterEnergyStorageBlock;
import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.tile.TileEntityMachineSolarPanel;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public class BlockSolarPanel extends MOMatterEnergyStorageBlock<TileEntityMachineSolarPanel> {
	public BlockSolarPanel(Material material, String name) {
		super(material, name, true, false);

		setBoundingBox(new AxisAlignedBB(0, 0, 0, 1, 8 / 16d, 1));
		setHardness(20.0F);
		this.setResistance(5.0f);
		this.setHarvestLevel("pickaxe", 2);
		setHasGui(true);
	}

	@Override
	public Class<TileEntityMachineSolarPanel> getTileEntityClass() {
		return TileEntityMachineSolarPanel.class;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState meta) {
		return new TileEntityMachineSolarPanel();
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	protected boolean hasMachineSound() {
		return false;
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		super.onConfigChanged(config);
		config.initMachineCategory(getTranslationKey());
		TileEntityMachineSolarPanel.ENERGY_CAPACITY = config.getMachineInt(getTranslationKey(), "storage.energy",
				64000, "Maximum energy storage capacity of the Solar Panel in RF");
		TileEntityMachineSolarPanel.MAX_ENERGY_EXTRACT = config.getMachineInt(getTranslationKey(), "transfer.energy",
				512, "Maximum RF per tick the Solar Panel can push into adjacent machines");
		TileEntityMachineSolarPanel.CHARGE_AMOUNT = config.getMachineInt(getTranslationKey(), "generation.base",
				8, "Base RF generated per tick at peak solar angle (scales with sun position and light level)");
	}

}
