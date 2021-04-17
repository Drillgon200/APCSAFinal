package com.drillgon200.shooter.util;

public class Vec3f {

	public float x, y, z;
	
	public Vec3f(float val) {
		this(val, val, val);
	}
	
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
	
	public Vec3f rotateX(float pitch){
		double cos = Math.cos(pitch);
        double sin = Math.sin(pitch);
        double newX = this.x;
        double newY = this.y*cos + this.z*sin;
        double newZ = this.z*cos - this.y*sin;
        return new Vec3f((float)newX, (float)newY, (float)newZ);
    }

    public Vec3f rotateY(float yaw){
    	double cos = Math.cos(yaw);
    	double sin = Math.sin(yaw);
        double newX = this.x*cos + this.z*sin;
        double newY = this.y;
        double newZ = this.z*cos - this.x*sin;
        return new Vec3f((float)newX, (float)newY, (float)newZ);
    }
    
    public Vec3f rotateZ(float pitch){
		double cos = Math.cos(pitch);
        double sin = Math.sin(pitch);
        double newX = this.x*cos + this.y*sin;
        double newY = this.y*cos - this.x*sin;
        double newZ = this.z;
        return new Vec3f((float)newX, (float)newY, (float)newZ);
    }
    
    public Vec3f add(Vec3f other){
    	return new Vec3f(x+other.x, y+other.y, z+other.z);
    }
    
    public Vec3f mutateAdd(Vec3f other){
    	this.x += other.x;
    	this.y += other.y;
    	this.z += other.z;
    	return this;
    }
    
    public Vec3f add(float x, float y, float z){
    	return new Vec3f(this.x+x, this.y+y, this.z+z);
    }
    
    public Vec3f subtract(Vec3f other){
    	return new Vec3f(x-other.x, y-other.y, z-other.z);
    }
    
    public Vec3f mutateSubtract(Vec3f other){
    	this.x -= other.x;
    	this.y -= other.y;
    	this.z -= other.z;
    	return this;
    }
    
    public Vec3f subtract(float x, float y, float z){
    	return new Vec3f(this.x-x, this.y-y, this.z-z);
    }
    
    public Vec3f cross(Vec3f other){
    	return new Vec3f(this.y * other.z - this.z * other.y, this.z * other.x - this.x * other.z, this.x * other.y - this.y * other.x);
    }
    
    public float dot(Vec3f other){
    	return this.x*other.x + this.y*other.y + this.z*other.z;
    }

	public Vec3f scale(float f) {
		return new Vec3f(x*f, y*f, z*f);
	}
	
	public Vec3f mutateScale(float f){
		this.x *= f;
		this.y *= f;
		this.z *= f;
		return this;
	}
	
	public Vec3f scaled(double d) {
		return scale((float)d);
	}

	public Vec3f negate() {
		return new Vec3f(-x, -y, -z);
	}
	
	public float val(int idx){
		switch(idx){
		case 0: return x;
		case 1: return y;
		case 2: return z;
		}
		throw new ArrayIndexOutOfBoundsException("Bad index");
	}
	
	public void setVal(int idx, float f){
		switch(idx){
		case 0: x = f; return;
		case 1: y = f; return;
		case 2: z = f; return;
		}
		throw new ArrayIndexOutOfBoundsException("Bad index");
	}
	
	//Reflection of vector is vec-2(dot(vec, axis))axis (project the vector onto the axis, times two, then subtract the original vector)
	public Vec3f reflect(Vec3f axis){
		//Didn't want to create a bunch of useless objects in case I use this in performance sensitive code.
		//return axis.scale(2*this.dot(axis)).subtract(this);
		float vDotAxis2 = 2*(x*axis.x+y*axis.y+z*axis.z);
		return new Vec3f(vDotAxis2*axis.x-x, vDotAxis2*axis.y-y, vDotAxis2*axis.z-z);
	}
	
	public float lenSq(){
		return x*x+y*y+z*z;
	}
	
	public float len(){
		return (float) Math.sqrt(lenSq());
	}
	
	public Vec3f normalize(){
		float len = (float)Math.sqrt(x*x+y*y+z*z);
		if(len == 0){
			return new Vec3f(0, 0, 0);
		}
		float lengthRcp = 1F/len;
		return new Vec3f(x*lengthRcp, y*lengthRcp, z*lengthRcp);
	}
	
	public Vec3f mul(Vec3f other){
		return new Vec3f(x*other.x, y*other.y, z*other.z);
	}
	
	//https://en.wikipedia.org/wiki/Outer_product
	public Matrix3f outerProduct(Vec3f other) {
		Matrix3f mat = new Matrix3f(
				(float)(x*other.x), (float)(x*other.y), (float)(x*other.z),
				(float)(y*other.x), (float)(y*other.y), (float)(y*other.z),
				(float)(z*other.x), (float)(z*other.y), (float)(z*other.z));
		return mat;
	}
	
	public Vec3f matTransform(Matrix3f mat) {
		float x,y,z;
		x = mat.m00*this.x + mat.m01*this.y + mat.m02*this.z; 
		y = mat.m10*this.x + mat.m11*this.y + mat.m12*this.z; 
		z = mat.m20*this.x + mat.m21*this.y + mat.m22*this.z; 
		//float x = mat.m00 * this.x + mat.m10 * this.y + mat.m20 * this.z;
		//float y = mat.m01 * this.x + mat.m11 * this.y + mat.m21 * this.z;
		//float z = mat.m02 * this.x + mat.m12 * this.y + mat.m22 * this.z;

		return new Vec3f(x, y, z);
	}
	
	public float distanceTo(Vec3f other) {
		float dx = this.x-other.x;
		float dy = this.y-other.y;
		float dz = this.z-other.z;
		return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
	}
	
	public Vec3f lerp(Vec3f other, float interp) {
		return new Vec3f(x + (other.x-x)*interp, y + (other.y-y)*interp, z + (other.z-z)*interp);
	}
	
	public Vec3f max(Vec3f other){
		return new Vec3f(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
	}
	
	public Vec3f min(Vec3f other){
		return new Vec3f(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
	}
	
	public Vec3f copy() {
		return new Vec3f(x, y, z);
	}
	
	public void set(Vec3f vec) {
		this.x = vec.x;
		this.y = vec.y;
		this.z = vec.z;
	}
	
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public String toString() {
		return "Vector 3: " + x + " " + y + " " + z;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Vec3f)){
			return false;
		}
		Vec3f vec = (Vec3f)obj;
		return x == vec.x && y == vec.y && z == vec.z;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31*result + Float.floatToIntBits(x);
		result = 31*result + Float.floatToIntBits(y);
		result = 31*result + Float.floatToIntBits(z);
        return result;
	}

}
