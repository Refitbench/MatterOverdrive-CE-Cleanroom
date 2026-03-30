
package matteroverdrive.tile;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.data.Inventory;
import matteroverdrive.data.MachineEnergyStorage;
import matteroverdrive.data.inventory.EnergySlot;
import matteroverdrive.machines.MOTileEntityMachine;
import matteroverdrive.machines.MachineNBTCategory;
import matteroverdrive.network.packet.client.PacketPowerUpdate;
import matteroverdrive.util.MOEnergyHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;

public abstract class MOTileEntityMachineEnergy extends MOTileEntityMachine {
	public static final int ENERGY_CLIENT_SYNC_RANGE = 16;
	public static final int ENERGY_CLIENT_SYNC_INTERVAL_TICKS = 5;
	protected MachineEnergyStorage<MOTileEntityMachineEnergy> energyStorage;
	protected int energySlotID;
	private int lastSyncedEnergy = Integer.MIN_VALUE;
	private long nextEnergySyncTick = Long.MIN_VALUE;
	private NetworkRegistry.TargetPoint energySyncTargetPoint;

	public MOTileEntityMachineEnergy(int upgradeCount) {
		super(upgradeCount);
		this.energyStorage = new MachineEnergyStorage<>(512, 512, 512, this);
	}

	@Override
	protected void RegisterSlots(Inventory inventory) {
		energySlotID = inventory.AddSlot(new EnergySlot(true));
		super.RegisterSlots(inventory);
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, EnumSet<MachineNBTCategory> categories, boolean toDisk) {
		super.writeCustomNBT(nbt, categories, toDisk);
		if (categories.contains(MachineNBTCategory.DATA)) {
			NBTTagCompound energy = energyStorage.serializeNBT();
			nbt.setTag("Energy", energy);
		}
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, EnumSet<MachineNBTCategory> categories) {
		super.readCustomNBT(nbt, categories);
		if (categories.contains(MachineNBTCategory.DATA)) {
			energyStorage.deserializeNBT(nbt.getCompoundTag("Energy"));
		}
	}

	public void update() {
		super.update();
		manageCharging();
	}

	protected void manageCharging() {
		if (this.world.isRemote) {
			return;
		}

		ItemStack chargeStack = this.inventory.getStackInSlot(energySlotID);
		if (chargeStack.isEmpty() || !MOEnergyHelper.isEnergyContainerItem(chargeStack)) {
			return;
		}

		int freeSpace = getFreeEnergySpace();
		if (freeSpace <= 0) {
			return;
		}

		int extracted = MOEnergyHelper.extractEnergyFromContainer(chargeStack, freeSpace, false);
		if (extracted > 0) {
			getEnergyStorage().receiveEnergy(extracted, false);
		}
	}

	public boolean isCharging() {
		return !this.inventory.getStackInSlot(energySlotID).isEmpty()
				&& MOEnergyHelper.isEnergyContainerItem(this.inventory.getStackInSlot(energySlotID))
				&& this.inventory.getStackInSlot(energySlotID).getCapability(CapabilityEnergy.ENERGY, null)
						.extractEnergy(getFreeEnergySpace(), true) > 0;
	}

	public int getEnergySlotID() {
		return this.energySlotID;
	}

	public MachineEnergyStorage<MOTileEntityMachineEnergy> getEnergyStorage() {
		return this.energyStorage;
	}

	public int getFreeEnergySpace() {
		return getEnergyStorage().getMaxEnergyStored() - getEnergyStorage().getEnergyStored();
	}

	public void UpdateClientPower() {
		UpdateClientPower(false);
	}

	public void UpdateClientPower(boolean force) {
		if (world == null || world.isRemote) {
			return;
		}

		int currentEnergy = energyStorage.getEnergyStored();
		boolean changed = currentEnergy != lastSyncedEnergy;
		long now = world.getTotalWorldTime();

		if (!force) {
			if (!changed) {
				return;
			}
			boolean boundaryValue = currentEnergy == 0 || currentEnergy == energyStorage.getMaxEnergyStored();
			if (now < nextEnergySyncTick && !boundaryValue) {
				return;
			}
		}

		MatterOverdrive.NETWORK.sendToAllAround(new PacketPowerUpdate(this), getEnergySyncTargetPoint());
		lastSyncedEnergy = currentEnergy;
		nextEnergySyncTick = now + ENERGY_CLIENT_SYNC_INTERVAL_TICKS;
	}

	private NetworkRegistry.TargetPoint getEnergySyncTargetPoint() {
		if (energySyncTargetPoint == null) {
			energySyncTargetPoint = new NetworkRegistry.TargetPoint(world.provider.getDimension(),
					getPos().getX(), getPos().getY(), getPos().getZ(), ENERGY_CLIENT_SYNC_RANGE);
		}
		return energySyncTargetPoint;
	}

	@Override
	public void readFromPlaceItem(ItemStack itemStack) {
		super.readFromPlaceItem(itemStack);

		if (!itemStack.isEmpty()) {
			if (itemStack.hasTagCompound()) {
				energyStorage.deserializeNBT(itemStack.getTagCompound().getCompoundTag("Energy"));
			}
		}
	}

	@Override
	public void writeToDropItem(ItemStack itemStack) {
		super.writeToDropItem(itemStack);

		if (!itemStack.isEmpty()) {
			if (energyStorage.getEnergyStored() > 0) {
				if (!itemStack.hasTagCompound()) {
					itemStack.setTagCompound(new NBTTagCompound());
				}

				NBTTagCompound energy = energyStorage.serializeNBT();
				itemStack.getTagCompound().setTag("Energy", energy);
			}
		}
	}

	@Override
	public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityEnergy.ENERGY) {
			return true;
		}

		return super.hasCapability(capability, facing);
	}

	@Nonnull
	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityEnergy.ENERGY) {
			return CapabilityEnergy.ENERGY.cast(energyStorage);
		}

		return super.getCapability(capability, facing);
	}
}