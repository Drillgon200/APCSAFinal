package com.drillgon200.physics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.drillgon200.shooter.util.Vec3f;

public class GJK {

	public static final int gjkMaxIterations = 64;
	public static final int epaMaxIterations = 128;
	
	public static Simplex csoSimplex = new Simplex();
	public static float margin = 0;
	
	//https://www.youtube.com/watch?v=Qupqu1xe7Io
	public static GJKInfo colliding(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b){
		return colliding(bodyA, bodyB, a, b, true);
	}
	public static boolean collidesAny(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b){
		return colliding(bodyA, bodyB, a, b, false) != null;
	}
	public static GJKInfo colliding(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b, boolean epa){
		GJKInfo returnInfo = new GJKInfo();
		csoSimplex.reset();
		Vec3f direction = new Vec3f(0, 0, 1);
		Vec3f supportCSO = doCSOSupport(bodyA, bodyB, a, b, direction).v;
		direction = supportCSO.negate();
		for(int iter = 0; iter < gjkMaxIterations; iter ++){
			supportCSO = doCSOSupport(bodyA, bodyB, a, b, direction).v;
			if(supportCSO.dot(direction) < 0){
				//We didn't find a closer point
				returnInfo.result = Result.SEPARATED;
				if(!epa)
					return null;
				return returnInfo;
			}
			float mask = 1000;
			switch(csoSimplex.size){
			case 0:
			case 1:
				//Should never happen since we already added 2 points.
				break;
			case 2:
				Vec3f ab = csoSimplex.points[1].v.subtract(csoSimplex.points[0].v);
				Vec3f ao = csoSimplex.points[0].v.negate();
				if(ab.dot(ao) > 0){
					direction = ab.cross(ao).cross(ab);
				} else {
					csoSimplex.points[1] = null;
					csoSimplex.size--;
					direction = csoSimplex.points[0].v.scale(-1);
				}
				break;
			case 3:
				ab = csoSimplex.points[1].v.subtract(csoSimplex.points[0].v);
				Vec3f ac = csoSimplex.points[2].v.subtract(csoSimplex.points[0].v);
				Vec3f abc = ab.cross(ac);
				ao = csoSimplex.points[0].v.negate();
				if(iter > mask){
					System.out.println("tri:");
					System.out.println(direction);
					System.out.println(csoSimplex.points[0].v);
					System.out.println(csoSimplex.points[1].v);
					System.out.println(csoSimplex.points[2].v);
				}
				direction = triangleCase(ab, ac, abc, ao);
				if(iter > mask){
					System.out.println(direction);
				}
				break;
			case 4:
				ab = csoSimplex.points[1].v.subtract(csoSimplex.points[0].v);
				ac = csoSimplex.points[2].v.subtract(csoSimplex.points[0].v);
				Vec3f ad = csoSimplex.points[3].v.subtract(csoSimplex.points[0].v);
				ao = csoSimplex.points[0].v.negate();
				if(iter > mask){
					System.out.println("quad: ");
					System.out.println(direction);
					System.out.println(csoSimplex.points[0].v);
					System.out.println(csoSimplex.points[1].v);
					System.out.println(csoSimplex.points[2].v);
					System.out.println(csoSimplex.points[3].v);
				}
				Vec3f dir = tetraCase(ab, ac, ad, ao);
				if(iter > mask){
					System.out.println(dir);
				}
				if(dir == null){
					if(epa)
						EPA(bodyA, bodyB, a, b, returnInfo);
					return returnInfo;
				} else {
					direction = dir;
				}
				break;
			}
		}
		//Fail, most likely because the origin was exactly touching a simplex.
		//I'm not sure how much of a performance impact adding checks for these would be.
		//But it's worth checking out to see if it's needed or not
		//TODO check
		returnInfo.result = Result.GJK_FAILED;
		return returnInfo;
	}
	
