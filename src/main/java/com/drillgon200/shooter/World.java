package com.drillgon200.shooter;

import java.util.ArrayList;
import java.util.List;

import com.drillgon200.shooter.entity.Entity;

public class World {

	public List<Entity> entities = new ArrayList<>();
	
	/**
	 * Adds an entity to the world
	 * @param ent - The entity to add to the world
	 * @return Whether the entity was successfully added.
	 */
	public boolean addEntity(Entity ent){
		entities.add(ent);
		return true;
	}
	
	public void updateEntities(){
		for(Entity ent : entities){
			ent.update();
		}
	}
}
