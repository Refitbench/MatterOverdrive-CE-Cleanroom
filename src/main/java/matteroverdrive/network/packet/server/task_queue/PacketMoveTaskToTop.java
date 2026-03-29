package matteroverdrive.network.packet.server.task_queue;

import io.netty.buffer.ByteBuf;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.api.network.IMatterNetworkDispatcher;
import matteroverdrive.network.packet.TileEntityUpdatePacket;
import matteroverdrive.network.packet.client.task_queue.PacketSyncTaskQueue;
import matteroverdrive.network.packet.server.AbstractServerPacketHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMoveTaskToTop extends TileEntityUpdatePacket {
	int taskIndex;
	byte queueID;

	public PacketMoveTaskToTop() {
		super();
	}

	public PacketMoveTaskToTop(TileEntity dispatcher, int taskIndex, byte queueID) {
		super(dispatcher);
		this.taskIndex = taskIndex;
		this.queueID = queueID;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		super.fromBytes(buf);
		taskIndex = buf.readInt();
		queueID = buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		super.toBytes(buf);
		buf.writeInt(taskIndex);
		buf.writeByte(queueID);
	}

	public static class ServerHandler extends AbstractServerPacketHandler<PacketMoveTaskToTop> {

		@Override
		public void handleServerMessage(EntityPlayerMP player, PacketMoveTaskToTop message, MessageContext ctx) {
			TileEntity entity = message.getTileEntity(player.world);

			if (entity instanceof IMatterNetworkDispatcher) {
				IMatterNetworkDispatcher dispatcher = (IMatterNetworkDispatcher) entity;
				dispatcher.getTaskQueue(message.queueID).moveToFront(message.taskIndex);
				MatterOverdrive.NETWORK.sendTo(new PacketSyncTaskQueue(dispatcher, message.queueID), player);
			}
		}
	}
}