package com.drillgon200.shooter;

import java.nio.IntBuffer;
import java.util.Queue;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import com.drillgon200.physics.AABBTree;
import com.drillgon200.shooter.entity.Player;
import com.drillgon200.shooter.level.LevelClient;
import com.drillgon200.shooter.level.LevelCommon;
import com.drillgon200.shooter.render.ClusterManager;
import com.drillgon200.shooter.render.Light;
import com.drillgon200.shooter.util.GLAllocation;

public class WorldClient extends World {

	public LevelClient level;
	
	public int ticksSinceLastStateUpdate = 0;
	
	public AABBTree<Light> lights = new AABBTree<>(0.2F);
	
	public IntBuffer geoOffsets;
	public IntBuffer geoCounts;
	
	public void gatherLights(){
		lights.updateTree();
		
		/*int index = 0;
		for(Light l : level.staticLights){
			l.setIndex(index++);
			l.addToBuffer(ClusterManager.lightBuffer);
		}
		
		for(Light l : lights){
			l.setIndex(index++);
			l.addToBuffer(ClusterManager.lightBuffer);
		}
		
		ClusterManager.lightBuffer.limit(ClusterManager.lightBuffer.position());
		ClusterManager.lightBuffer.rewind();
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, ClusterManager.lightTBO);
		GL30.glBufferData(GL31.GL_TEXTURE_BUFFER, ClusterManager.lightBuffer, GL30.GL_STREAM_DRAW);
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
		ClusterManager.lightBuffer.limit(ClusterManager.lightBuffer.capacity());
		*/
		
		
		int index = level.staticLights.size();
		for(Light l : lights){
			if(index >= ClusterManager.MAX_LIGHTS)
				break;
			l.setIndex(index++);
			l.addToBuffer(ClusterManager.lightBuffer);
		}
		
		ClusterManager.lightBuffer.limit(ClusterManager.lightBuffer.position());
		ClusterManager.lightBuffer.rewind();
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, ClusterManager.lightTBO);
		GL30.glBufferSubData(GL31.GL_TEXTURE_BUFFER, level.staticLights.size()*Light.BYTES_PER_LIGHT, ClusterManager.lightBuffer);
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
		ClusterManager.lightBuffer.limit(ClusterManager.lightBuffer.capacity());
		
	}
	
	public void setLevel(LevelClient newLevel){
		if(geoOffsets == null){
			geoOffsets = GLAllocation.createDirectIntBuffer(1024);
			geoCounts = GLAllocation.createDirectIntBuffer(1024);
		}
		if(level != null){
			level.delete();
		}
		level = newLevel;
		
		int index = 0;
		for(Light l : level.staticLights){
			l.setIndex(index++);
			l.addToBuffer(ClusterManager.lightBuffer);
		}
		ClusterManager.lightBuffer.limit(ClusterManager.lightBuffer.position());
		ClusterManager.lightBuffer.rewind();
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, ClusterManager.lightTBO);
		GL30.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, ClusterManager.lightBuffer);
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
		ClusterManager.lightBuffer.limit(ClusterManager.lightBuffer.capacity());
	}
	
	@Override
	public LevelCommon getLevel() {
		return level;
	}
	
	@Override
	public boolean isRemote() {
		return true;
	}
	
	public void renderStaticGeo(){
		GL30.glBindVertexArray(level.geometryVao);
		level.staticGeometry.forEach(geo -> level.renderersByMaterial[geo.materialIndex].add(geo.offsetAndCount));
		for(int i = 0; i < level.renderersByMaterial.length; i ++){
			Queue<int[]> list = level.renderersByMaterial[i];
			level.levelMaterials[i].sendUniformData(Resources.world.getShaderId(), "mat.");
			for(int[] offsetAndCount : list){
				geoOffsets.put(offsetAndCount[0]);
				geoCounts.put(offsetAndCount[1]);
				//GL30.glDrawArrays(GL11.GL_TRIANGLES, offsetAndCount[0], offsetAndCount[1]);
			}
			geoOffsets.flip();
			geoCounts.flip();
			GL30.glMultiDrawArrays(GL11.GL_TRIANGLES, geoOffsets, geoCounts);
			geoOffsets.clear();
			geoCounts.clear();
			list.clear();
		}
		
		GL30.glBindVertexArray(0);
	}
}
