package com.drillgon200.networking.udp;

import java.nio.ByteBuffer;

public class BitWriter {

	private ByteBuffer buf;
	public int bitCount = 0;
	public long scratch = 0;
	
	public BitWriter(ByteBuffer buf) {
		this.buf = buf;
	}
	
	public void writeBits(int data, int bits){
		scratch = scratch | (((long)data) << bitCount);
		bitCount += bits;
		tryWriteWord();
	}
	
	public void writeInt(int i){
		scratch = scratch | (((long)i&0xFFFFFFFFL) << bitCount);
		bitCount += 32;
		tryWriteWord();
	}
	
	public void writeShort(short s){
		scratch = scratch | (((long)s&0xFFFF) << bitCount);
		bitCount += 16;
		tryWriteWord();
	}
	
	public void writeByte(byte b){
		scratch = scratch | (((long)b&0xFF) << bitCount);
		bitCount += 8;
		tryWriteWord();
	}
	
	public void writeFloat(float f){
		writeInt(Float.floatToRawIntBits(f));
	}
	
	public void writeBoolean(boolean b){
		byte data = (byte) (b ? 1 : 0);
		scratch = scratch | (((long)data&0xFF) << bitCount);
		bitCount += 1;
		tryWriteWord();
	}
	
	public void padToWord(){
		writeBits(0, 32-bitCount);
	}
	
	private void tryWriteWord(){
		if(bitCount < 32)
			return;
		buf.putInt((int)(scratch&0xFFFFFFFF));
		bitCount -= 32;
		scratch = scratch >>> 32;
	}
	
	public void flushData(){
		int bytes = (bitCount+7)/8;
		for(int i = 0; i < bytes; i ++){
			buf.put((byte)((scratch >>> i*8)&0xFF));
		}
		clear();
	}
	
	public void clear(){
		bitCount = 0;
		scratch = 0;
	}
	
}
