package com.drillgon200.shooter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import com.drillgon200.networking.udp.IMessageUDP;
import com.drillgon200.networking.udp.MessageContext;
import com.drillgon200.networking.udp.UDPNetworkThreadServer;
import com.drillgon200.shooter.entity.Entity;
import com.drillgon200.shooter.entity.EntityRegistry;
import com.drillgon200.shooter.entity.PlayerServer;
import com.drillgon200.shooter.level.LevelCommon;
import com.drillgon200.shooter.level.LevelLoader;
import com.drillgon200.shooter.packets.SPacketJoinClient;
import com.drillgon200.shooter.packets.SPacketStateUpdate;
import com.drillgon200.shooter.util.Vec3f;

public class ShooterServer {
	
	public volatile static ServerState state = ServerState.DOWN;
	public volatile static Thread updateThread;
	public volatile static UDPNetworkThreadServer netServer;
	
	public static long serverTicks = 0;
	private static long lastTickTime;
	private static Queue<Runnable> scheduledTasks = new ArrayDeque<>();
	
	public static EntityRegistry entityRegistry;
	public static WorldServer world;
	
	public static List<PlayerServer> players = new ArrayList<>();
	
	public static void start_async(Deque<String> log, int port){
		new Thread(() -> {
			if(state != ServerState.DOWN){
				switch(state){
				case STARTING:
					log.addLast("Server is still starting!");
					break;
				case ACTIVE:
					log.addLast("Server is already running!");
					break;
				case SHUTTING_DOWN:
					log.addLast("Server hasn't finished shutdown!");
					break;
				default:
					break;
				}
				return;
			}
			if(!availablePort(InetAddress.getLoopbackAddress().getHostAddress(), port)){
				log.addLast("Another application is using this port! Use a different one.");
				return;
			}
			log.addLast("Server starting...");
			state = ServerState.STARTING;

			log.addLast("Loading levels and intializing world...");
			entityRegistry = new EntityRegistry();
			registerEntities();
			PacketDispatcher.registerPackets();
			LevelCommon level = LevelLoader.loadServer("/assets/shooter/levels/level_test.dlf");
			world = new WorldServer();
			world.setLevel(level);
			serverTicks = 0;
			
			log.addLast("Starting update loop...");
			updateThread = new Thread(() -> {
				try {
					updateLoop();
				} catch(Exception e) {
					log.add("Server crashed! Check stack trace.");
					e.printStackTrace();
				}
			});
			updateThread.setName("Server Update Thread");
			updateThread.start();
			
			log.addLast("Initializing net server on port + " + port + "...");
			netServer = new UDPNetworkThreadServer(port, log);
			netServer.start();
			
			state = ServerState.ACTIVE;
			log.addLast("Server active!");
		}).start();
	}
	
	private static boolean availablePort(String addr, int port) {
		  boolean available = true;
		  try {
			ServerSocketChannel sock = ServerSocketChannel.open();
			InetSocketAddress socketaddr = new InetSocketAddress(addr, port);
			sock.bind(socketaddr);
			sock.close();
		  } catch(IOException e) {
			  available = false;
		  }
		  return available;
	}
	
	public static void registerEntities(){
		entityRegistry.registerEntitiesCommon();
		entityRegistry.register("player_server", PlayerServer.class);
	}
	
	private static void updateLoop() throws Exception {
		lastTickTime = System.currentTimeMillis();
		while(state != ServerState.DOWN){
			long time = System.currentTimeMillis();
			long deltaTime = time-lastTickTime;
			float ticksPassed = ((float)(deltaTime)/MainConfig.TICKLENGTH);
			int ticks = (int)ticksPassed;
			for(int i = 0; i < Math.min(10, ticks); i ++){
				lastTickTime = time;
				try {
					serverTick();
				} catch(Exception x){
					x.printStackTrace();
					throw new Exception("Exception in main tick loop", x);
				}
			}
			//I still don't know if this is the right way to make it not eat 20% of my CPU
			Thread.sleep(Math.max(1, (long)MainConfig.TICKLENGTH-deltaTime));
		}
	}
	
	public static void addScheduledTask(Runnable r){
		synchronized(scheduledTasks){
			scheduledTasks.add(r);
		}
	}
	
	private static void serverTick(){
		synchronized(scheduledTasks){
			for(Runnable r : scheduledTasks){
				r.run();
			}
			scheduledTasks.clear();
		}
		Iterator<PlayerServer> itr = players.iterator();
		while(itr.hasNext()){
			PlayerServer p = itr.next();
			if(p.connection.isClosed){
				itr.remove();
				p.markedForRemoval = true;
			}
		}
		
		world.updateEntities();
		if(serverTicks % 2 == 0){
			sendStateUpdatePacket();
		}
		serverTicks ++;
	}
	
	public static void tryJoinPlayer(MessageContext c) {
		if(world.playerEntities.size() > 3)
			return;
		Vec3f pos = world.level.possibleSpawns.get(world.rand.nextInt(world.level.possibleSpawns.size()));
		PlayerServer player = new PlayerServer(world, pos.x, pos.y, pos.z);
		player.connection = c.connection;
		c.connection.player = player;
		for(Entity ent : world.entities){
			world.sendEntityAddPacket(player.connection, ent);
		}
		world.addEntity(player);
		players.add(player);
		player.connection.sendMessage(new SPacketJoinClient(player.entityId));
	}
	
	public static void sendStateUpdatePacket(){
		int[] data = new int[world.entities.size()*4];
		for(int i = 0; i < world.entities.size()*4; i += 4){
			Entity ent = world.entities.get(i/4);
			data[i] = ent.entityId;
			Vec3f pos = ent.getInterpolatedPos(1);
			data[i+1] = Float.floatToIntBits(pos.x);
			data[i+2] = Float.floatToIntBits(pos.y);
			data[i+3] = Float.floatToIntBits(pos.z);
		}
		IMessageUDP update = new SPacketStateUpdate(data);
		for(PlayerServer p : players){
			p.connection.sendMessage(update);
		}
	}
	
	public static void shutdown_async(Deque<String> log){
		new Thread(() -> {
			if(state != ServerState.ACTIVE){
				switch(state){
				case STARTING:
					log.addLast("Can't shutdown while starting!");
					break;
				case DOWN:
					log.addLast("Server is not active!");
					break;
				case SHUTTING_DOWN:
					log.addLast("Server hasn't finished shutdown!");
					break;
				default:
					break;
				}
				return;
			}
			log.addLast("Server shutting down...");
			state = ServerState.SHUTTING_DOWN;
			
			log.addLast("Unloading world...");
			world.onShutDown();
			world.getLevel().delete();
			
			log.addLast("Shutting down net server...");
			netServer.terminate();
			
			state = ServerState.DOWN;
			log.addLast("Waiting for update thread to terminate...");
			try {
				updateThread.join();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			updateThread = null;
			log.addLast("Server shut down successfully!");
			log.addLast("");
		}).start();
	}
	
	public static enum ServerState {
		DOWN,
		STARTING,
		ACTIVE,
		SHUTTING_DOWN;
	}

}
