package com.drillgon200.shooter.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

public class ResourcePool<T> {

	private final BlockingQueue<T> pool;
	private final Supplier<T> instanceCreator;
	
	public ResourcePool(int capacity, Supplier<T> instanceCreator) {
		pool = new ArrayBlockingQueue<>(capacity, true);
		this.instanceCreator = instanceCreator;
		createPool();
	}
	
	public T get(){
		try {
			return pool.take();
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void release(T object){
		pool.offer(object);
	}
	
	public void createPool(){
		while(pool.remainingCapacity() > 0){
			pool.add(instanceCreator.get());
		}
	}
}
