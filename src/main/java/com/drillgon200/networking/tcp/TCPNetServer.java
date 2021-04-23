package com.drillgon200.networking.tcp;

import java.io.IOException;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class TCPNetServer {

	public TCPConnectionAcceptorThread acceptor = null;
	public TCPConnectionAcceptorThread localAcceptor = null;
	public TCPNetworkThreadServer netThread = null;
	public int port;
	
	public TCPNetServer(int port) {
		this.port = port;
	}
	
	public void start(Deque<String> log){
		try {
			Queue<TCPConnection> connectionQueue = new ArrayBlockingQueue<>(32);
			this.acceptor = new TCPConnectionAcceptorThread(port, false, connectionQueue);
			this.localAcceptor = new TCPConnectionAcceptorThread(port, true, connectionQueue);
			this.netThread = new TCPNetworkThreadServer(connectionQueue);
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