	public static Vec3f triangleCase(Vec3f ab, Vec3f ac, Vec3f abc, Vec3f ao){
		if(abc.cross(ac).dot(ao) > 0){
			if(ac.dot(ao) > 0){
				csoSimplex.points[1] = csoSimplex.points[2];
				csoSimplex.points[2] = null;
				csoSimplex.size--;
				return ac.cross(ao).cross(ac);
			} else {
				if(ab.dot(ao) > 0){
					csoSimplex.points[2] = null;
					csoSimplex.size--;
					return ab.cross(ao).cross(ab);
				} else {
					csoSimplex.points[1] = null;
					csoSimplex.points[2] = null;
					csoSimplex.size -= 2;
					return ao;
				}
			}
		} else {
			if(ab.cross(abc).dot(ao) > 0){
				if(ab.dot(ao) > 0){
					csoSimplex.points[2] = null;
					csoSimplex.size--;
					return ab.cross(ao).cross(ab);
				} else {
					csoSimplex.points[1] = null;
					csoSimplex.points[2] = null;
					csoSimplex.size -= 2;
					return ao;
				}
			} else {
				if(abc.dot(ao) > 0){
					return abc;
				} else {
					Mkv tmp = csoSimplex.points[2];
					csoSimplex.points[2] = csoSimplex.points[1];
					csoSimplex.points[1] = tmp;
					return abc.negate();
				}
			}
		}
	}
	
	public static Vec3f tetraCase(Vec3f ab, Vec3f ac, Vec3f ad, Vec3f ao){
		if(ab.cross(ac).dot(ao) > 0){
			csoSimplex.points[3] = null;
			csoSimplex.size--;
			return triangleCase(ab, ac, ab.cross(ac), ao);
		} else if(ac.cross(ad).dot(ao) > 0){
			//Winding order doesn't actually matter here since I'm pretty sure GJK sorts that out in triangleCase and
			//EPA sorts it out when adding a face.
			//Never mind, it definitely did matter
			csoSimplex.points[1] = csoSimplex.points[2];
			csoSimplex.points[2] = csoSimplex.points[3];
			csoSimplex.points[3] = null;
			csoSimplex.size--;
			return triangleCase(ac, ad, ac.cross(ad), ao);
		} else if(ad.cross(ab).dot(ao) > 0){
			csoSimplex.points[2] = csoSimplex.points[1];
			csoSimplex.points[1] = csoSimplex.points[3];
			csoSimplex.points[3] = null;
			csoSimplex.size--;
			return triangleCase(ad, ab, ad.cross(ab), ao);
		} else {
			//Origin is contained by simplex, we're done
			return null;
		}
	}
	
	//Calls csoSupport, possibly will be useful if I need to keep the support points found on a and b as well.
	public static Mkv doCSOSupport(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b, Vec3f direction){
		Vec3f supportCSO = new Vec3f(0, 0, 0);
		csoSupport(bodyA, bodyB, a, b, direction, supportCSO);
		Mkv vert = new Mkv(supportCSO, direction);
		csoSimplex.push_back(vert);
		return vert;
	}
	
	public static void csoSupport(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b, Vec3f dir, Vec3f supportCSO){
		/*if(a.body != null){
			Vec3f vecA = a.body.globalToLocalVec(dir);
			supportA.set(a.body.localToGlobalPos(a.support(vecA)));
		} else {
			supportA.set(a.support(dir));
		}
		if(b.body != null){
			Vec3f vecB = b.body.globalToLocalVec(dir.negate());
			supportB.set(b.body.localToGlobalPos(b.support(vecB)));
		} else {
			supportB.set(b.support(dir.negate()));
		}
		supportCSO.set(supportA.subtract(supportB));*/
		supportCSO.set(localSupport(bodyA, a, dir).subtract(localSupport(bodyB, b, dir.negate())));
	}
	
	public static Vec3f localSupport(RigidBody body, Collider c, Vec3f worldDir){
		if(body != null){
			Vec3f localDir = body.globalToLocalVec(worldDir);
			if(margin != 0){
				localDir = localDir.normalize();
				return body.localToGlobalPos(c.support(localDir).add(localDir.mutateScale(margin)));
			}
			return body.localToGlobalPos(c.support(localDir));
		} else {
			if(margin != 0){
				worldDir = worldDir.normalize();
				return c.support(worldDir).add(worldDir.mutateScale(margin));
			}
			return c.support(worldDir);
		}
	}
	
	/// EPA START ///
	
