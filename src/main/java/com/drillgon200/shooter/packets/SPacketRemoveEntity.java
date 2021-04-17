package com.drillgon200.shooter.packets;

import java.nio.ByteBuffer;

import com.drillgon200.networking.Connection;
import com.drillgon200.networking.IMessage;
import com.drillgon200.networking.IMessageHandler;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.entity.Entity;

public class SPacketRemoveEntity implements IMessage {

	short id;
	
	public SPacketRemoveEntity() {
	}
	
	public SPacketRemoveEntity(Entity e) {
		id = e.entityId;
	}
	
	@Override
	public void write(ByteBuffer buffer) {
		buffer.putShort(id);
	}

	@Override
	public void read(ByteBuffer buffer) {
		id = buffer.getShort();
	}
	
	public static class Handler implements IMessageHandler<SPacketRemoveEntity> {

		@Override
		public void onMessage(SPacketRemoveEntity m, Connection c) {
			Shooter.addScheduledTask(() -> {
				Entity e = Shooter.world.getEntityById(m.id);
				if(e != null){
					e.markedForRemoval = true;
				}
			});
		}
		
	}

}
