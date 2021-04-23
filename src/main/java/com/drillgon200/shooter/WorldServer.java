package com.drillgon200.shooter;

import java.util.ArrayDeque;
import java.util.Deque;

import com.drillgon200.networking.udp.UDPConnection;
import com.drillgon200.shooter.entity.Entity;
import com.drillgon200.shooter.entity.Player;
import com.drillgon200.shooter.entity.PlayerServer;
import com.drillgon200.shooter.level.LevelCommon;
import com.drillgon200.shooter.packets.SPacketAddEntity;
import com.drillgon200.shooter.packets.SPacketRemoveEntity;

public class WorldServer extends World {

	public LevelCommon level;
	
	public short currentEntityId = 0;
	public Deque<Short> availableEntityIds = new ArrayDeque<>();
	
	@Override
	public boolean addEntity(Entity ent) {
		short id;
		if(availableEntityIds.size() > 0){
			id = availableEntityIds.pop();
		} else {
			id = currentEntityId++;
		}
		ent.entityId = id;
		if(ent instanceof PlayerServer){
			PacketDispatcher.sendToAllExcept(new SPacketAddEntity(ent, "player_client_multiplayer"), (PlayerServer)ent);
			((Player)ent).connection.sendMessage(new SPacketAddEntity(ent, "player_client"));
		} else {
			PacketDispatcher.sendToAll(new SPacketAddEntity(ent));
		}
		return super.addEntity(ent);
	}
	
	public void sendEntityAddPacket(UDPConnection c, Entity ent){
		if(ent instanceof PlayerServer){
			c.sendMessage(new SPacketAddEntity(ent, "player_client_multiplayer"));
		} else {
			c.sendMessage(new SPacketAddEntity(ent));
		}
	}
	
	@Override
	protected void removeEntity(Entity ent) {
		availableEntityIds.push(ent.entityId);
		super.removeEntity(ent);
		PacketDispatcher.sendToAll(new SPacketRemoveEntity(ent));
	}
	
	@Override
	public void updateEntities(){
		super.updateEntities();
	}

	public void setLevel(LevelCommon newLevel){
		if(level != null){
			level.delete();
		}
		level = newLevel;
		for(Player p : playerEntities){
			p.setPos(level.possibleSpawns.get(rand.nextInt(level.possibleSpawns.size())));
		}
	}
	
	@Override
	public LevelCommon getLevel() {
		return level;
	}
	
	@Override
	public boolean isRemote() {
		return false;
	}

	public void onShutDown() {
		//Kick players with message and stuff
	}
}
