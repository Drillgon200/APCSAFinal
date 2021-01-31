package com.drillgon200.shooter.util;

public class Vec3f {

	public float x, y, z;
	
	public Vec3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vec3f(float yaw, float pitch){
		double xzLen = Math.cos(pitch);
		x = (float) (xzLen*Math.cos(yaw));
		y = (float) Math.sin(pitch);
		z = (float) (xzLen*Math.sin(-yaw));
	}
	
	public Vec3f rotatePitch(float pitch)
    {
		double f = Math.cos(pitch);
        double f1 = Math.sin(pitch);
        double d0 = this.x;
        double d1 = this.y * (double)f + this.z * (double)f1;
        double d2 = this.z * (double)f - this.y * (double)f1;
        return new Vec3f((float)d0, (float)d1, (float)d2);
    }

    public Vec3f rotateYaw(float yaw)
    {
    	double f = Math.cos(yaw);
    	double f1 = Math.sin(yaw);
        double d0 = this.x * (double)f + this.z * (double)f1;
        double d1 = this.y;
        double d2 = this.z * (double)f - this.x * (double)f1;
        return new Vec3f((float)d0, (float)d1, (float)d2);
    }

	public Vec3f scale(float f) {
		return new Vec3f(x*f, y*f, z*f);
	}

	public Vec3f negate() {
		return new Vec3f(-x, -y, -z);
	}
}
