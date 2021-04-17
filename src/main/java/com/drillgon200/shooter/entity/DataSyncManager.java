package com.drillgon200.shooter.entity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DataSyncManager {

	private List<DataEntry> entries = new ArrayList<>();
	
	public <T> void register(String name, DataEntry<T> d, T defaultValue){
		entries.add(new DataEntry(name, defaultValue));
	}
	
	public void serialize(ByteBuffer buf){
		
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getValue(String name){
		for(DataEntry<?> e : entries){
			if(e.name.equals(name)){
				return (T) e.value;
			}
		}
		throw new RuntimeException("Unregistered key " + name);
	}
	
	public static class DataEntry<T> {
		private byte id;
		private String name;
		private T value;
		
		public DataEntry(String name, T val) {
			
		}
	}
}
