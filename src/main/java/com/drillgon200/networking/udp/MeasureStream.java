package com.drillgon200.networking.udp;

public class MeasureStream implements Stream {

	private int bitCount = 0;
	
	@Override
	public boolean isWriting() {
		return true;
	}

	@Override
	public int serializeInt(int i, int min, int max) {
		bitCount += 32;
		return i;
	}

	@Override
	public short serializeShort(short s, short min, short max) {
		bitCount += 16;
		return s;
	}

	@Override
	public byte serializeByte(byte s, byte min, byte max) {
		bitCount += 8;
		return s;
	}

	@Override
	public float serializeFloat(float f, float min, float max) {
		bitCount += 32;
		return f;
	}

	@Override
	public int serializeBits(int data, int bits, int min, int max) {
		bitCount += bits;
		return data;
	}

	@Override
	public boolean serializeBoolean(boolean b) {
		return false;
	}
	
	public int getBits(){
		return bitCount;
	}
	
	public int getBytes(){
		return (bitCount+7)/8;
	}

}
