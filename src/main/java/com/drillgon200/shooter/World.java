package com.drillgon200.shooter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.drillgon200.physics.AABBCollider;
import com.drillgon200.physics.AABBTree;
import com.drillgon200.physics.AxisAlignedBB;
import com.drillgon200.physics.Collider;
import com.drillgon200.physics.ConvexMeshCollider;
import com.drillgon200.physics.RayTraceResult;
import com.drillgon200.shooter.entity.Entity;
import com.drillgon200.shooter.util.Triangle;
import com.drillgon200.shooter.util.Vec3f;

public class World {

	public AABBTree<Collider> static_colliders;
	
	public List<Entity> entities = new ArrayList<>();
	
	public World() {
		static_colliders = new AABBTree<>(0.2F);
		static_colliders.insert(new AABBCollider(new AxisAlignedBB(-10, -1, -10, 10, 0, 300)));
		Triangle[] triangles = new Triangle[2];
		triangles[0] = new Triangle(new Vec3f(1, 0, -1), new Vec3f(0, 0, -3), new Vec3f(0, 3, -3));
		triangles[1] = new Triangle(new Vec3f(0, 3, -3), new Vec3f(1, 3, -1), new Vec3f(1, 0, -1));
		static_colliders.insert(new ConvexMeshCollider(triangles, 1));
	}
	
	/**
	 * Adds an entity to the world
	 * @param ent - The entity to add to the world
	 * @return Whether the entity was successfully added.
	 */
	public boolean addEntity(Entity ent){
		entities.add(ent);
		return true;
	}
	
	public RayTraceResult rayCast(Vec3f start, Vec3f end){
		return static_colliders.rayCast(start, end);
	}
	
	public void updateEntities(){
		Iterator<Entity> itr = entities.iterator();
		while(itr.hasNext()){
			Entity ent = itr.next();
			ent.update();
			if(ent.isDead){
				itr.remove();
			}
		}
	}
}
