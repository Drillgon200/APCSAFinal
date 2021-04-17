package com.drillgon200.shooter.util;

public class Vec4f {

	public float x, y, z, w;

	public Vec4f() {
	}
	
	public Vec4f(Vec3f xyz, float w) {
		this(xyz.x, xyz.y, xyz.z, w);
	}
	
	public Vec4f(float val){
		this(val, val, val, val);
	}
	
	public Vec4f(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
}
