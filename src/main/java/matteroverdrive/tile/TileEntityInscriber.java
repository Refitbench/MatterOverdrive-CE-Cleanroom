package matteroverdrive.tile;

import java.util.EnumSet;
import java.util.Optional;

import matteroverdrive.api.inventory.UpgradeTypes;
import matteroverdrive.blocks.BlockInscriber;
import matteroverdrive.client.render.RenderParticlesHandler;
import matteroverdrive.data.Inventory;
import matteroverdrive.data.inventory.InscriberSlot;
import matteroverdrive.data.inventory.RemoveOnlySlot;
import matteroverdrive.data.recipes.InscriberRecipe;
import matteroverdrive.fx.InscriberSparkParticle;
import matteroverdrive.init.MatterOverdriveRecipes;
import matteroverdrive.init.MatterOverdriveSounds;
import matteroverdrive.machines.MachineNBTCategory;
import matteroverdrive.machines.events.MachineEvent;
import matteroverdrive.proxy.ClientProxy;
import matteroverdrive.util.math.MOMathHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityInscriber extends MOTileEntityMachineEnergy {
	public static int ENERGY_CAPACITY = 512000;
	public static int ENERGY_TRANSFER = 512000;
	public static double ENERGY_USAGE_MULTIPLIER = 1.0;
	private static final EnumSet<UpgradeTypes> upgradeTypes = EnumSet.of(UpgradeTypes.PowerUsage, UpgradeTypes.Speed,
			UpgradeTypes.PowerStorage, UpgradeTypes.PowerTransfer, UpgradeTypes.Muffler);
	public static int MAIN_INPUT_SLOT_ID, SEC_INPUT_SLOT_ID, OUTPUT_SLOT_ID;
	@SideOnly(Side.CLIENT)
	private float nextHeadX, nextHeadY;
	@SideOnly(Side.CLIENT)
	private float lastHeadX, lastHeadY;
	@SideOnly(Side.CLIENT)
	private float headAnimationTime;
	private int inscribeTime;
	private InscriberRecipe cachedRecipe;

	public TileEntityInscriber() {
		super(4);
		this.energyStorage.setCapacity(ENERGY_CAPACITY);
		this.energyStorage.setMaxExtract(ENERGY_TRANSFER);
		this.energyStorage.setMaxReceive(ENERGY_TRANSFER);
		playerSlotsHotbar = true;
		playerSlotsMain = true;
	}

	@Override
	protected void RegisterSlots(Inventory inventory) {
		MAIN_INPUT_SLOT_ID = inventory.AddSlot(new InscriberSlot(true, false).setSendToClient(true));
		SEC_INPUT_SLOT_ID = inventory.AddSlot(new InscriberSlot(true, true));
		OUTPUT_SLOT_ID = inventory.AddSlot(new RemoveOnlySlot(false).setSendToClient(true));
		super.RegisterSlots(inventory);
	}

	protected void manageInscription() {
		if (!world.isRemote) {
			if (this.isInscribing()) {
				if (this.energyStorage.getEnergyStored() >= getEnergyDrainPerTick()) {
					this.inscribeTime++;
					energyStorage.modifyEnergyStored(-getEnergyDrainPerTick());

					if (this.inscribeTime >= getSpeed()) {
						this.inscribeTime = 0;
						this.inscribeItem();
					}
				}
			}
		}

		if (!this.isInscribing()) {
			this.inscribeTime = 0;
		}
	}

	public boolean canPutInOutput() {
		ItemStack outputStack = inventory.getStackInSlot(OUTPUT_SLOT_ID);
		return outputStack.isEmpty() || (cachedRecipe != null && outputStack.isItemEqual(cachedRecipe.getOutput(this)));
	}

	public void inscribeItem() {
		if (cachedRecipe != null && canPutInOutput()) {
			ItemStack outputSlot = inventory.getStackInSlot(OUTPUT_SLOT_ID);
			if (!outputSlot.isEmpty()) {
				outputSlot.grow(1);
			} else {
				inventory.setInventorySlotContents(OUTPUT_SLOT_ID, cachedRecipe.getOutput(this));
			}

			inventory.decrStackSize(MAIN_INPUT_SLOT_ID, 1);
			inventory.decrStackSize(SEC_INPUT_SLOT_ID, 1);

			calculateRecipe();
		}
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, EnumSet<MachineNBTCategory> categories, boolean toDisk) {
		super.writeCustomNBT(nbt, categories, toDisk);
		if (categories.contains(MachineNBTCategory.DATA)) {
			nbt.setInteger("inscribeTime", inscribeTime);
		}
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, EnumSet<MachineNBTCategory> categories) {
		super.readCustomNBT(nbt, categories);
		if (categories.contains(MachineNBTCategory.DATA)) {
			inscribeTime = nbt.getInteger("inscribeTime");
		}
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		// Only re-create the tile entity when the block type actually changes,
		// not for property-only updates like the ACTIVE flag flip.
		return !(newState.getBlock() instanceof BlockInscriber);
	}

	@Override
	public boolean getServerActive() {
		return isInscribing() && this.energyStorage.getEnergyStored() >= getEnergyDrainPerTick();
	}

	public int getEnergyDrainPerTick() {
		int maxEnergy = getEnergyDrainMax();
		int speed = getSpeed();
		if (speed > 0) {
			return maxEnergy / speed;
		}
		return 0;
	}

	public int getEnergyDrainMax() {
		if (cachedRecipe != null) {
			return (int) (cachedRecipe.getEnergy() * ENERGY_USAGE_MULTIPLIER * getUpgradeMultiply(UpgradeTypes.PowerUsage));
		}
		return 0;
	}

	public int getSpeed() {
		if (cachedRecipe != null) {
			return (int) (cachedRecipe.getTime() * getUpgradeMultiply(UpgradeTypes.Speed));
		}
		return 0;
	}

	public boolean isInscribing() {
		return cachedRecipe != null && canPutInOutput();
	}

	@Override
	public SoundEvent getSound() {
		return MatterOverdriveSounds.electricMachine;
	}

	@Override
	public boolean hasSound() {
		return true;
	}

	@Override
	public float soundVolume() {
		if (getUpgradeMultiply(UpgradeTypes.Muffler) >= 2d) {
			return 0.0f;
		}

		return 1;
	}

	@Override
	public void update() {
		super.update();
		if (world.isRemote && isActive()) {
			handleHeadAnimation();
			spawnInscriberEffects();
		}
		manageInscription();
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack item, EnumFacing side) {
		return slot == OUTPUT_SLOT_ID;
	}

	@Override
	public boolean isAffectedByUpgrade(UpgradeTypes type) {
		return upgradeTypes.contains(type);
	}

	@Override
	protected void onMachineEvent(MachineEvent event) {
		if (event instanceof MachineEvent.Awake) {
			calculateRecipe();
		}
		if (event instanceof MachineEvent.ActiveChange && !world.isRemote) {
			// setBlockState with a genuinely changed ACTIVE property triggers
			// automatic checkLight + client chunk update on both sides.
			BlockInscriber.setActive(isActive(), world, getPos());
		}
	}

	@Override
	public float getProgress() {
		float speed = (float) getSpeed();
		if (speed > 0) {
			return (float) (inscribeTime) / speed;
		}
		return 0;
	}

	@SideOnly(Side.CLIENT)
	protected void handleHeadAnimation() {
		if (headAnimationTime >= 1) {
			lastHeadX = nextHeadX;
			lastHeadY = nextHeadY;
			nextHeadX = MathHelper.clamp((float) random.nextGaussian(), -1, 1);
			nextHeadY = MathHelper.clamp((float) random.nextGaussian(), -1, 1);
			headAnimationTime = 0;
		}

		headAnimationTime += 0.05f;
	}

	@SideOnly(Side.CLIENT)
	private void spawnInscriberEffects() {
		// Fire every 3 ticks; 25% random chance on off-ticks for an irregular rhythm.
		long wt = world.getWorldTime();
		boolean onBeat    = (wt % 3 == 0);
		boolean randomHit = !onBeat && (random.nextFloat() < 0.25f);
		if (!onBeat && !randomHit) return;

		double cx = getPos().getX() + 0.5;
		double cy = getPos().getY();
		double cz = getPos().getZ() + 0.5;
		double coneY = cy + 0.750;

		// Brief contact-arc glow at cone tip
		InscriberSparkParticle flash = new InscriberSparkParticle(
				world,
				cx + (random.nextFloat() - 0.5) * 0.02,
				coneY,
				cz + (random.nextFloat() - 0.5) * 0.02,
				0, 0.002, 0,
				1.0f, 0.80f, 0.40f,
				0.35f, 2);
		ClientProxy.renderHandler.getRenderParticlesHandler()
				.addEffect(flash, RenderParticlesHandler.Blending.Additive);

		// Spark burst: count and colour warmth vary each burst for irregular feel
		int count = 3 + random.nextInt(6); // 3-8 sparks
		float warmth = 0.35f + random.nextFloat() * 0.60f; // orange → near-white per burst
		for (int i = 0; i < count; i++) {
			double sx = cx + (random.nextFloat() - 0.5) * 0.05;
			double sy = coneY + (random.nextFloat() - 0.5) * 0.015;
			double sz = cz + (random.nextFloat() - 0.5) * 0.05;

			double angle  = random.nextDouble() * Math.PI * 2;
			double hSpeed = 0.07 + random.nextDouble() * 0.12; // 0.07-0.19 radial
			double vx = Math.cos(angle) * hSpeed;
			double vy = 0.04 + random.nextDouble() * 0.05;    // 0.04-0.09, peaks below arm
			double vz = Math.sin(angle) * hSpeed;

			float r = 1.0f;
			float g = warmth + random.nextFloat() * (1.0f - warmth) * 0.35f;
			float b = random.nextFloat() * 0.10f;

			InscriberSparkParticle spark = new InscriberSparkParticle(
					world, sx, sy, sz, vx, vy, vz, r, g, b);
			ClientProxy.renderHandler.getRenderParticlesHandler()
					.addEffect(spark, RenderParticlesHandler.Blending.Additive);
		}
	}

	@SideOnly(Side.CLIENT)
	public float geatHeadX() {
		return MOMathHelper.Lerp(lastHeadX, nextHeadX, headAnimationTime);
	}

	@SideOnly(Side.CLIENT)
	public float geatHeadY() {
		return MOMathHelper.Lerp(lastHeadY, nextHeadY, headAnimationTime);
	}

	public void calculateRecipe() {
		ItemStack mainStack = inventory.getStackInSlot(MAIN_INPUT_SLOT_ID);
		ItemStack secStack = inventory.getStackInSlot(SEC_INPUT_SLOT_ID);
		if (!mainStack.isEmpty() && !secStack.isEmpty()) {
			Optional<InscriberRecipe> recipe = MatterOverdriveRecipes.INSCRIBER.get(this);
			cachedRecipe = recipe.orElse(null);
			return;
		}
		cachedRecipe = null;
	}

	@Override
	public ItemStack decrStackSize(int slot, int size) {
		ItemStack stack = super.decrStackSize(slot, size);
		calculateRecipe();
		return stack;
	}

	public void setInventorySlotContents(int slot, ItemStack itemStack) {
		super.setInventorySlotContents(slot, itemStack);
		calculateRecipe();
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		return new int[] { MAIN_INPUT_SLOT_ID, SEC_INPUT_SLOT_ID, OUTPUT_SLOT_ID };
	}

}
