package com.drillgon200.networking;

import java.io.IOException;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class NetServer {

	public ConnectionAcceptorThread acceptor = null;
	public ConnectionAcceptorThread localAcceptor = null;
	public NetworkThreadServer netThread = null;
	public int port;
	
	public NetServer(int port) {
		this.port = port;
	}
	
	public void start(Deque<String> log){
		try {
			Queue<Connection> connectionQueue = new ArrayBlockingQueue<>(32);
			this.acceptor = new ConnectionAcceptorThread(port, false, connectionQueue);
			this.localAcceptor = new ConnectionAcceptorThread(port, true, connectionQueue);
			this.netThread = new NetworkThreadServer(connectionQueue);
			acceptor.setLog(log);
			localAcceptor.setLog(log);
			netThread.setLog(log);
			this.acceptor.start();
			this.localAcceptor.start();
			this.netThread.start();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown(){
		try {
			acceptor.terminate();
			acceptor.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		try {
			localAcceptor.terminate();
			localAcceptor.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		try {
			netThread.terminate();
			netThread.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
}
