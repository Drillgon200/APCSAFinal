package com.drillgon200.shooter.packets;

import com.drillgon200.networking.udp.IMessageHandlerUDP;
import com.drillgon200.networking.udp.IMessageUDP;
import com.drillgon200.networking.udp.MessageContext;
import com.drillgon200.networking.udp.Stream;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.Shooter.ConnectionState;
import com.drillgon200.shooter.entity.Entity;
import com.drillgon200.shooter.util.Vec3f;

public class SPacketStateUpdate implements IMessageUDP {

	int[] data;
	
	public SPacketStateUpdate() {
	}
	
	public SPacketStateUpdate(int[] data) {
		this.data = data;
	}
	
	@Override
	public boolean reliable() {
		return false;
	}
	
	@Override
	public void serialize(Stream s) {
		if(s.isWriting()){
			s.serializeInt(data.length);
		} else {
			data = new int[s.serializeInt(0)];
		}
		for(int i = 0; i < data.length; i ++){
			data[i] = s.serializeInt(data[i]);
		}
	}

	public static class Handler implements IMessageHandlerUDP<SPacketStateUpdate> {

		@Override
		public void onMessage(SPacketStateUpdate m, MessageContext c) {
			Shooter.addScheduledTask(() -> {
				if(Shooter.state != ConnectionState.CONNECTED)
					return;
				Shooter.world.ticksSinceLastStateUpdate = 0;
				for(int i = 0; i < m.data.length; i += 4){
					int id = m.data[i];
					float x = Float.intBitsToFloat(m.data[i+1]);
					float y = Float.intBitsToFloat(m.data[i+2]);
					float z = Float.intBitsToFloat(m.data[i+3]);
					Entity e = Shooter.world.getEntityById((short)id);
					if(e == null || e == Shooter.player)
						continue;
					e.updatePositionFromServer(new Vec3f(x, y, z));
				}
			});
		}
		
	}
}
