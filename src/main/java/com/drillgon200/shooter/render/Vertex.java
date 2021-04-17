package com.drillgon200.shooter.render;

import java.nio.ByteBuffer;

import com.drillgon200.shooter.util.Vec3f;

public class Vertex {

	public Vec3f pos;
	public float u;
	public float v;
	public float lmapU;
	public float lmapV;
	public Vec3f normal;
	public Vec3f tangent;
	public byte[] boneIndices = new byte[4];
	public byte[] boneWeights = new byte[4];
	
	public void setSkinningData(ByteBuffer buf){
		for(int i = 0; i < 4; i ++){
			int idx = buf.getInt();
			float weight = buf.getFloat();
			boneIndices[i] = (byte)idx;
			boneWeights[i] = (byte)(int)(weight*255);
		}
	}
}
