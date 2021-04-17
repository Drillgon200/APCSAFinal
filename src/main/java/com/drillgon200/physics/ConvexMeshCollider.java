package com.drillgon200.physics;

import org.lwjgl.opengl.GL11;

import com.drillgon200.shooter.util.Matrix3f;
import com.drillgon200.shooter.util.Tessellator;
import com.drillgon200.shooter.util.Triangle;
import com.drillgon200.shooter.util.Vec3f;
import com.drillgon200.shooter.util.VertexFormat;

public class ConvexMeshCollider extends Collider {

	public Triangle[] triangles;
	public float[] vertices;
	public int[] indices;
	public AxisAlignedBB localBox;
	
	private ConvexMeshCollider(){
	}
	
	public ConvexMeshCollider(Triangle[] triangles){
		int[] indc = new int[triangles.length*3];
		float[] verts = new float[triangles.length*9];
		for(int i = 0; i < triangles.length; i ++){
			indc[i*3+0] = i*9+0;
			indc[i*3+1] = i*9+3;
			indc[i*3+2] = i*9+6;
			verts[i*9+0] = triangles[i].p1.x;
			verts[i*9+1] = triangles[i].p1.y;
			verts[i*9+2] = triangles[i].p1.z;
			verts[i*9+3] = triangles[i].p2.x;
			verts[i*9+4] = triangles[i].p2.y;
			verts[i*9+5] = triangles[i].p2.z;
			verts[i*9+6] = triangles[i].p3.x;
			verts[i*9+7] = triangles[i].p3.y;
			verts[i*9+8] = triangles[i].p3.z;
		}
		fromData(indc, verts);
	}
	
	public ConvexMeshCollider(Triangle[] triangles, float density){
		this(triangles);
		//Calculate inertia, mass, etc
		this.mass = computeVolume()*density;
		this.localCentroid = computeCenterOfMass();
		this.localInertiaTensor = computeInertia(localCentroid, mass);
	}

	public ConvexMeshCollider(int[] indices, float[] vertices, float density) {
		fromData(indices, vertices, density);
	}
	
	public ConvexMeshCollider(int[] indices, float[] vertices) {
		fromData(indices, vertices);
	}
	
	public void fromData(int[] indices, float[] vertices, float density){
		fromData(indices, vertices);
		
		//Calculate inertia, mass, etc
		this.mass = computeVolume()*density;
		this.localCentroid = computeCenterOfMass();
		this.localInertiaTensor = computeInertia(localCentroid, mass);
	}
	
