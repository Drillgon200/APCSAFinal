package com.drillgon200.shooter.level;

import java.util.Queue;

import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.drillgon200.physics.AABBTree;
import com.drillgon200.shooter.render.Light;
import com.drillgon200.shooter.render.Material;
import com.drillgon200.shooter.render.StaticGeometry;
import com.drillgon200.shooter.util.TextureManager;

public class LevelClient extends LevelCommon {

	public AABBTree<Light> staticLights = new AABBTree<>(0.2F);
	public Material[] levelMaterials;
	public Queue<int[]>[] renderersByMaterial;
	public AABBTree<StaticGeometry> staticGeometry = new AABBTree<>(0);
	public int geometryVao;
	
	public String[] lightmaps;
	
	public void sendLightmapUniforms(int shader, int textureOffset) {
		GL13.glActiveTexture(textureOffset);
		TextureManager.bindTexture(lightmaps[0]);
		GL13.glActiveTexture(textureOffset+1);
		TextureManager.bindTexture(lightmaps[1]);
		
		textureOffset -= GL14.GL_TEXTURE0;
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "lmap_color"), textureOffset);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "lmap_direction"), textureOffset+1);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}
	
	@Override
	public void delete() {
		GL30.glDeleteVertexArrays(geometryVao);
		super.delete();
	}
}
