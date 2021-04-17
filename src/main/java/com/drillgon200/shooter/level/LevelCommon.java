package com.drillgon200.shooter.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import com.drillgon200.physics.AABBTree;
import com.drillgon200.physics.AxisAlignedBB;
import com.drillgon200.physics.Collider;
import com.drillgon200.physics.RayTraceResult;
import com.drillgon200.physics.TreeObject;
import com.drillgon200.shooter.render.Light;
import com.drillgon200.shooter.render.Material;
import com.drillgon200.shooter.render.StaticGeometry;
import com.drillgon200.shooter.util.TextureManager;
import com.drillgon200.shooter.util.Vec3f;

public class LevelCommon {

	public String name;
	public List<Vec3f> possibleSpawns = new ArrayList<>();
	public AABBTree<Collider> staticColliders = new AABBTree<>(0.2F);
	
	public void delete(){
		
	}
	
}
