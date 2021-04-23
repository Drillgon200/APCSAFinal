package com.drillgon200.shooter.packets;

import com.drillgon200.networking.udp.IMessageHandlerUDP;
import com.drillgon200.networking.udp.IMessageUDP;
import com.drillgon200.networking.udp.MessageContext;
import com.drillgon200.networking.udp.Stream;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.entity.Entity;

public class SPacketRemoveEntity implements IMessageUDP {

	short id;
	
	public SPacketRemoveEntity() {
	}
	
	public SPacketRemoveEntity(Entity e) {
		id = e.entityId;
	}
	
	@Override
	public boolean reliable() {
		return true;
	}
	
	@Override
	public void serialize(Stream s) {
		id = s.serializeShort(id);
	}
	
	public static class Handler implements IMessageHandlerUDP<SPacketRemoveEntity> {

		@Override
		public void onMessage(SPacketRemoveEntity m, MessageContext c) {
			Shooter.addScheduledTask(() -> {
				Entity e = Shooter.world.getEntityById(m.id);
				if(e != null){
					e.markedForRemoval = true;
				}
			});
		}
		
	}

}
