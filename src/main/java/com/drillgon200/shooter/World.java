package com.drillgon200.shooter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.drillgon200.physics.RayTraceResult;
import com.drillgon200.shooter.entity.Entity;
import com.drillgon200.shooter.entity.Player;
import com.drillgon200.shooter.level.LevelCommon;
import com.drillgon200.shooter.util.Vec3f;

public abstract class World {

	public Random rand;
	
	public List<Player> playerEntities = new ArrayList<>();
	public List<Entity> entities = new ArrayList<>();
	private Map<Short, Entity> entitiesById = new HashMap<>();
	
	public World() {
		rand = new Random();
		/*static_colliders = new AABBTree<>(0.2F);
		static_colliders.insert(new AABBCollider(new AxisAlignedBB(-10, -1, -10, 10, 0, 290)));
		Triangle[] triangles = new Triangle[2];
		triangles[0] = new Triangle(new Vec3f(1, 0, -1), new Vec3f(0, 0, -3), new Vec3f(0, 3, -3));
		triangles[1] = new Triangle(new Vec3f(0, 3, -3), new Vec3f(1, 3, -1), new Vec3f(1, 0, -1));
		//static_colliders.insert(new ConvexMeshCollider(triangles, 1));
		static_colliders.insert(new TriangleCollider(triangles[0]));
		static_colliders.insert(new TriangleCollider(triangles[1]));
		static_colliders.insert(new TriangleCollider(new Triangle(new Vec3f(-10, 4, 10+20), new Vec3f(10, 4, 10+20), new Vec3f(10, 0, -10+20))));
		static_colliders.insert(new TriangleCollider(new Triangle(new Vec3f(10, 0, -10+20), new Vec3f(-10, 0, -10+20), new Vec3f(-10, 4, 10+20))));
		static_colliders.insert(new TriangleCollider(new Triangle(new Vec3f(-10, 0, 10+40), new Vec3f(10, 0, 10+40), new Vec3f(10, 4, -10+40))));
		static_colliders.insert(new TriangleCollider(new Triangle(new Vec3f(10, 4, -10+40), new Vec3f(-10, 4, -10+40), new Vec3f(-10, 0, 10+40))));*/
	}
	
	/**
	 * Adds an entity to the world
	 * @param ent - The entity to add to the world
	 * @return Whether the entity was successfully added.
	 */
	public boolean addEntity(Entity ent){
		entities.add(ent);
		if(ent instanceof Player)
			playerEntities.add((Player) ent);
		if(entitiesById.containsKey(ent.entityId)){
			throw new RuntimeException("Duplicate entity ids!");
		}
		entitiesById.put(ent.entityId, ent);
		return true;
	}
	
	public RayTraceResult rayCast(Vec3f start, Vec3f end){
		return getLevel().staticColliders.rayCast(start, end);
	}
	
	public abstract LevelCommon getLevel();
	
	public abstract boolean isRemote();
	
	public void updateEntities(){
		Iterator<Entity> itr = entities.iterator();
		List<Entity> deadEntities = new ArrayList<>();
		while(itr.hasNext()){
			Entity ent = itr.next();
			ent.update();
			if(ent.markedForRemoval){
				deadEntities.add(ent);
				itr.remove();
			}
		}
		for(Entity ent : deadEntities){
			removeEntity(ent);
		}
	}
	
	protected void removeEntity(Entity ent){
		if(ent instanceof Player){
			playerEntities.remove(ent);
		}
		entitiesById.remove(ent.entityId);
	}
	
	public Entity getEntityById(short id){
		return entitiesById.get(id);
	}
	
	public void reset() {
		playerEntities.clear();
		entities.clear();
		entitiesById.clear();
	}
}
