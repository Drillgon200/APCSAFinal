package com.drillgon200.shooter.util;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import com.drillgon200.shooter.util.VertexFormat.Element;

public class Tessellator {

	public static final Tessellator instance = new Tessellator();
	
	//Give it a megabyte to start with, should be enough for anything we do, right?
	private static ByteBuffer buf = GLAllocation.createDirectByteBuffer(1024*1024);
	private static int vbo;
	private int drawMode;
	private VertexFormat currentFormat;
	private int currentElement = 0;
	private int vertexCount = 0;
	public boolean isDrawing = false;
	public float offsetX, offsetY, offsetZ;
	
	public void begin(int mode, VertexFormat format){
		if(isDrawing){
			throw new RuntimeException("Tessellator already drawing!");
		}
		drawMode = mode;
		currentFormat = format;
		isDrawing = true;
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
		currentFormat.setState();
	}
	
	public void setOffset(Vec3f offset) {
		setOffset(offset.x, offset.y, offset.z);
	}
	
	public void setOffset(float x, float y, float z){
		this.offsetX = x;
		this.offsetY = y;
		this.offsetZ = z;
	}
	
	public Tessellator pos(float x, float y, float z){
		return generic(x+offsetX, y+offsetY, z+offsetZ);
	}
	
	public Tessellator tex(float x, float y){
		return generic(x, y);
	}
	
	public Tessellator normal(float x, float y, float z){
		return generic(x, y, z);
	}
	
	public Tessellator generic(float x){
		Element e = currentFormat.elements[currentElement];
		switch(e.type){
		case GL11.GL_FLOAT:
			buf.putFloat(x);
			break;
		case GL11.GL_INT:
		case GL11.GL_UNSIGNED_INT:
			buf.putInt((int)x);
			break;
		case GL11.GL_SHORT:
		case GL11.GL_UNSIGNED_SHORT:
			buf.putShort((short)x);
			break;
		case GL11.GL_BYTE:
		case GL11.GL_UNSIGNED_BYTE:
			buf.put((byte)((int)x));
			break;
		}
		currentElement ++;
		return this;
	}
	
	public Tessellator generic(float x, float y){
		Element e = currentFormat.elements[currentElement];
		switch(e.type){
		case GL11.GL_FLOAT:
			buf.putFloat(x);
			buf.putFloat(y);
			break;
		case GL11.GL_INT:
		case GL11.GL_UNSIGNED_INT:
			buf.putInt((int)x);
			buf.putInt((int)y);
			break;
		case GL11.GL_SHORT:
		case GL11.GL_UNSIGNED_SHORT:
			buf.putShort((short)x);
			buf.putShort((short)y);
			break;
		case GL11.GL_BYTE:
		case GL11.GL_UNSIGNED_BYTE:
			buf.put((byte)((int)x));
			buf.put((byte)((int)y));
			break;
		}
		currentElement ++;
		return this;
	}
	
	public Tessellator generic(float x, float y, float z){
		Element e = currentFormat.elements[currentElement];
		switch(e.type){
		case GL11.GL_FLOAT:
			buf.putFloat(x);
			buf.putFloat(y);
			buf.putFloat(z);
			break;
		case GL11.GL_INT:
		case GL11.GL_UNSIGNED_INT:
			buf.putInt((int)x);
			buf.putInt((int)y);
			buf.putInt((int)z);
			break;
		case GL11.GL_SHORT:
		case GL11.GL_UNSIGNED_SHORT:
			buf.putShort((short)x);
			buf.putShort((short)y);
			buf.putShort((short)z);
			break;
		case GL11.GL_BYTE:
		case GL11.GL_UNSIGNED_BYTE:
			buf.put((byte)((int)x));
			buf.put((byte)((int)y));
			buf.put((byte)((int)z));
			break;
		}
		currentElement ++;
		return this;
	}
	
	public Tessellator generic(float x, float y, float z, float w){
		Element e = currentFormat.elements[currentElement];
		switch(e.type){
		case GL11.GL_FLOAT:
			buf.putFloat(x);
			buf.putFloat(y);
			buf.putFloat(z);
			buf.putFloat(w);
			break;
		case GL11.GL_INT:
		case GL11.GL_UNSIGNED_INT:
			buf.putInt((int)x);
			buf.putInt((int)y);
			buf.putInt((int)z);
			buf.putInt((int)w);
			break;
		case GL11.GL_SHORT:
		case GL11.GL_UNSIGNED_SHORT:
			buf.putShort((short)x);
			buf.putShort((short)y);
			buf.putShort((short)z);
			buf.putShort((short)w);
			break;
		case GL11.GL_BYTE:
		case GL11.GL_UNSIGNED_BYTE:
			buf.put((byte)((int)x));
			buf.put((byte)((int)y));
			buf.put((byte)((int)z));
			buf.put((byte)((int)w));
			break;
		}
		currentElement ++;
		return this;
	}
	
	public void putData(byte[] bytes){
		checkLimit(bytes.length);
		buf.put(bytes);
	}
	
	public void putData(ByteBuffer bytes){
		checkLimit(bytes.remaining());
		buf.put(bytes);
	}
	
	public void addVertexCount(int v){
		vertexCount += v;
	}
	
	public void endVertex(){
		currentElement = 0;
		vertexCount ++;
		checkLimit(currentFormat.bytes_per_vertex);
	}
	
	public void draw(){
		buf.rewind();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
		GL15.glDrawArrays(drawMode, 0, vertexCount);
		currentFormat.unsetState();
		buf.rewind();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		drawMode = 0;
		currentFormat = null;
		currentElement = 0;
		isDrawing = false;
		vertexCount = 0;
	}
	
	public static void init(){
		vbo = GL15.glGenBuffers();
	}
	
	private static void checkLimit(int bytes){
		if(buf.position() + bytes >= buf.capacity()){
			ByteBuffer newBuf = GLAllocation.createDirectByteBuffer((int) (buf.capacity()*1.5));
			newBuf.put(buf);
			buf = newBuf;
		}
	}
	
}
