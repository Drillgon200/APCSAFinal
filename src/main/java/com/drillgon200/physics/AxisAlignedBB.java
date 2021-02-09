package com.drillgon200.physics;

import com.drillgon200.shooter.util.Vec3f;

public class AxisAlignedBB {

	public float minX;
	public float maxX;
	public float minY;
	public float maxY;
	public float minZ;
	public float maxZ;
	
	public AxisAlignedBB(Vec3f min, Vec3f max) {
		this(min.x, min.y, min.z, max.x, max.y, max.z);
	}
	
	public AxisAlignedBB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ){
		this(minX, minY, minZ, maxX, maxY, maxZ, true);
	}
	
	public AxisAlignedBB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, boolean checkValues) {
		if(checkValues){
			this.minX = Math.min(minX, maxX);
			this.maxX = Math.max(minX, maxX);
			this.minY = Math.min(minY, maxY);
			this.maxY = Math.max(minY, maxY);
			this.minZ = Math.min(minZ, maxZ);
			this.maxZ = Math.max(minZ, maxZ);
		} else {
			//Skip some checks and assume the values are already in order. Only use if you absolutely know they are.
			this.minX = minX;
			this.minY = minY;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxY = maxY;
			this.maxZ = maxZ;
		}
	}
	
	public boolean intersects(AxisAlignedBB other){
		return other.maxX > this.minX && other.minX < this.maxX &&
				other.maxY > this.minY && other.minY < this.maxY &&
				other.maxZ > this.minZ && other.minZ < this.maxZ;
	}
	
	public AxisAlignedBB union(AxisAlignedBB other){
		return new AxisAlignedBB(Math.min(other.minX, this.minX), Math.min(other.minY, this.minY), Math.min(other.minZ, this.minZ), 
				Math.max(other.maxX, this.maxX), Math.max(other.maxY, this.maxY), Math.max(other.maxZ, this.maxZ), false);
	}
	public AxisAlignedBB union(AxisAlignedBB other, float margin){
		return new AxisAlignedBB(Math.min(other.minX, this.minX)-margin, Math.min(other.minY, this.minY)-margin, Math.min(other.minZ, this.minZ)-margin, 
				Math.max(other.maxX, this.maxX)+margin, Math.max(other.maxY, this.maxY)+margin, Math.max(other.maxZ, this.maxZ)+margin, margin < 0);
	}
	public AxisAlignedBB expand(float x, float y, float z){
		float minX = this.minX;
		float minY = this.minY;
		float minZ = this.minZ;
		float maxX = this.maxX;
		float maxY = this.maxY;
		float maxZ = this.maxZ;
		
		if(x < 0)
			minX += x;
		else
			maxX += x;
		
		if(y < 0)
			minY += y;
		else
			maxY += y;
		
		if(z < 0)
			minZ += z;
		else
			maxZ += z;
		
		return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ, false);
	}
	public AxisAlignedBB grow(float size){
		return grow(size, size, size);
	}
	public AxisAlignedBB grow(float x, float y, float z){
		return new AxisAlignedBB(minX-x, minY-y, minZ-z, maxX+x, maxY+y, maxZ+z, x<0 && y<0&& z<0);
	}
	public float area(){
		float width = maxX-minX;
		float depth = maxZ-minZ;
		float height = maxY-minY;
		return 2F*(width*depth + width*height + depth*height);
	}
	public float volume(){
		float width = maxX-minX;
		float depth = maxZ-minZ;
		float height = maxY-minY;
		return width*depth*height;
	}
	
	public boolean contains(AxisAlignedBB other){
		return maxX >= other.maxX && maxY >= other.maxY && maxZ >= other.maxZ && minX <= other.minX && minY <= other.minY && minZ <= other.minZ;
	}
	
	/**
	 * 
	 * @param start - The starting ray cast position
	 * @param dir - The direction
	 * @param maxDistSq - The maximum distance to ray cast
	 * @return float array containing min and max times of impact
	 */
	public float[] rayIntercepts(Vec3f start, Vec3f dir){
		float invX = 1F/dir.x;
		float tx1 = (minX-start.x)*invX;
		float tx2 = (maxX-start.x)*invX;
		
		float tMin = Math.min(tx1, tx2);
		float tMax = Math.max(tx1, tx2);
		
		float invY = 1F/dir.y;
		float ty1 = (minY-start.y)*invY;
		float ty2 = (maxY-start.y)*invY;
		
		tMin = Math.max(tMin, Math.min(ty1, ty2));
		tMax = Math.min(tMax, Math.max(ty1, ty2));
		
		float invZ = 1F/dir.z;
		float tz1 = (minZ-start.z)*invZ;
		float tz2 = (maxZ-start.z)*invZ;
		
		tMin = Math.max(tMin, Math.min(tz1, tz2));
		tMax = Math.min(tMax, Math.max(tz1, tz2));
		
		if(tMax >= 0 && tMin <= tMax && tMin <= 1){
			return new float[]{tMin, tMax};
		}
		return null;
	}
	
	public RayTraceResult rayCastAABB(Vec3f start, Vec3f dir){
		Vec3f normal = null;
		
		float invX = 1F/dir.x;
		float tx1 = (minX-start.x)*invX;
		float tx2 = (maxX-start.x)*invX;
		
		float tMin;
		if(tx1 < tx2){
			tMin = tx1;
			normal = new Vec3f(-1, 0, 0);
		} else {
			tMin = tx2;
			normal = new Vec3f(1, 0, 0);
		}
		float tMax = Math.max(tx1, tx2);
		
		float invY = 1F/dir.y;
		float ty1 = (minY-start.y)*invY;
		float ty2 = (maxY-start.y)*invY;
		
		if(ty1 < ty2){
			if(ty1 > tMin){
				tMin = ty1;
				normal = new Vec3f(0, -1, 0);
			}
		} else {
			if(ty2 > tMin){
				tMin = ty2;
				normal = new Vec3f(0, 1, 0);
			}
		}
		
		tMax = Math.min(tMax, Math.max(ty1, ty2));
		
		float invZ = 1F/dir.z;
		float tz1 = (minZ-start.z)*invZ;
		float tz2 = (maxZ-start.z)*invZ;
		
		if(tz1 < tz2){
			if(tz1 > tMin){
				tMin = tz1;
				normal = new Vec3f(0, 0, -1);
			}
		} else {
			if(tz2 > tMin){
				tMin = tz2;
				normal = new Vec3f(0, 0, 1);
			}
		}
		
		tMax = Math.min(tMax, Math.max(tz1, tz2));
		
		if(tMax >= 0 && tMin <= tMax && tMin <= 1){
			return new RayTraceResult(true, tMin, start.add(dir.scale(tMin)), normal);
		}
		return new RayTraceResult();
	}
	
	public Vec3f getCenter(){
		return new Vec3f(minX + (maxX-minX)*0.5F, minY + (maxY-minY)*0.5F, minZ + (maxZ-minZ)*0.5F);
	}

	public AxisAlignedBB offset(float posX, float posY, float posZ) {
		return new AxisAlignedBB(minX + posX, minY + posY, minZ + posZ, maxX + posX, maxY + posY, maxZ + posZ);
	}
	public AxisAlignedBB offset(Vec3f pos) {
		return offset(pos.x, pos.y, pos.z);
	}
}
