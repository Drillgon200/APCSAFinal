package com.drillgon200.networking.udp;

public class WriteStream implements Stream {

	public BitWriter writer;
	
	public WriteStream(BitWriter w) {
		writer = w;
	}
	
	@Override
	public boolean isWriting() {
		return true;
	}

	@Override
	public int serializeInt(int i, int min, int max) {
		if(i < min || i > max)
			throw new PacketException("Value out of range");
		writer.writeInt(i);
		return i;
	}
	
	@Override
	public short serializeShort(short s, short min, short max) {
		if(s < min || s > max)
			throw new PacketException("Value out of range");
		writer.writeShort(s);
		return s;
	}

	@Override
	public byte serializeByte(byte s, byte min, byte max) {
		if(s < min || s > max)
			throw new PacketException("Value out of range");
		writer.writeByte(s);
		return s;
	}
	
	@Override
	public float serializeFloat(float f, float min, float max) {
		if(f < min ||f > max)
			throw new PacketException("Value out of range");
		writer.writeFloat(f);
		return f;
	}

	@Override
	public int serializeBits(int data, int bits, int min, int max) {
		if(data < min || data > max)
			throw new PacketException("Value out of range");
		writer.writeBits(data, bits);
		return data;
	}

	public void clear() {
		writer.clear();
	}

	public void finish() {
		writer.flushData();
	}

	@Override
	public boolean serializeBoolean(boolean b) {
		writer.writeBoolean(b);
		return b;
	}

}
