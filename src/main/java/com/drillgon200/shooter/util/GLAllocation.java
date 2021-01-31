package com.drillgon200.shooter.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GLAllocation {

	public static ByteBuffer createDirectByteBuffer(int size){
		return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
	}
	
	public static IntBuffer createDirectIntBuffer(int size){
		return createDirectByteBuffer(size << 2).asIntBuffer();
	}
	
	public static FloatBuffer createDirectFloatBuffer(int size){
		return createDirectByteBuffer(size << 2).asFloatBuffer();
	}
}
