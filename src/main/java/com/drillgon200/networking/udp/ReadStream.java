package com.drillgon200.networking.udp;

public class ReadStream implements Stream {

	private BitReader reader;
	
	public ReadStream(BitReader read) {
		this.reader = read;
	}
	
	@Override
	public boolean isWriting() {
		return false;
	}

	@Override
	public int serializeInt(int i, int min, int max) {
		i = reader.readInt();
		if(i < min || i > max)
			throw new PacketException("Read value out of range");
		return i;
	}
	
	@Override
	public short serializeShort(short s, short min, short max) {
		s = reader.readShort();
		if(s < min || s > max)
			throw new PacketException("Read value out of range");
		return s;
	}

	@Override
	public byte serializeByte(byte s, byte min, byte max) {
		s = reader.readByte();
		if(s < min || s > max)
			throw new PacketException("Read value out of range");
		return s;
	}
	
	@Override
	public float serializeFloat(float f, float min, float max) {
		f = reader.readFloat();
		if(f < min || f > max)
			throw new PacketException("Read value out of range");
		return f;
	}

	@Override
	public int serializeBits(int data, int bits, int min, int max) {
		data = reader.readBits(bits);
		if(data < min || data > max)
			throw new PacketException("Read value out of range");
		return data;
	}
	
	public boolean canReadBits(int bits){
		return reader.canReadBits(bits);
	}
	
	public void clear(){
		reader.clear();
	}

	@Override
	public boolean serializeBoolean(boolean b) {
		return reader.readBoolean();
	}

}
