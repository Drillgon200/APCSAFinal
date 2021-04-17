package com.drillgon200.shooter.entity;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.drillgon200.shooter.World;

public class EntityRegistry {

	private Map<String, Class<? extends Entity>> entitiesByName = new HashMap<>();
	private Map<Class<? extends Entity>, String> namesByEntity = new HashMap<>();
	
	public void register(String name, Class<? extends Entity> e){
		entitiesByName.put(name, e);
		namesByEntity.put(e, name);
	}
	
	public Entity constructEntity(String name, World world){
		//All entities must have a single world constructor.
		//I think this is slightly easier than registering a factory for every single entity.
		Class<? extends Entity> clazz = entitiesByName.get(name);
		try {
			Entity ent = clazz.getConstructor(World.class).newInstance(world);
			return ent;
		} catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			//Just crash, it's fatal to the program if we can't add the entity
			throw new RuntimeException(e);
		}
	}
	
	public String getIdName(Class<? extends Entity> clazz){
		return namesByEntity.get(clazz);
	}

	public void registerEntitiesCommon() {
		
	}
}
