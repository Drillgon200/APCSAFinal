package com.drillgon200.networking.udp;

public interface Stream {

	public boolean isWriting();
	
	public default int serializeInt(int i){
		return serializeInt(i, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	public int serializeInt(int i, int min, int max);
	public default float serializeFloat(float f){
		return serializeFloat(f, -Float.MAX_VALUE, Float.MAX_VALUE);
	}
	public default short serializeShort(short s){
		return serializeShort(s, Short.MIN_VALUE, Short.MAX_VALUE);
	}
	public short serializeShort(short s, short min, short max);
	public default byte serializeByte(byte s){
		return serializeByte(s, Byte.MIN_VALUE, Byte.MAX_VALUE);
	}
	public byte serializeByte(byte s, byte min, byte max);
	
	public float serializeFloat(float f, float min, float max);
	
	public default int serializeBits(int data, int bits){
		return serializeBits(data, bits, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	public int serializeBits(int data, int bits, int min, int max);
	
	public boolean serializeBoolean(boolean b);
	
}
