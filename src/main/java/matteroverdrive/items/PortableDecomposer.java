
package matteroverdrive.items;

import java.text.DecimalFormat;
import java.util.List;

import javax.annotation.Nullable;

import matteroverdrive.handler.ConfigurationHandler;
import matteroverdrive.init.OverdriveFluids;
import matteroverdrive.items.includes.EnergyContainer;
import matteroverdrive.items.includes.MOItemEnergyContainer;
import matteroverdrive.util.IConfigSubscriber;
import matteroverdrive.util.MatterHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PortableDecomposer extends MOItemEnergyContainer implements IConfigSubscriber {
	public static int ENERGY_CAPACITY = 128000;
	public static int ENERGY_TRANSFER = 256;
	public static int MAX_MATTER = 512;
	public static double MATTER_RATIO = 0.1;
	public static double ENERGY_COST_MULTIPLIER = 1.0;

	public PortableDecomposer(String name) {
		super(name);
	}

	@Override
	protected int getCapacity() {
		return ENERGY_CAPACITY;
	}

	@Override
	protected int getInput() {
		return ENERGY_TRANSFER;
	}

	@Override
	protected int getOutput() {
		return ENERGY_TRANSFER;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addDetails(ItemStack itemstack, EntityPlayer player, @Nullable World worldIn, List<String> infos) {
		super.addDetails(itemstack, player, worldIn, infos);
		infos.add(String.format("%s/%s %s", DecimalFormat.getIntegerInstance().format(getMatter(itemstack)),
				getMaxMatter(itemstack), MatterHelper.MATTER_UNIT));
		if (itemstack.getTagCompound() != null) {
			ItemStack s;
			NBTTagList list = itemstack.getTagCompound().getTagList("Items", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < list.tagCount(); i++) {
				s = new ItemStack(list.getCompoundTagAt(i));
				infos.add(TextFormatting.GRAY + s.getDisplayName());
			}
		}
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
			float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing)) {
			IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing);
			FluidStack fluid = new FluidStack(OverdriveFluids.matterPlasma, getMatter(stack));
			int filled = handler.fill(fluid, true);
			setMatter(stack, Math.max(0, fluid.amount - filled));
			return EnumActionResult.SUCCESS;
		}
		return EnumActionResult.FAIL;
	}

	public int getMatter(ItemStack itemStack) {
		if (itemStack.getTagCompound() != null) {
			return itemStack.getTagCompound().getInteger("Matter");
		}
		return 0;
	}

	public void setMatter(ItemStack itemStack, float matter) {
		if (itemStack.getTagCompound() == null) {
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.getTagCompound().setFloat("Matter", matter);
	}

	public float getMaxMatter(ItemStack itemStack) {
		if (itemStack.getTagCompound() != null && itemStack.getTagCompound().hasKey("MaxMatter")) {
			return itemStack.getTagCompound().getFloat("MaxMatter");
		}
		return MAX_MATTER;
	}

	public boolean isStackListed(ItemStack decomposer, ItemStack itemStack) {
		if (decomposer.getTagCompound() != null && MatterHelper.containsMatter(itemStack)) {
			NBTTagList stackList = decomposer.getTagCompound().getTagList("Items", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < stackList.tagCount(); i++) {
				ItemStack s = new ItemStack(stackList.getCompoundTagAt(i));
				if (s.isItemEqual(itemStack) && ItemStack.areItemStackTagsEqual(s, itemStack)) {
					return true;
				}
			}
		}
		return false;
	}

	public void addStackToList(ItemStack decomposer, ItemStack itemStack) {
		if (decomposer.getTagCompound() == null) {
			decomposer.setTagCompound(new NBTTagCompound());
		}

		NBTTagList list = decomposer.getTagCompound().getTagList("Items", Constants.NBT.TAG_COMPOUND);
		if (MatterHelper.containsMatter(itemStack)) {
			NBTTagCompound tagCompound = new NBTTagCompound();
			itemStack.writeToNBT(tagCompound);
			list.appendTag(tagCompound);
		}
		decomposer.getTagCompound().setTag("Items", list);
	}

	public void decomposeItem(ItemStack decomposer, ItemStack itemStack) {
		if (MatterHelper.containsMatter(itemStack) && isStackListed(decomposer, itemStack)) {
			IEnergyStorage storage = getStorage(decomposer);
			float matterFromItem = MatterHelper.getMatterAmountFromItem(itemStack) * (float) MATTER_RATIO;
			int energyForItem = MathHelper.ceil(MatterHelper.getMatterAmountFromItem(itemStack) * ENERGY_COST_MULTIPLIER);
			float freeMatter = getMaxMatter(decomposer) - getMatter(decomposer);
			if (freeMatter > 0 && storage.getEnergyStored() > energyForItem) {
				int canTakeCount = (int) (freeMatter / matterFromItem);
				int itemsTaken = Math.min(canTakeCount, itemStack.getCount());
				itemsTaken = Math.min(itemsTaken, storage.getEnergyStored() / energyForItem);
				if (storage instanceof EnergyContainer)
					((EnergyContainer) storage).setEnergy(storage.getEnergyStored() - (itemsTaken * energyForItem));
				setMatter(decomposer, getMatter(decomposer) + itemsTaken * matterFromItem);
				itemStack.setCount(Math.max(0, itemStack.getCount() - itemsTaken));
			}
		}
	}

	@Override
	public void onConfigChanged(ConfigurationHandler config) {
		config.initMachineCategory("portable_decomposer");
		ENERGY_CAPACITY = config.getMachineInt("portable_decomposer", "storage.energy",
				128000, "Maximum energy storage capacity of the Portable Decomposer in RF");
		ENERGY_TRANSFER = config.getMachineInt("portable_decomposer", "transfer.energy",
				256, "RF per tick that can be inserted or extracted from the Portable Decomposer");
		MAX_MATTER = config.getMachineInt("portable_decomposer", "storage.matter",
				512, "Maximum mB of matter plasma the Portable Decomposer can hold internally");
		MATTER_RATIO = config.getMachineDouble("portable_decomposer", "ratio.matter",
				0.1, 0.0, 1.0, "Fraction of an item's matter value that is extracted (0.1 = 10%)");
		ENERGY_COST_MULTIPLIER = config.getMachineDouble("portable_decomposer", "cost.energy_multiplier",
				1.0, "Multiplier applied to an item's raw matter value to determine RF cost per decomposition");
	}
}