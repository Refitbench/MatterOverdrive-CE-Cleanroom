
package matteroverdrive.tile.pipes;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.api.matter.IMatterHandler;
import matteroverdrive.api.transport.IGridNode;
import matteroverdrive.data.MatterStorage;
import matteroverdrive.data.transport.FluidPipeNetwork;
import matteroverdrive.data.transport.IFluidPipe;
import matteroverdrive.init.MatterOverdriveCapabilities;
import matteroverdrive.init.OverdriveFluids;
import matteroverdrive.machines.MachineNBTCategory;
import matteroverdrive.machines.decomposer.TileEntityMachineDecomposer;
import matteroverdrive.network.packet.client.PacketMatterUpdate;
import matteroverdrive.util.TimeTracker;
import matteroverdrive.util.math.MOMathHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;

public class TileEntityMatterPipe extends TileEntityPipe implements IFluidPipe {
    public static Random rand = new Random();
    protected final MatterStorage storage;
    protected FluidPipeNetwork fluidPipeNetwork;
    protected int transferSpeed;
    TimeTracker t;
    private final Map<EnumFacing, BlockPos> transferTargets = new EnumMap<>(EnumFacing.class);
    private boolean transferTargetsDirty = true;

    /**
     * Back-pressure-aware IMatterHandler exposed to external pushers (e.g. decomposers).
     * Only accepts matter up to the total capacity available in the network's downstream
     * destinations, preventing the pipe buffer from absorbing matter that can never be
     * delivered when all downstream targets (e.g. a full replicator) are saturated.
     */
    private final IMatterHandler incomingMatterHandler = new IMatterHandler() {
        @Override
        public int receiveMatter(int amount, boolean simulate) {
            int downstream = getNetworkAvailableCapacity();
            int alreadyBuffered = storage.getMatterStored();
            int canAccept = Math.max(0, Math.min(amount, downstream - alreadyBuffered));
            if (canAccept <= 0) return 0;
            return storage.receiveMatter(canAccept, simulate);
        }

        @Override public int extractMatter(int amount, boolean simulate) { return storage.extractMatter(amount, simulate); }
        @Override public int getMatterStored() { return storage.getMatterStored(); }
        @Override public void setMatterStored(int amount) { storage.setMatterStored(amount); }
        @Override public int getCapacity() { return storage.getCapacity(); }
        @Override public void setCapacity(int capacity) { storage.setCapacity(capacity); }
        @Override public int modifyMatterStored(int amount) { return storage.modifyMatterStored(amount); }
        @Override public NBTTagCompound serializeNBT() { return storage.serializeNBT(); }
        @Override public void deserializeNBT(NBTTagCompound nbt) { storage.deserializeNBT(nbt); }
    };

    public TileEntityMatterPipe() {
        t = new TimeTracker();
        storage = new MatterStorage(32);
        this.transferSpeed = 10;
    }

