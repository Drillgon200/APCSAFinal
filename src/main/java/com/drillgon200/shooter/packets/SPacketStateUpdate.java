package com.drillgon200.shooter.packets;

import java.nio.ByteBuffer;

import com.drillgon200.networking.Connection;
import com.drillgon200.networking.IMessage;
import com.drillgon200.networking.IMessageHandler;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.Shooter.ConnectionState;
import com.drillgon200.shooter.entity.Entity;
import com.drillgon200.shooter.util.Vec3f;

public class SPacketStateUpdate implements IMessage {

	int[] data;
	
	public SPacketStateUpdate() {
	}
	
	public SPacketStateUpdate(int[] data) {
		this.data = data;
	}
	
	@Override
	public void write(ByteBuffer buffer) {
		buffer.putInt(data.length);
		for(int i = 0; i < data.length; i ++){
			buffer.putInt(data[i]);
		}
	}

	@Override
	public void read(ByteBuffer buffer) {
		data = new int[buffer.getInt()];
		for(int i = 0; i < data.length; i ++){
			data[i] = buffer.getInt();
		}
	}

	public static class Handler implements IMessageHandler<SPacketStateUpdate> {

		@Override
		public void onMessage(SPacketStateUpdate m, Connection c) {
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
					if(e == Shooter.player)
						continue;
					e.updatePositionFromServer(new Vec3f(x, y, z));
				}
			});
		}
		
	}
}
