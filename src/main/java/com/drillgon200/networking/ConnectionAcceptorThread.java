package com.drillgon200.networking;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class ConnectionAcceptorThread extends Thread {

	public volatile boolean shouldShutDown = false;
	public int port;
	public ServerSocketChannel listener;
	public Queue<Connection> connectionQueue;
	public boolean local;
	
	public Deque<String> log;
	
	public ConnectionAcceptorThread(int port, boolean local, Queue<Connection> connectionQueue) {
		this.connectionQueue = connectionQueue;
		this.local = local;
		this.setName((local ? "Local" : "Server") + " Connection Acceptor Thread");
		this.port = port;
	}
	
	public void setLog(Deque<String> log){
		this.log = log;
	}
	
	public void terminate(){
		shouldShutDown = true;
		try {
			listener.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		init();
		while(!shouldShutDown){
			SocketChannel channel;
			try {
				channel = listener.accept();
				System.out.println("Accepting connection: " + channel);
				if(log != null)
					log.addLast("Accepting connection: " + channel);
				connectionQueue.add(new Connection(channel));
			} catch(AsynchronousCloseException e){
				//Eat it.
				//I have literally no clue how to close this thing properly without exiting the program.
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		try {
			listener.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void init(){
		try {
			listener = ServerSocketChannel.open();
			InetSocketAddress addr = new InetSocketAddress(local ? InetAddress.getLoopbackAddress().getHostAddress() : getLocalAddress(), port);
			listener.bind(addr);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private String getLocalAddress(){
		String addr = "";
		try {
			addr = InetAddress.getLocalHost().getHostAddress();
		} catch(UnknownHostException e1) {}
		try(final DatagramSocket socket = new DatagramSocket()){
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			addr = socket.getLocalAddress().getHostAddress();
			socket.close();
		} catch(SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
		return addr;
	}
}