    /**
     * Returns the total matter capacity available across all downstream destination
     * targets in this pipe's network. Used for back-pressure: the pipe refuses to
     * accept more from a source (decomposer) than what the network can actually deliver.
     */
    private int getNetworkAvailableCapacity() {
        if (fluidPipeNetwork == null) return 0;
        int total = 0;
        for (IFluidPipe pipe : fluidPipeNetwork.getNodes()) {
            TileEntityMatterPipe networkPipe = (TileEntityMatterPipe) pipe;
            if (networkPipe.transferTargetsDirty) {
                networkPipe.rebuildTransferTargets();
            }
            for (Map.Entry<EnumFacing, BlockPos> entry : networkPipe.transferTargets.entrySet()) {
                EnumFacing direction = entry.getKey();
                TileEntity te = networkPipe.getWorld().getTileEntity(entry.getValue());
                if (te != null && !te.isInvalid()
                        && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite())) {
                    total += te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite())
                            .fill(new FluidStack(OverdriveFluids.matterPlasma, Integer.MAX_VALUE / 2), false);
                }
            }
        }
        return total;
    }

    @Override
    public void update() {
        super.update();
        if (!world.isRemote) {
            manageTransfer();
            manageNetwork();
        }
    }

	public boolean establishConnectionFromSide(IBlockState blockState, EnumFacing side) {

		int connCount = getConnectionsCount();
		if (connCount < 1) {
			if (!MOMathHelper.getBoolean(getConnectionsMask(), side.ordinal())) {
				setConnection(side, true);
                markTransferTargetsDirty();
				world.markBlockRangeForRenderUpdate(pos, pos);
				return true;
			}
		}
		return false;
	}

    public void manageNetwork() {
        if (fluidPipeNetwork == null) {
            if (!tryConnectToNeighborNetworks(world)) {
                FluidPipeNetwork network = MatterOverdrive.FLUID_NETWORK_HANDLER.getNetwork(this);
                network.addNode(this);
            }
        }
    }

	public void manageTransfer() {
        if (transferTargetsDirty) {
            rebuildTransferTargets();
        }

		if (storage.getMatterStored() > 0 && getNetwork() != null) {
			for (IFluidPipe pipe : getNetwork().getNodes()) {
                TileEntityMatterPipe networkPipe = (TileEntityMatterPipe) pipe;
                if (networkPipe.transferTargetsDirty) {
                    networkPipe.rebuildTransferTargets();
                }

                for (Map.Entry<EnumFacing, BlockPos> entry : networkPipe.transferTargets.entrySet()) {
                    EnumFacing direction = entry.getKey();
                    TileEntity handler = networkPipe.getWorld().getTileEntity(entry.getValue());
                    if (handler == null || handler.isInvalid() || !handler.hasCapability(
                            CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite())) {
                        networkPipe.transferTargetsDirty = true;
                        continue;
                    }

                    int amount = storage.extractMatter(handler
                            .getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite())
                            .fill(new FluidStack(OverdriveFluids.matterPlasma, storage.getMatterStored()), true),
                            false);
                    if (amount != 0) {
                        if (handler.hasCapability(MatterOverdriveCapabilities.MATTER_HANDLER,
                                direction.getOpposite())) {
                            MatterOverdrive.NETWORK.sendToAllAround(new PacketMatterUpdate(handler), handler, 64);
                        }
                        if (storage.getMatterStored() <= 0) {
                            return;
                        }
                    }
				}
			}
		}
	}

    private void markTransferTargetsDirty() {
        transferTargetsDirty = true;
    }

    private void rebuildTransferTargets() {
        transferTargets.clear();
        for (EnumFacing direction : EnumFacing.VALUES) {
            if (!isConnectableSide(direction)) {
                continue;
            }

            BlockPos neighborPos = pos.offset(direction);
            TileEntity handler = world.getTileEntity(neighborPos);
            if (handler != null
                    && !(handler instanceof TileEntityMachineDecomposer)
                    && !(handler instanceof IFluidPipe)
                    && handler.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite())) {
                transferTargets.put(direction, neighborPos);
            }
        }
        transferTargetsDirty = false;
    }

    @Override
    public boolean canConnectToPipe(TileEntity entity, EnumFacing direction) {
        if (entity != null) {
            if (entity instanceof TileEntityMatterPipe) {
                if (this.getBlockType() != entity.getBlockType()) {
                    return false;
                }
                return true;
            }
            return entity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction);
        }
        return false;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound comp, EnumSet<MachineNBTCategory> categories, boolean toDisk) {
        super.writeCustomNBT(comp, categories, toDisk);
        if (!world.isRemote && categories.contains(MachineNBTCategory.DATA) && toDisk) {
            storage.writeToNBT(comp);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound comp, EnumSet<MachineNBTCategory> categories) {
        super.readCustomNBT(comp, categories);
        if (categories.contains(MachineNBTCategory.DATA)) {
            storage.readFromNBT(comp);
        }
    }

    @Override
    protected void onAwake(Side side) {

    }

    @Override
    public void onPlaced(World world, EntityLivingBase entityLiving) {

    }

	@Override
	public void onAdded(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote) {
			int connectionCount = 0;
			for (EnumFacing enumFacing : EnumFacing.VALUES) {
				BlockPos neighborPos = pos.offset(enumFacing);
				TileEntity tileEntityNeignbor = world.getTileEntity(neighborPos);
				IBlockState neighborState = world.getBlockState(neighborPos);
				if (tileEntityNeignbor instanceof IFluidPipe) {
					if (connectionCount < 2 && ((IFluidPipe) tileEntityNeignbor)
							.establishConnectionFromSide(neighborState, enumFacing.getOpposite())) {
						this.setConnection(enumFacing, true);
						world.markBlockRangeForRenderUpdate(pos, pos);
						connectionCount++;
					}
				}
			}
            markTransferTargetsDirty();
		}
	}

    public boolean tryConnectToNeighborNetworks(World world) {
        boolean hasConnected = false;
        for (EnumFacing side : EnumFacing.VALUES) {
            TileEntity neighborEntity = world.getTileEntity(pos.offset(side));
            if (neighborEntity instanceof TileEntityMatterPipe && this.getBlockType() == neighborEntity.getBlockType()) {
                if (((TileEntityMatterPipe) neighborEntity).getNetwork() != null && ((TileEntityMatterPipe) neighborEntity).getNetwork() != this.fluidPipeNetwork) {
                    ((TileEntityMatterPipe) neighborEntity).getNetwork().addNode(this);
                    hasConnected = true;
                }
            }
        }
        return hasConnected;
    }

    @Override
    public void onDestroyed(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            markTransferTargetsDirty();
            if (fluidPipeNetwork != null) {
                fluidPipeNetwork.onNodeDestroy(state, this);
            }

            for (EnumFacing enumFacing : EnumFacing.VALUES) {
                if (isConnectableSide(enumFacing)) {
                    TileEntity tileEntityConnection = worldIn.getTileEntity(pos.offset(enumFacing));
                    if (tileEntityConnection instanceof TileEntityMatterPipe) {
                        ((TileEntityMatterPipe) tileEntityConnection).breakConnection(state, enumFacing.getOpposite());
                    }
                }
            }
        }
    }
    
    @Override
    public void onChunkUnload() {
        if (!world.isRemote) {
            markTransferTargetsDirty();
            IBlockState blockState = world.getBlockState(getPos());
            if (fluidPipeNetwork != null) {
                fluidPipeNetwork.onNodeDestroy(blockState, this);
            }
        }
    }

	public void breakConnection(IBlockState blockState, EnumFacing side) {
		setConnection(side, false);
        markTransferTargetsDirty();
		world.markBlockRangeForRenderUpdate(pos, pos);
	}

    @Override
    public void onNeighborBlockChange(IBlockAccess world, BlockPos pos, IBlockState state, Block neighborBlock) {
        markTransferTargetsDirty();
        queueUpdate();
    }

    @Override
    public void setConnections(int connections, boolean notify) {
        super.setConnections(connections, notify);
        if (getConnectionsMask() == connections) {
            markTransferTargetsDirty();
        }
    }

    @Override
    public void writeToDropItem(ItemStack itemStack) {

    }

    @Override
    public void readFromPlaceItem(ItemStack itemStack) {

    }

    @Override
    public TileEntity getTile() {
        return this;
    }

    @Override
    public FluidPipeNetwork getNetwork() {
        return fluidPipeNetwork;
    }

    @Override
    public void setNetwork(FluidPipeNetwork network) {
        this.fluidPipeNetwork = network;
    }

    @Override
    public BlockPos getNodePos() {
        return getPos();
    }

    @Override
    public World getNodeWorld() {
        return getWorld();
    }

    @Override
    public boolean canConnectToNetworkNode(IBlockState blockState, IGridNode toNode, EnumFacing direction) {
    	return isConnectableSide(direction);
    }

    @Override
    public boolean canConnectFromSide(IBlockState blockState, EnumFacing side) {
        return true;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == MatterOverdriveCapabilities.MATTER_HANDLER) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == MatterOverdriveCapabilities.MATTER_HANDLER) {
            return (T) incomingMatterHandler;
        }
        return super.getCapability(capability, facing);
    }
}