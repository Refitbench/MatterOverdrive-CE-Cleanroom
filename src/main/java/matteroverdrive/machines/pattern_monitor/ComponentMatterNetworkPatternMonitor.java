
package matteroverdrive.machines.pattern_monitor;

import matteroverdrive.api.matter.IMatterDatabase;
import matteroverdrive.api.matter.IMatterPatternStorage;
import matteroverdrive.container.matter_network.IMatterDatabaseWatcher;
import matteroverdrive.data.matter_network.IMatterNetworkEvent;
import matteroverdrive.data.matter_network.MatterDatabaseEvent;
import net.minecraft.item.ItemStack;
import matteroverdrive.matter_network.components.MatterNetworkComponentClient;

public class ComponentMatterNetworkPatternMonitor
		extends MatterNetworkComponentClient<TileEntityMachinePatternMonitor> {
	public ComponentMatterNetworkPatternMonitor(TileEntityMachinePatternMonitor patternMonitor) {
		super(patternMonitor);
	}

	@Override
	public void onNetworkEvent(IMatterNetworkEvent event) {
		if (event instanceof IMatterNetworkEvent.ClientAdded) {
			onClientAdded((IMatterNetworkEvent.ClientAdded) event);
		} else if (event instanceof IMatterNetworkEvent.AddedToNetwork) {
			onAddedToNetwork((IMatterNetworkEvent.AddedToNetwork) event);
		} else if (event instanceof IMatterNetworkEvent.RemovedFromNetwork) {
			onRemovedFromNetwork((IMatterNetworkEvent.RemovedFromNetwork) event);
		} else if (event instanceof IMatterNetworkEvent.ClientRemoved) {
			onClientRemoved((IMatterNetworkEvent.ClientRemoved) event);
		} else if (event instanceof MatterDatabaseEvent) {
			onPatternChange((MatterDatabaseEvent) event);
		}
	}

	private void onRemovedFromNetwork(IMatterNetworkEvent.RemovedFromNetwork event) {
		rootClient.getWatchers().stream().filter(watcher -> watcher instanceof IMatterDatabaseWatcher)
				.forEach(watcher -> ((IMatterDatabaseWatcher) watcher).onDisconnectFromNetwork(rootClient));
		refreshMonitorCount();
	}

	private void onAddedToNetwork(IMatterNetworkEvent.AddedToNetwork event) {
		rootClient.getWatchers().stream().filter(watcher -> watcher instanceof IMatterDatabaseWatcher)
				.forEach(watcher -> ((IMatterDatabaseWatcher) watcher).onConnectToNetwork(rootClient));
		refreshMonitorCount();
	}

	private void onClientAdded(IMatterNetworkEvent.ClientAdded event) {
		if (event.client instanceof IMatterDatabase) {
			MatterDatabaseEvent databaseEvent = new MatterDatabaseEvent.Added((IMatterDatabase) event.client);
			rootClient.getWatchers().stream().filter(watcher -> watcher instanceof IMatterDatabaseWatcher)
					.forEach(watcher -> ((IMatterDatabaseWatcher) watcher).onDatabaseEvent(databaseEvent));
			refreshMonitorCount();
		}
	}

	private void onClientRemoved(IMatterNetworkEvent.ClientRemoved event) {
		if (event.client instanceof IMatterDatabase) {
			MatterDatabaseEvent databaseEvent = new MatterDatabaseEvent.Removed((IMatterDatabase) event.client);
			rootClient.getWatchers().stream().filter(watcher -> watcher instanceof IMatterDatabaseWatcher)
					.forEach(watcher -> ((IMatterDatabaseWatcher) watcher).onDatabaseEvent(databaseEvent));
			refreshMonitorCount();
		}
	}

	private void onPatternChange(MatterDatabaseEvent event) {
		rootClient.getWatchers().stream().filter(watcher -> watcher instanceof IMatterDatabaseWatcher)
				.forEach(watcher -> ((IMatterDatabaseWatcher) watcher).onDatabaseEvent(event));
		refreshMonitorCount();
	}

	private void refreshMonitorCount() {
		if (rootClient.getWorld() == null || rootClient.getWorld().isRemote) {
			return;
		}

		int count = 0;
		if (rootClient.getNetwork() != null) {
			for (IMatterDatabase database : rootClient.getConnectedDatabases()) {
				for (ItemStack patternDrive : database.getPatternStorageList()) {
					if (patternDrive != null && patternDrive.getItem() instanceof IMatterPatternStorage) {
						IMatterPatternStorage storage = (IMatterPatternStorage) patternDrive.getItem();
						int capacity = storage.getCapacity(patternDrive);
						for (int i = 0; i < capacity; i++) {
							if (storage.getPatternAt(patternDrive, i) != null) {
								count++;
							}
						}
					}
				}
			}
		}

		if (rootClient.getCount() != count) {
			rootClient.setCount(count);
			rootClient.markDirty();
		}
	}
}