	public void fromData(int[] indices, float[] vertices){
		this.indices = indices;
		this.vertices = vertices;
		triangles = new Triangle[indices.length/3];
		for(int i = 0; i < indices.length; i += 3){
			Vec3f p1 = new Vec3f(vertices[indices[i+0]*3+0], vertices[indices[i+0]*3+1], vertices[indices[i+0]*3+2]);
			Vec3f p2 = new Vec3f(vertices[indices[i+1]*3+0], vertices[indices[i+1]*3+1], vertices[indices[i+1]*3+2]);
			Vec3f p3 = new Vec3f(vertices[indices[i+2]*3+0], vertices[indices[i+2]*3+1], vertices[indices[i+2]*3+2]);
			triangles[i/3] = new Triangle(p1, p2, p3);
		}
		float maxX = support(RigidBody.cardinals[0]).x;
		float maxY = support(RigidBody.cardinals[1]).y;
		float maxZ = support(RigidBody.cardinals[2]).z;
		float minX = support(RigidBody.cardinals[3]).x;
		float minY = support(RigidBody.cardinals[4]).y;
		float minZ = support(RigidBody.cardinals[5]).z;
		this.localBox = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	//The three methods below are from this site.
	//http://melax.github.io/volint.html
	
	private float computeVolume(){
		float vol = 0;
		for(Triangle t : triangles){
			vol += new Matrix3f(t.p1.x, t.p1.y, t.p1.z, t.p2.x, t.p2.y, t.p2.z, t.p3.x, t.p3.y, t.p3.z).determinant();
		}
		return vol/6F;
	}
	
	private Vec3f computeCenterOfMass(){
		Vec3f center = new Vec3f(0, 0, 0);
		float volume = 0;
		for(Triangle t : triangles){
			Matrix3f mat = new Matrix3f(t.p1.x, t.p1.y, t.p1.z, t.p2.x, t.p2.y, t.p2.z, t.p3.x, t.p3.y, t.p3.z);
			float vol = mat.determinant();
			center.x += vol*(mat.m00+mat.m10+mat.m20);
			center.y += vol*(mat.m01+mat.m11+mat.m21);
			center.z += vol*(mat.m02+mat.m12+mat.m22);
			volume += vol;
		}
		center.x /= volume*4F;
		center.y /= volume*4F;
		center.z /= volume*4F;
		return center;
	}
	
	private Matrix3f computeInertia(Vec3f com, float mass){
		float volume = 0;
		Vec3f diag = new Vec3f(0, 0, 0);
		Vec3f offd = new Vec3f(0, 0, 0);
		for(Triangle t : triangles){
			Matrix3f mat = new Matrix3f(t.p1.x-com.x, t.p1.y-com.y, t.p1.z-com.z, t.p2.x-com.x, t.p2.y-com.y, t.p2.z-com.z, t.p3.x-com.x, t.p3.y-com.y, t.p3.z-com.z);
			float d = mat.determinant();
			volume += d;
			
			for(int j = 0; j < 3; j ++){
				int j1 = (j+1)%3;
				int j2 = (j+2)%3;
				diag.setVal(j, diag.val(j) + 
						(mat.getElement(0, j)*mat.getElement(1, j) + mat.getElement(1, j)*mat.getElement(2, j) + mat.getElement(2, j)*mat.getElement(0, j) +
						mat.getElement(0, j)*mat.getElement(0, j) + mat.getElement(1, j)*mat.getElement(1, j) + mat.getElement(2, j)*mat.getElement(2, j))*d);
				offd.setVal(j, offd.val(j) +
						(mat.getElement(0, j1)*mat.getElement(1, j2) + mat.getElement(1, j1)*mat.getElement(2, j2) + mat.getElement(2, j1)*mat.getElement(0, j2) + 
						mat.getElement(0, j1)*mat.getElement(2, j2) + mat.getElement(1, j1)*mat.getElement(0, j2) + mat.getElement(2, j1)*mat.getElement(1, j2) +
						mat.getElement(0, j1)*mat.getElement(0, j2) + mat.getElement(1, j1)*mat.getElement(1, j2) + mat.getElement(2, j1)*mat.getElement(2, j2))*d);
			}
		}
		float volume2 = volume*(60F/6F);
		diag.x /= volume2;
		diag.y /= volume2;
		diag.z /= volume2;
		volume2 = volume*(120F/6F);
		offd.x /= volume2;
		offd.y /= volume2;
		offd.z /= volume2;
		diag = diag.scale(mass);
		offd = offd.scale(mass);
		return new Matrix3f(
				diag.y+diag.z, -offd.z, -offd.y,
				-offd.z, diag.x+diag.z, -offd.x,
				-offd.y, -offd.x, diag.x+diag.y);
	}

	@Override
	public Vec3f support(Vec3f dir) {
		double dot = -Float.MAX_VALUE;
		int index = 0;
		for(int i = 0; i < vertices.length; i += 3){
			double newDot = dir.x*vertices[i] + dir.y*vertices[i+1] + dir.z*vertices[i+2];
			if(newDot > dot){
				dot = newDot;
				index = i;
			}
		}
		return new Vec3f(vertices[index], vertices[index+1], vertices[index+2]);
	}

	@Override
	public Collider copy() {
		ConvexMeshCollider c = new ConvexMeshCollider();
		c.vertices = vertices;
		c.indices = indices;
		c.triangles = triangles;
		c.localBox = localBox;
		c.localCentroid = localCentroid;
		c.localInertiaTensor = localInertiaTensor;
		c.mass = mass;
		return c;
	}

	@Override
	public RayTraceResult rayCast(Vec3f pos1, Vec3f pos2) {
		Vec3f rayDir = pos2.subtract(pos1);
		float minDist = Float.MAX_VALUE;
		Vec3f normal = null;
		for(int i = 0; i < triangles.length; i ++){
			Triangle tri = triangles[i];
			float normalDotRayDir = tri.normal.dot(rayDir);
			if(Math.abs(normalDotRayDir) < 0.0001F){
				continue;
			}
			float t = tri.p1.subtract(pos1).dot(tri.normal)/normalDotRayDir;
			if(t < 0 || t > 1 || t > minDist || !tri.checkPointInTriangle(pos1.add(rayDir.scale(t)))){
				continue;
			}
			minDist = t;
			normal = tri.normal;
		}
		if(minDist <= 1){
			return new RayTraceResult(true, minDist, pos1.add(rayDir.scale(minDist)), normal);
		}
		return new RayTraceResult();
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return localBox;
	}

	@Override
	public void debugRender() {
		Tessellator.instance.begin(GL11.GL_LINES, VertexFormat.POSITION);
		for(Triangle t : triangles){
			Tessellator.instance.pos(t.p1.x, t.p1.y, t.p1.z).endVertex();
			Tessellator.instance.pos(t.p2.x, t.p2.y, t.p2.z).endVertex();
			Tessellator.instance.pos(t.p2.x, t.p2.y, t.p2.z).endVertex();
			Tessellator.instance.pos(t.p3.x, t.p3.y, t.p3.z).endVertex();
			Tessellator.instance.pos(t.p3.x, t.p3.y, t.p3.z).endVertex();
			Tessellator.instance.pos(t.p1.x, t.p1.y, t.p1.z).endVertex();
		}
		Tessellator.instance.draw();
	}
	
}
