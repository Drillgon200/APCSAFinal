package com.drillgon200.shooter.util;

public class Triangle {

	public Vec3f p1, p2, p3;
	public Vec3f normal;
	
	public Triangle(Vec3f p1, Vec3f p2, Vec3f p3){
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		normal = p2.subtract(p1).cross(p3.subtract(p1)).normalize();
	}
	
	/**
	 * Checks if a point is bound by each triangle's edge. Does NOT check if the point is on the triangle's plain.
	 * @param point - the point to check
	 * @return whether the point is bound by the triangle or not
	 */
	public boolean checkPointInTriangle(Vec3f point){
		Vec3f toP1 = this.p1.subtract(point);
		Vec3f toP2 = this.p2.subtract(point);
		Vec3f toP3 = this.p3.subtract(point);
		
		return toP1.cross(toP2).dot(this.normal) > 0 && toP2.cross(toP3).dot(this.normal) > 0 && toP3.cross(toP1).dot(this.normal) > 0;
	}
	
}
