package com.drillgon200.shooter;

import java.net.InetAddress;

import com.drillgon200.networking.udp.PacketClientServerTest;
import com.drillgon200.networking.udp.PacketServerClientTest;
import com.drillgon200.networking.udp.PacketTest;
import com.drillgon200.networking.udp.UDPNetworkManager;
import com.drillgon200.networking.udp.UDPNetworkThreadClient;
import com.drillgon200.networking.udp.UDPNetworkThreadServer;

public class Main {
	
	public static void main(String[] args) throws Exception {
		/*ByteBuffer buf = ByteBuffer.allocate(16);
		WriteStream ws = new WriteStream(new BitWriter(buf));
		ReadStream rs = new ReadStream(new BitReader(buf));
		ws.serializeShort((short) 1);
		ws.serializeBits(1, 8);
		ws.serializeByte((byte) 5);
		ws.finish();
		buf.flip();*/
		//System.out.println(rs.seria)
		
		
		/*UDPNetworkManager.registerPacket(PacketTest.class, PacketTest.Handler.class, Side.SERVER);
		UDPNetworkManager.registerPacket(PacketClientServerTest.class, PacketClientServerTest.Handler.class, Side.SERVER);
		UDPNetworkManager.registerPacket(PacketServerClientTest.class, PacketServerClientTest.Handler.class, Side.CLIENT);
		UDPNetworkThreadServer serv = new UDPNetworkThreadServer(12345);
		serv.start();
		UDPNetworkThreadClient cli = new UDPNetworkThreadClient();
		cli.start();
		cli.connect(InetAddress.getLoopbackAddress(), 12345);
		cli.sendMessage(new PacketTest("bruh0", 150));
		cli.sendMessage(new PacketTest("bruh1", 1500));
		cli.sendMessage(new PacketTest("bruh2", 150));
		cli.sendMessage(new PacketTest("bruh3", 150));
		cli.sendMessage(new PacketTest("bruh4", 150));
		cli.sendMessage(new PacketTest("bruh5", 150));
		long time = System.currentTimeMillis();
		while(System.currentTimeMillis()-time < 5000){
		}
		serv.terminate();
		cli.terminate();
		serv.join();
		cli.join();
		if(true)
			return;*/
		
		
		//NetworkManager.registerPacket(CPacketJoinServer.class, CPacketJoinServer.Handler.class, 0, Side.SERVER);
		if(args.length > 0 && args.length > 0 && args[0].equals("server")){
			System.out.println("Starting server...");
			int port = getPortFromString(args.length > 1 ? args[1] : "", 46655);
			if(port == -1)
				return;
			ShooterServer.start_async(null, port);
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					ShooterServer.shutdown_async(null);
					System.out.println("Stopping server...");
				}
			});
			while(true){
			}
		}
		System.out.println("Starting game...");
		//TODO move this somewhere else, it's completely irrelevant to this project
		/*long[][] numbers = new long[3][3];
		Random rand = new Random();
		for(int i = 0; i < numbers.length; i ++){
			numbers[i] = new long[]{rand.nextInt(1000000001), rand.nextInt(1000)+1, rand.nextInt(1000000001)};
		}
		numbers[0] = new long[]{10, 4, 3};
		numbers[1] = new long[]{20, 4, 2};
		//numbers[2] = new int[]{14, 5, 2};
		long weightSum = 0;
		long denom = 0;
		for(int i = 0; i < numbers.length; i ++){
			weightSum += (numbers[i][0]+numbers[i][2])*numbers[i][1];
			denom += numbers[i][1];
		}
		System.out.println("mean " + (weightSum/denom));
		long l = System.nanoTime();
		long last = 0;
		for(int i = 0; i < 20; i +=1){
			long duck = 0;
			for(int j = 0; j < numbers.length; j ++){
				duck += Math.max(Math.abs(numbers[j][0]-i)-numbers[j][2], 0)*numbers[j][1];
			}
			System.out.println(i + " " + duck + " " + (last-duck));
			last = duck;
		}
		long fin = 0;
		for(int j = 0; j < numbers.length; j ++){
			fin += Math.max(Math.abs(numbers[j][0]-(weightSum/denom))-numbers[j][2], 0)*numbers[j][1];
		}
		System.out.println("final " + (weightSum/denom) + " " + fin);
		System.out.println(System.nanoTime()-l);*/
		
		/*System.out.println("creating client");
		NetworkThreadClient c = new NetworkThreadClient();
		System.out.println("running client");
		c.start();
		System.out.println("connecting client");
		c.connect(InetAddress.getByName("127.0.0.1"), 46655);
		
		System.out.println("registering packet");
		
		System.out.println("sending packet");
		c.serverConnection.sendPacket(new CPacketJoinServer("bruh"));
		c.serverConnection.sendPacket(new CPacketJoinServer("bruh"));
		c.serverConnection.sendPacket(new CPacketJoinServer("bruh"));
		c.serverConnection.sendPacket(new CPacketJoinServer("bruh"));
		c.serverConnection.sendPacket(new CPacketJoinServer("bruh"));
		
		while(c.serverConnection.writer.currentMessage != null || !c.serverConnection.writer.writeQueue.isEmpty()){
		}
		
		System.out.println("shutting down");
		try {
			c.terminate();
			c.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}*/
		//s.shutdown();
		
		Shooter.init();
		Shooter.updateLoop();
		Shooter.shutdown();
		System.out.println("Exited game.");
	}
	
	private static int getPortFromString(String text, int def){
		int port;
		if(text.isEmpty()){
			port = def;
		} else {
			try {
				port = Integer.parseInt(text);
			} catch(NumberFormatException e){
				System.out.println("Port not a number!");
				return -1;
			}
			//According to google, 1024 and below are reserved and 65535 is the default max on windows
			if(port < 1024 || port > 65535){
				System.out.println("Port out of range! Must be between 1024 and 65535.");
				return -1;
			}
		}
		return port;
	}
	
	
}