	private static List<Mkv[]> faces = new ArrayList<>();
	private static List<Mkv[]> edges = new ArrayList<>();
	private static Vec3f[][] features = new Vec3f[2][3];
	
	public static void EPA(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b, GJKInfo info){
		//Create the faces for the first tetrahedron
		faces.add(buildFace(csoSimplex.points[0], csoSimplex.points[1], csoSimplex.points[2]));
		faces.add(buildFace(csoSimplex.points[0], csoSimplex.points[2], csoSimplex.points[3]));
		faces.add(buildFace(csoSimplex.points[0], csoSimplex.points[3], csoSimplex.points[1]));
		faces.add(buildFace(csoSimplex.points[1], csoSimplex.points[2], csoSimplex.points[3]));
		for(int iter = 0; iter < epaMaxIterations; iter ++){
			Mkv[] closestFace = null;
			double smallestDist = Double.MAX_VALUE;
			for(Mkv[] face : faces){
				double lenSq = originDistToPlaneSq(face);
				if(lenSq < smallestDist){
					smallestDist = lenSq;
					closestFace = face;
				}
			}
			Mkv support = doCSOSupport(bodyA, bodyB, a, b, closestFace[3].v);
			final float epsilon = 0.0001F;
			double dist = distToPlaneSq(closestFace, support.v);
			//System.out.println(iter + " " + dist);
			if(dist < epsilon){
				info.result = Result.COLLIDING;
				Vec3f separation = planeProjectOrigin(closestFace);
				info.normal = closestFace[3].v;
				info.depth = (float) separation.len();
				for(int i = 0; i < 3; i ++){
					features[0][i] = localSupport(bodyA, a, closestFace[i].r);
					features[1][i] = localSupport(bodyB, b, closestFace[i].r.negate());
				}
				Vec3f bCoords = barycentricCoords(closestFace, separation);
				info.contactPointA = new Vec3f(
						features[0][0].x*bCoords.x+features[0][1].x*bCoords.y+features[0][2].x*bCoords.z,
						features[0][0].y*bCoords.x+features[0][1].y*bCoords.y+features[0][2].y*bCoords.z,
						features[0][0].z*bCoords.x+features[0][1].z*bCoords.y+features[0][2].z*bCoords.z);
				//Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTauHit(Minecraft.getMinecraft().world, features[0][0].xCoord, features[0][0].yCoord, features[0][0].zCoord, 1F, new Vec3fd(0, 0, 1)));
				//Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTauHit(Minecraft.getMinecraft().world, features[0][1].xCoord, features[0][1].yCoord, features[0][1].zCoord, 1F, new Vec3fd(0, 0, 1)));
				//Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTauHit(Minecraft.getMinecraft().world, features[0][2].xCoord, features[0][2].yCoord, features[0][2].zCoord, 1F, new Vec3fd(0, 0, 1)));
				//Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTauHit(Minecraft.getMinecraft().world, info.contactPointA.xCoord, info.contactPointA.yCoord, info.contactPointA.zCoord, 2F, new Vec3fd(0, 0, 1)));
				info.contactPointB = new Vec3f(
						features[1][0].x*bCoords.x+features[1][1].x*bCoords.y+features[1][2].x*bCoords.z,
						features[1][0].y*bCoords.x+features[1][1].y*bCoords.y+features[1][2].y*bCoords.z,
						features[1][0].z*bCoords.x+features[1][1].z*bCoords.y+features[1][2].z*bCoords.z);
				
				faces.clear();
				return;
			}
			//E x p a n d  the polytope
			Iterator<Mkv[]> itr = faces.iterator();
			while(itr.hasNext()){
				Mkv[] face = itr.next();
				if(face[3].v.dot(support.v.subtract(face[0].v)) > 0){
					itr.remove();
					Mkv[] edge = new Mkv[]{face[1], face[0]};
					if(!removeEdge(edge)){
						edge[0] = face[0];
						edge[1] = face[1];
						edges.add(edge);
					}
					edge = new Mkv[]{face[2], face[1]};
					if(!removeEdge(edge)){
						edge[0] = face[1];
						edge[1] = face[2];
						edges.add(edge);
					}
					edge = new Mkv[]{face[0], face[2]};
					if(!removeEdge(edge)){
						edge[0] = face[2];
						edge[1] = face[0];
						edges.add(edge);
					}
				}
			}
			for(Mkv[] edge : edges){
				faces.add(buildFace(edge[0], edge[1], support));
			}
			edges.clear();
		}
		faces.clear();
		info.result = Result.EPA_FAILED;
	}
	
