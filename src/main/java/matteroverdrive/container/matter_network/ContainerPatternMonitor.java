
package matteroverdrive.container.matter_network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.api.matter.IMatterDatabase;
import matteroverdrive.api.matter.IMatterPatternStorage;
import matteroverdrive.api.matter_network.IMatterNetworkClient;
import matteroverdrive.api.network.IMatterNetworkDispatcher;
import matteroverdrive.data.matter_network.ItemPattern;
import matteroverdrive.data.matter_network.ItemPatternMapping;
import matteroverdrive.data.matter_network.MatterDatabaseEvent;
import matteroverdrive.gui.GuiPatternMonitor;
import matteroverdrive.machines.MOTileEntityMachine;
import matteroverdrive.machines.pattern_monitor.TileEntityMachinePatternMonitor;
import matteroverdrive.machines.replicator.TileEntityMachineReplicator;
import matteroverdrive.matter_network.MatterNetworkTaskQueue;
import matteroverdrive.matter_network.tasks.MatterNetworkTaskReplicatePattern;
import matteroverdrive.network.packet.client.pattern_monitor.PacketClearPatterns;
import matteroverdrive.network.packet.client.pattern_monitor.PacketSendItemPattern;
import matteroverdrive.network.packet.client.task_queue.PacketSyncTaskQueue;
import matteroverdrive.util.MOContainerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class ContainerPatternMonitor extends ContainerTaskQueueMachine<TileEntityMachinePatternMonitor>
		implements IMatterDatabaseWatcher {

	private final List<TileEntityMachineReplicator> watchedReplicators = new ArrayList<>();

	public ContainerPatternMonitor(InventoryPlayer inventory, TileEntityMachinePatternMonitor machine) {
		super(inventory, machine);
	}

	@Override
	public void init(InventoryPlayer inventory) {
		addAllSlotsFromInventory(machine.getInventoryContainer());
		MOContainerHelper.AddPlayerSlots(inventory, this, 45, 89, false, true);
	}

	@SideOnly(Side.CLIENT)
	public void setItemPattern(ItemPatternMapping itemPattern) {
		if (Minecraft.getMinecraft().currentScreen instanceof GuiPatternMonitor) {
			((GuiPatternMonitor) Minecraft.getMinecraft().currentScreen).setPattern(itemPattern);
		}
	}

	@SideOnly(Side.CLIENT)
	public void clearPatternStoragePatterns(BlockPos database, int storageId) {
		if (Minecraft.getMinecraft().currentScreen instanceof GuiPatternMonitor) {
			((GuiPatternMonitor) Minecraft.getMinecraft().currentScreen).clearPatterns(database, storageId);
		}
	}

	@SideOnly(Side.CLIENT)
	public void clearDatabasePatterns(BlockPos blockPos) {
		if (Minecraft.getMinecraft().currentScreen instanceof GuiPatternMonitor) {
			((GuiPatternMonitor) Minecraft.getMinecraft().currentScreen).clearPatterns(blockPos);
		}
	}

	@SideOnly(Side.CLIENT)
	public void clearAllPatterns() {
		if (Minecraft.getMinecraft().currentScreen instanceof GuiPatternMonitor) {
			((GuiPatternMonitor) Minecraft.getMinecraft().currentScreen).clearPatterns();
		}
	}

	@Override
	public void onWatcherAdded(MOTileEntityMachine machine) {
		if (machine == this.machine) {
			// Normal handling for our own monitor machine
			super.onWatcherAdded(machine);
			if (machine instanceof IMatterDatabaseMonitor) {
				sendAllPatterns((IMatterDatabaseMonitor) machine);
			}
			refreshReplicatorWatchers();
			sendNetworkTaskQueue();
		}
		// For replicators: called by our own addWatcher above.
		// Don't call super - we don't want individual replicator queues sent to the client.
	}

	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		for (TileEntityMachineReplicator replicator : watchedReplicators) {
			replicator.removeWatcher(this);
		}
		watchedReplicators.clear();
	}

	@Override
	public void onTaskAdded(IMatterNetworkDispatcher dispatcher, long taskId, int queueId) {
		sendNetworkTaskQueue();
	}

	@Override
	public void onTaskRemoved(IMatterNetworkDispatcher dispatcher, long taskId, int queueId) {
		sendNetworkTaskQueue();
	}

	@Override
	public void onTaskChanged(IMatterNetworkDispatcher dispatcher, long taskId, int queueId) {
		sendNetworkTaskQueue();
	}

	private void refreshReplicatorWatchers() {
		for (TileEntityMachineReplicator replicator : watchedReplicators) {
			replicator.removeWatcher(this);
		}
		watchedReplicators.clear();

		if (machine.getNetwork() == null) {
			return;
		}

		for (IMatterNetworkClient client : machine.getNetwork().getClients()) {
			if (client instanceof TileEntityMachineReplicator) {
				TileEntityMachineReplicator replicator = (TileEntityMachineReplicator) client;
				watchedReplicators.add(replicator);
				replicator.addWatcher(this);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void sendNetworkTaskQueue() {
		MatterNetworkTaskQueue<MatterNetworkTaskReplicatePattern> netQueue =
				(MatterNetworkTaskQueue<MatterNetworkTaskReplicatePattern>) machine.getTaskQueue(1);
		netQueue.clear();
		for (TileEntityMachineReplicator replicator : watchedReplicators) {
			MatterNetworkTaskQueue<?> repQueue = replicator.getTaskQueue(0);
			for (int i = 0; i < repQueue.size(); i++) {
				Object task = repQueue.getAt(i);
				if (task instanceof MatterNetworkTaskReplicatePattern) {
					netQueue.queue((MatterNetworkTaskReplicatePattern) task);
				}
			}
		}
		MatterNetworkTaskQueue<?> pendingQueue = machine.getTaskQueue(0);
		for (int i = 0; i < pendingQueue.size(); i++) {
			Object task = pendingQueue.getAt(i);
			if (task instanceof MatterNetworkTaskReplicatePattern) {
				netQueue.queue((MatterNetworkTaskReplicatePattern) task);
			}
		}
		MatterOverdrive.NETWORK.sendTo(new PacketSyncTaskQueue(machine, 1), (EntityPlayerMP) getPlayer());
	}

	private void sendAllPatterns(IMatterDatabaseMonitor monitor) {
		for (IMatterDatabase database : monitor.getConnectedDatabases()) {
			for (int d = 0; d < database.getPatternStorageCount(); d++) {
				ItemStack storageStack = database.getPatternStorage(d);
				if (storageStack != null) {
					IMatterPatternStorage storage = (IMatterPatternStorage) storageStack.getItem();
					for (int i = 0; i < storage.getCapacity(storageStack); i++) {
						MatterOverdrive.NETWORK.sendTo(
								new PacketSendItemPattern(windowId, new ItemPatternMapping(
										storage.getPatternAt(storageStack, i), database.getPosition(), d, i)),
								(EntityPlayerMP) getPlayer());
					}
				}
			}

		}
	}

	@Override
	public void onConnectToNetwork(IMatterDatabaseMonitor monitor) {
		refreshReplicatorWatchers();
		sendAllPatterns(monitor);
		sendNetworkTaskQueue();
	}

	@Override
	public void onDisconnectFromNetwork(IMatterDatabaseMonitor monitor) {
		refreshReplicatorWatchers();
		sendNetworkTaskQueue();
		MatterOverdrive.NETWORK.sendTo(new PacketClearPatterns(windowId), (EntityPlayerMP) getPlayer());
	}

	@Override
	public void onDatabaseEvent(MatterDatabaseEvent event) {
		if (event instanceof MatterDatabaseEvent.Added) {
			onDatabaseAdded(event.database);
		} else if (event instanceof MatterDatabaseEvent.Removed) {
			onDatabaseRemoved(event.database);
		} else if (event instanceof MatterDatabaseEvent.PatternStorageChanged) {
			onPatternStorageChange(event.database, ((MatterDatabaseEvent.PatternStorageChanged) event).storageID);
		} else if (event instanceof MatterDatabaseEvent.PatternChanged) {
			onPatternChange(event.database, ((MatterDatabaseEvent.PatternChanged) event).patternStorageId,
					((MatterDatabaseEvent.PatternChanged) event).patternId);
		}
	}

	private void onDatabaseAdded(IMatterDatabase database) {
		for (int d = 0; d < database.getPatternStorageCount(); d++) {
			ItemStack storageStack = database.getPatternStorage(d);
			IMatterPatternStorage storage = (IMatterPatternStorage) storageStack.getItem();
			for (int i = 0; i < storage.getCapacity(storageStack); i++) {
				MatterOverdrive.NETWORK.sendTo(
						new PacketSendItemPattern(windowId, new ItemPatternMapping(
								storage.getPatternAt(storageStack, i), database.getPosition(), d, i)),
						(EntityPlayerMP) getPlayer());
			}
		}
	}

	private void onDatabaseRemoved(IMatterDatabase database) {
		MatterOverdrive.NETWORK.sendTo(new PacketClearPatterns(windowId, database.getPosition()),
				(EntityPlayerMP) getPlayer());
	}

	private void onPatternStorageChange(IMatterDatabase database, int patternStorage) {
		MatterOverdrive.NETWORK.sendTo(new PacketClearPatterns(windowId, database.getPosition(), patternStorage),
				(EntityPlayerMP) getPlayer());
		ItemStack storageStack = database.getPatternStorage(patternStorage);
		if (storageStack != null) {
			IMatterPatternStorage storage = (IMatterPatternStorage) storageStack.getItem();
			for (int i = 0; i < storage.getCapacity(storageStack); i++) {
				MatterOverdrive.NETWORK.sendTo(
						new PacketSendItemPattern(windowId, new ItemPatternMapping(
								storage.getPatternAt(storageStack, i), database.getPosition(), patternStorage, i)),
						(EntityPlayerMP) getPlayer());
			}
		}
	}

	private void onPatternChange(IMatterDatabase database, int patternStorage, int patternId) {
		ItemStack patternStorageStack = database.getPatternStorage(patternStorage);
		if (patternStorageStack != null && patternStorageStack.getItem() instanceof IMatterPatternStorage) {
			ItemPattern itemPattern = ((IMatterPatternStorage) patternStorageStack.getItem())
					.getPatternAt(patternStorageStack, patternId);
			MatterOverdrive.NETWORK.sendTo(
					new PacketSendItemPattern(windowId,
							new ItemPatternMapping(itemPattern, database.getPosition(), patternStorage, patternId)),
					(EntityPlayerMP) getPlayer());
		}
	}

	public static class PatternMapping {
		private int storageId;
		private int patternId;
		private ItemPattern pattern;

		public PatternMapping(int storageId, int patternId, ItemPattern itemPattern) {
			this.storageId = storageId;
			this.patternId = patternId;
			this.pattern = itemPattern;
		}
	}
}
