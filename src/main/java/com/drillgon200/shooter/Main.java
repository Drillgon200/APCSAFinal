package com.drillgon200.shooter;

import com.drillgon200.networking.NetServer;

public class Main {
	
	public static void main(String[] args) throws Exception {
		//NetworkManager.registerPacket(CPacketJoinServer.class, CPacketJoinServer.Handler.class, 0, Side.SERVER);
		if(args.length > 0 && args.length > 0 && args[0].equals("server")){
			System.out.println("Starting server...");
			NetServer s = new NetServer(46655);
			s.start(null);
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					s.shutdown();
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
	
}
