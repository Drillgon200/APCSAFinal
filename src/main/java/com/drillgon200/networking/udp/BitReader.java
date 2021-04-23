package com.drillgon200.networking.udp;

import java.nio.ByteBuffer;

public class BitReader {

	private ByteBuffer buf;
	private long scratch = 0;
	private int bitCount = 0;
	
	public BitReader(ByteBuffer buf) {
		this.buf = buf;
	}
	
	public int readBits(int count){
		tryReadNext(count);
		int data = (int) (scratch & 0xFFFFFFFF);
		data = (data << (32-count)) >>> (32-count);
		scratch = scratch >>> count;
		bitCount -= count;
		return data;
	}
	
	public int readInt(){
		tryReadNext(32);
		int data = (int) (scratch & 0xFFFFFFFF);
		scratch = scratch >>> 32;
		bitCount -= 32;
		return data;
	}
	
	public short readShort(){
		tryReadNext(16);
		short data = (short) (scratch & 0xFFFF);
		scratch = scratch >>> 16;
		bitCount -= 16;
		return data;
	}
	
	public byte readByte(){
		tryReadNext(8);
		byte data = (byte) (scratch & 0xFF);
		scratch = scratch >>> 8;
		bitCount -= 8;
		return data;
	}
	
	public float readFloat(){
		return Float.intBitsToFloat(readInt());
	}
	
	public boolean readBoolean(){
		tryReadNext(1);
		byte data = (byte)(scratch & 1);
		scratch = scratch >>> 1;
		bitCount -= 1;
		return data > 0 ? true : false;
	}
	
	public void padToWord(){
		bitCount = 0;
		scratch = 0;
	}
	
	private void tryReadNext(int bits){
		if(bitCount >= bits)
			return;
		if(buf.remaining() >= 4){
			int i = buf.getInt();
			scratch = scratch | ((i & 0xFFFFFFFFL) << bitCount);
			bitCount += 32;
		} else {
			while(buf.hasRemaining()){
				scratch = scratch | ((buf.get()&0xFF) << bitCount);
				bitCount += 8;
			}
		}
	}
	
	public boolean canReadBits(int bits){
		return bitCount + buf.remaining()*8 >= bits;
	}
	
	public void clear(){
		scratch = 0;
		bitCount = 0;
	}
}
