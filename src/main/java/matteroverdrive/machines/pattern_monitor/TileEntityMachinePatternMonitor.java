
package matteroverdrive.machines.pattern_monitor;

import matteroverdrive.api.inventory.UpgradeTypes;
import matteroverdrive.api.matter.IMatterDatabase;
import matteroverdrive.api.matter_network.IMatterNetworkClient;
import matteroverdrive.api.matter_network.IMatterNetworkComponent;
import matteroverdrive.api.matter_network.IMatterNetworkConnection;
import matteroverdrive.api.network.IMatterNetworkDispatcher;
import matteroverdrive.api.transport.IGridNode;
import matteroverdrive.container.matter_network.IMatterDatabaseMonitor;
import matteroverdrive.data.transport.MatterNetwork;
import matteroverdrive.machines.MOTileEntityMachine;
import matteroverdrive.machines.MachineNBTCategory;
import matteroverdrive.machines.components.ComponentMatterNetworkConfigs;
import matteroverdrive.machines.events.MachineEvent;
import matteroverdrive.machines.replicator.ComponentTaskProcessingReplicator;
import matteroverdrive.matter_network.MatterNetworkTaskQueue;
import matteroverdrive.matter_network.tasks.MatterNetworkTaskReplicatePattern;
import matteroverdrive.machines.replicator.TileEntityMachineReplicator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class TileEntityMachinePatternMonitor extends MOTileEntityMachine
		implements IMatterNetworkClient, IMatterDatabaseMonitor, IMatterNetworkDispatcher, IMatterNetworkConnection {
	private ComponentMatterNetworkPatternMonitor networkComponent;
	private ComponentMatterNetworkConfigs componentMatterNetworkConfigs;
	private ComponentTaskProcessingPatternMonitor taskProcessingComponent;
	private final MatterNetworkTaskQueue<MatterNetworkTaskReplicatePattern> networkActiveTaskQueue =
			new MatterNetworkTaskQueue<MatterNetworkTaskReplicatePattern>("Network Active Tasks", 64) {
				@Override
				public MatterNetworkTaskReplicatePattern dropAt(int i) {
					MatterNetworkTaskReplicatePattern task = super.dropAt(i);
					if (task != null) {
						propagateRemovalToReplicators(task);
					}
					return task;
				}

				@Override
				public MatterNetworkTaskReplicatePattern moveToFront(int i) {
					MatterNetworkTaskReplicatePattern task = getAt(i);
					if (task != null) {
						super.moveToFront(i);
						propagateMoveToFront(task);
					}
					return task;
				}
			};
	private int count = 0;

	public TileEntityMachinePatternMonitor() {
		super(4);
		playerSlotsHotbar = true;
	}

	@Override
	public BlockPos getPosition() {
		return getPos();
	}

	@Override
	protected void registerComponents() {
		super.registerComponents();
		networkComponent = new ComponentMatterNetworkPatternMonitor(this);
		componentMatterNetworkConfigs = new ComponentMatterNetworkConfigs(this);
		taskProcessingComponent = new ComponentTaskProcessingPatternMonitor("Replication Tasks", this, 8, 0);
		addComponent(networkComponent);
		addComponent(componentMatterNetworkConfigs);
		addComponent(taskProcessingComponent);
	}

	@Override
	protected void onMachineEvent(MachineEvent event) {

	}

	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 0;
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, EnumSet<MachineNBTCategory> categories, boolean toDisk) {
		super.writeCustomNBT(nbt, categories, toDisk);

		if (categories.contains(MachineNBTCategory.GUI)) {
			nbt.setInteger("Count", count);
		}
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, EnumSet<MachineNBTCategory> categories) {
		super.readCustomNBT(nbt, categories);
		if (categories.contains(MachineNBTCategory.GUI)) {
			count = nbt.getInteger("Count");
		}
	}

	public int getCount() {
		return count;
	}

	public void setCount(int Count) {
		this.count = Count;
	}

	@Override
	public boolean canConnectFromSide(IBlockState blockState, EnumFacing side) {
		return true;
	}

	@Override
	public BlockPos getNodePos() {
		return getPos();
	}

	@Override
	public boolean establishConnectionFromSide(IBlockState blockState, EnumFacing side) {
		return networkComponent.establishConnectionFromSide(blockState, side);
	}

	@Override
	public void breakConnection(IBlockState blockState, EnumFacing side) {
		networkComponent.breakConnection(blockState, side);
	}

	@Override
	public MatterNetwork getNetwork() {
		return networkComponent.getNetwork();
	}

	@Override
	public void setNetwork(MatterNetwork network) {
		networkComponent.setNetwork(network);
	}

	@Override
	public World getNodeWorld() {
		return getWorld();
	}

	@Override
	public boolean canConnectToNetworkNode(IBlockState blockState, IGridNode toNode, EnumFacing direction) {
		return networkComponent.canConnectToNetworkNode(blockState, toNode, direction);
	}

	@Override
	public List<IMatterDatabase> getConnectedDatabases() {
		List<IMatterNetworkClient> clients = getNetwork().getClients();
		List<IMatterDatabase> databases = new ArrayList<>();
		for (IMatterNetworkClient client : clients) {
			if (client instanceof IMatterDatabase) {
				databases.add((IMatterDatabase) client);
			}
		}
		return databases;
	}

	@Override
	public SoundEvent getSound() {
		return null;
	}

	@Override
	public boolean hasSound() {
		return false;
	}

	@Override
	public boolean getServerActive() {
		return false;
	}

	@Override
	public float soundVolume() {
		return 0;
	}

	public boolean isAffectedByUpgrade(UpgradeTypes type) {
		return false;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		return new int[0];
	}

	@Override
	public IMatterNetworkComponent getMatterNetworkComponent() {
		return networkComponent;
	}

	@Override
	public MatterNetworkTaskQueue<?> getTaskQueue(int queueID) {
		if (queueID == 1) {
			return networkActiveTaskQueue;
		}
		return taskProcessingComponent.getTaskQueue();
	}

	@Override
	public int getTaskQueueCount() {
		return 2;
	}

	@SuppressWarnings("unchecked")
	private void propagateRemovalToReplicators(MatterNetworkTaskReplicatePattern task) {
		if (taskProcessingComponent.getTaskQueue().remove(task)) {
			taskProcessingComponent.sendTaskQueueRemovedFromWatchers(task.getId());
		}

		if (getNetwork() == null) {
			return;
		}
		for (IMatterNetworkClient client : getNetwork().getClients()) {
			if (client instanceof TileEntityMachineReplicator) {
				MatterNetworkTaskQueue<MatterNetworkTaskReplicatePattern> repQueue =
						(MatterNetworkTaskQueue<MatterNetworkTaskReplicatePattern>)
						((TileEntityMachineReplicator) client).getTaskQueue(0);
				if (repQueue.remove(task)) {
					((TileEntityMachineReplicator) client).getComponent(ComponentTaskProcessingReplicator.class)
							.sendTaskQueueRemovedFromWatchers(task.getId());
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void propagateMoveToFront(MatterNetworkTaskReplicatePattern task) {
		if (taskProcessingComponent.getTaskQueue().moveToFront(task)) {
			taskProcessingComponent.sendTaskQueueChangedToWatchers(task.getId());
			return;
		}

		if (getNetwork() == null) {
			return;
		}

		for (IMatterNetworkClient client : getNetwork().getClients()) {
			if (client instanceof TileEntityMachineReplicator) {
				TileEntityMachineReplicator replicator = (TileEntityMachineReplicator) client;
				MatterNetworkTaskQueue<MatterNetworkTaskReplicatePattern> repQueue =
						(MatterNetworkTaskQueue<MatterNetworkTaskReplicatePattern>) replicator.getTaskQueue(0);
				if (repQueue.moveToFront(task)) {
					replicator.getComponent(ComponentTaskProcessingReplicator.class)
							.sendTaskQueueChangedToWatchers(task.getId());
					return;
				}
			}
		}
	}

}
