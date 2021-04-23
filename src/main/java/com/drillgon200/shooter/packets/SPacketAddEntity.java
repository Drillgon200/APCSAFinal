package com.drillgon200.shooter.packets;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.drillgon200.networking.udp.IMessageHandlerUDP;
import com.drillgon200.networking.udp.IMessageUDP;
import com.drillgon200.networking.udp.MessageContext;
import com.drillgon200.networking.udp.Stream;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.ShooterServer;
import com.drillgon200.shooter.entity.Entity;
import com.drillgon200.shooter.util.Vec3f;

public class SPacketAddEntity implements IMessageUDP {

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
	public boolean reliable() {
		return true;
	}
	
	@Override
	public void serialize(Stream s) {
		byte[] bytes;
		if(s.isWriting()){
			bytes = entName.getBytes(Charset.forName("ascii"));
			s.serializeBits(bytes.length, 8);
		} else {
			bytes = new byte[s.serializeBits(0, 8)];
		}
		for(int i = 0; i < bytes.length; i ++){
			bytes[i] = s.serializeByte(bytes[i]);
		}
		if(!s.isWriting()){
			entName = new String(bytes, Charset.forName("ascii"));
		}
		entId = s.serializeShort(entId);
		x = s.serializeFloat(x);
		y = s.serializeFloat(y);
		z = s.serializeFloat(z);
	}
	
	public static class Handler implements IMessageHandlerUDP<SPacketAddEntity> {

		@Override
		public void onMessage(SPacketAddEntity m, MessageContext c) {
			Shooter.addScheduledTask(() -> {
				Entity ent = Shooter.entityRegistry.constructEntity(m.entName, Shooter.world);
				ent.entityId = m.entId;
				ent.setPos(m.x, m.y, m.z);
				Shooter.world.addEntity(ent);
			});
		}
		
	}

}