	//I don't trust ArrayList's default remove to work with arrays, and I don't want to spend the time to check it.
	public static boolean removeEdge(Mkv[] edge){
		Iterator<Mkv[]> itr = edges.iterator();
		while(itr.hasNext()){
			Mkv[] edge2 = itr.next();
			if(edge[0] == edge2[0] && edge[1] == edge2[1]){
				itr.remove();
				return true;
			}
		}
		return false;
	}
	
	public static Vec3f planeProjectOrigin(Mkv[] face){
		Vec3f point = face[0].v;
		float dot = face[3].v.dot(point);
		return face[3].v.scale(dot);
	}
	
	public static double distToPlaneSq(Mkv[] face, Vec3f point){
		float dot = face[3].v.dot(point.subtract(face[0].v));
		Vec3f proj = face[3].v.scale(dot);
		return proj.lenSq();
	}
	
	public static double originDistToPlaneSq(Mkv[] face){
		float dot = face[3].v.dot(face[0].v);
		Vec3f proj = face[3].v.scale(dot);
		return proj.lenSq();
	}
	
	public static Mkv[] buildFace(Mkv a, Mkv b, Mkv c){
		Vec3f ab = b.v.subtract(a.v);
		Vec3f ac = c.v.subtract(a.v);
		Vec3f ao = a.v.negate();
		Vec3f normal = ab.cross(ac).normalize();
		if(normal.dot(ao) < 0){
			return new Mkv[]{a, b, c, new Mkv(normal, null)};
		} else {
			return new Mkv[]{a, c, b, new Mkv(normal.negate(), null)};
		}
	}
	
	public static Vec3f barycentricCoords(Mkv[] face, Vec3f point){
		//Idea is that the barycentric coordinate is the area of the opposite triangle to the vertex, so we compute that with the cross product
		//and make that the weight. You also have to divide by the sum of the weights to normalize them.
		//I was under the impression that the area of the triangle would be the cross product over 2, but apparently the barycentric coords don't need that.
		//I'm thinking this is because the normalization deals with that for me.
		float u = face[1].v.subtract(point).cross(face[2].v.subtract(point)).len();
		float v = face[0].v.subtract(point).cross(face[2].v.subtract(point)).len();
		float w = face[0].v.subtract(point).cross(face[1].v.subtract(point)).len();
		//Normalize
		float uvw = u+v+w;
		return new Vec3f(u, v, w).scale(1F/uvw);
	}
	
	public static class Simplex {
		public int size = 0;
		public Mkv[] points = new Mkv[4];
		
		public void push_back(Mkv vec){
			for(int i = Math.min(size, 2); i >= 0; i --){
				points[i+1] = points[i];
			}
			points[0] = vec;
			size ++;
			if(size > 4)
				size = 4;
		}
		
		public void reset(){
			size = 0;
			for(int i = 0; i < 4; i ++){
				points[i] = null;
			}
		}
		
		public Simplex copy(){
			Simplex simp = new Simplex();
			simp.size = size;
			for(int i = 0; i < 4; i ++){
				simp.points[i] = points[i].copy();
			}
			return simp;
		}
	}
	
	//Minkowski vertex, a struct for both the vertex on the minkowski difference and the ray that got there for extracting the contact.
	//Idea from the bullet physics engine.
	public static class Mkv {
		public Vec3f v;
		public Vec3f r;
		
		public Mkv(Vec3f point, Vec3f direction) {
			this.v = point;
			this.r = direction;
		}
		
		public Mkv copy(){
			Mkv vert = new Mkv(v.copy(), r.copy());
			return vert;
		}
	}
	
	public static class GJKInfo {
		public Result result;
		public Vec3f normal;
		public float depth;
		public Vec3f contactPointA;
		public Vec3f contactPointB;
	}
	
	public static enum Result {
		COLLIDING,
		SEPARATED,
		GJK_FAILED,
		EPA_FAILED;
	}
}
