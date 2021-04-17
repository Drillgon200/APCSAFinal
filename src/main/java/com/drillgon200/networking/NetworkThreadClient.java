package com.drillgon200.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Deque;

import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.Shooter.ConnectionState;

public class NetworkThreadClient extends Thread {

	public volatile boolean shouldShutDown = false;
	public InetAddress server;
	public int port;
	public volatile Connection serverConnection;
	long lastCommunicatedTime;
	long currentTime;
	
	public Deque<String> log;
	
	public NetworkThreadClient() {
		this.setName("Client Network Thread");
	}
	
	public void setLog(Deque<String> log){
		this.log = log;
	}
	
	public void terminate(){
		shouldShutDown = true;
	}
	
	@Override
	public void run() {
		try {
			while(!shouldShutDown){
				//I don't know if it's the best practice to do this, but it makes it not use 20% of my CPU and works I guess?
				try {
					Thread.sleep(1);
				} catch(InterruptedException e1) {
					e1.printStackTrace();
				}
				if(serverConnection != null){
					currentTime = System.currentTimeMillis();
					if(currentTime - lastCommunicatedTime > NetworkManager.TIMEOUT){
						Shooter.state = ConnectionState.DISCONNECTING;
						serverConnection.isClosed = true;
						serverConnection.channel.close();
						serverConnection = null;
						System.out.println("Server timed out.");
						if(log != null){
							log.addLast("Server timed out.");
						}
						continue;
					}
					readConnection();
					if(serverConnection != null)
						writeConnection();
				}
			}
			close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void readConnection() throws IOException {
		int amountRead = serverConnection.reader.read(serverConnection);
		if(amountRead > 0){
			lastCommunicatedTime = System.currentTimeMillis();
		}
		if(serverConnection.isClosed){
			Shooter.state = ConnectionState.DISCONNECTING;
			System.out.println("Closed connection");
			serverConnection.channel.close();
			serverConnection = null;
			if(log != null)
				log.addLast("Disconnected");
		}
	}
	
	private void writeConnection() throws IOException {
		serverConnection.writer.write(serverConnection);
	}
	
	public void connect(InetAddress server, int port) throws IOException {
		close();
		SocketChannel channel = SocketChannel.open();
		channel.connect(new InetSocketAddress(server, port));
		lastCommunicatedTime = System.currentTimeMillis();
		Connection c = new Connection(channel);
		channel.configureBlocking(false);
		this.server = server;
		this.port = port;
		serverConnection = c;
	}
	
	public void close(){
		if(serverConnection == null)
			return;
		serverConnection.isClosed = true;
		try {
			serverConnection.channel.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
