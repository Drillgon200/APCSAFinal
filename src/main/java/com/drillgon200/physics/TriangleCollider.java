package com.drillgon200.physics;

import org.lwjgl.opengl.GL11;

import com.drillgon200.shooter.util.Matrix3f;
import com.drillgon200.shooter.util.Tessellator;
import com.drillgon200.shooter.util.Triangle;
import com.drillgon200.shooter.util.Vec3f;
import com.drillgon200.shooter.util.VertexFormat;

public class TriangleCollider extends Collider {

	public Triangle tri;
	public AxisAlignedBB box;
	
	private TriangleCollider() {
	}
	
	/**
	 * Constructs a collider for a single triangle. DO NOT use this in a RigidBody, it has no mass and can't be calculated correctly.
	 * @param t - the triangle
	 */
	public TriangleCollider(Triangle t) {
		this.tri = t;
		this.mass = 0;
		//Shouldn't matter, since this will never be used in a RigidBody
		this.localInertiaTensor = new Matrix3f().setZero();
		this.localCentroid = new Vec3f((t.p1.x+t.p2.x+t.p3.x)/3F, (t.p1.y+t.p2.y+t.p3.y)/3F, (t.p1.z+t.p2.z+t.p3.z)/3F);
		
		float maxX = Math.max(t.p1.x, Math.max(t.p2.x, t.p3.x));
		float maxY = Math.max(t.p1.y, Math.max(t.p2.y, t.p3.y));
		float maxZ = Math.max(t.p1.z, Math.max(t.p2.z, t.p3.z));
		float minX = Math.min(t.p1.x, Math.min(t.p2.x, t.p3.x));
		float minY = Math.min(t.p1.y, Math.min(t.p2.y, t.p3.y));
		float minZ = Math.min(t.p1.z, Math.min(t.p2.z, t.p3.z));
		box = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public RayTraceResult rayCast(Vec3f pos1, Vec3f pos2) {
		Vec3f rayDir = pos2.subtract(pos1);
		float normalDotRayDir = tri.normal.dot(rayDir);
		if(Math.abs(normalDotRayDir) > 0.0001F){
			float t = tri.p1.subtract(pos1).dot(tri.normal)/normalDotRayDir;
			if(t <= 1 && t >= 0){
				Vec3f pos = pos1.add(rayDir.scale(t));
				if(tri.checkPointInTriangle(pos)){
					return new RayTraceResult(true, t, pos, tri.normal);
				}
			}
		}
		return new RayTraceResult();
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return box;
	}

	@Override
	public Vec3f support(Vec3f direction) {
		float dot = direction.dot(tri.p1);
		Vec3f vert = tri.p1;
		if(direction.dot(tri.p2) > dot){
			vert = tri.p2;
		}
		if(direction.dot(tri.p3) > dot){
			vert = tri.p3;
		}
		return vert;
	}

	@Override
	public Collider copy() {
		TriangleCollider c = new TriangleCollider();
		c.tri = tri;
		c.box = box;
		c.localCentroid = localCentroid;
		c.localInertiaTensor = localInertiaTensor;
		c.mass = mass;
		return c;
	}
	
	@Override
	public void debugRender() {
		Tessellator.instance.begin(GL11.GL_LINES, VertexFormat.POSITION);
		Tessellator.instance.pos(tri.p1.x, tri.p1.y, tri.p1.z).endVertex();
		Tessellator.instance.pos(tri.p2.x, tri.p2.y, tri.p2.z).endVertex();
		Tessellator.instance.pos(tri.p2.x, tri.p2.y, tri.p2.z).endVertex();
		Tessellator.instance.pos(tri.p3.x, tri.p3.y, tri.p3.z).endVertex();
		Tessellator.instance.pos(tri.p3.x, tri.p3.y, tri.p3.z).endVertex();
		Tessellator.instance.pos(tri.p1.x, tri.p1.y, tri.p1.z).endVertex();
		Tessellator.instance.draw();
	}
}
