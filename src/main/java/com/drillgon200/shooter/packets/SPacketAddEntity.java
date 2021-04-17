package com.drillgon200.shooter.packets;

import java.nio.ByteBuffer;

import com.drillgon200.networking.Connection;
import com.drillgon200.networking.IMessage;
import com.drillgon200.networking.IMessageHandler;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.ShooterServer;
import com.drillgon200.shooter.entity.Entity;
import com.drillgon200.shooter.entity.EntityRegistry;
import com.drillgon200.shooter.util.Vec3f;

public class SPacketAddEntity implements IMessage {

	public String entName;
	public short entId;
	public float x;
	public float y;
	public float z;
	
	public SPacketAddEntity() {
	}
	
	public SPacketAddEntity(Entity ent) {
		this.entName = ShooterServer.entityRegistry.getIdName(ent.getClass());
		this.entId = ent.entityId;
		Vec3f pos = ent.getInterpolatedPos(1);
		this.x = pos.x;
		this.y = pos.y;
		this.z = pos.z;
	}
	
	public SPacketAddEntity(Entity ent, String name) {
		this.entName = name;
		this.entId = ent.entityId;
		Vec3f pos = ent.getInterpolatedPos(1);
		this.x = pos.x;
		this.y = pos.y;
		this.z = pos.z;
	}
	
	@Override
	public void write(ByteBuffer buffer) {
		byte[] bytes = entName.getBytes();
		buffer.putInt(bytes.length);
		for(byte b : bytes){
			buffer.put(b);
		}
		buffer.putShort(entId);
	}

	@Override
	public void read(ByteBuffer buffer) {
		int len = buffer.getInt();
		byte[] bytes = new byte[len];
		buffer.get(bytes);
		entName = new String(bytes);
		entId = buffer.getShort();
	}
	
	public static class Handler implements IMessageHandler<SPacketAddEntity> {

		@Override
		public void onMessage(SPacketAddEntity m, Connection c) {
			Shooter.addScheduledTask(() -> {
				Entity ent = Shooter.entityRegistry.constructEntity(m.entName, Shooter.world);
				ent.entityId = m.entId;
				ent.setPos(m.x, m.y, m.z);
				Shooter.world.addEntity(ent);
			});
		}
		
	}

}
