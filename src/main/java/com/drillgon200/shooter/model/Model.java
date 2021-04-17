package com.drillgon200.shooter.model;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.drillgon200.physics.Pair;
import com.drillgon200.shooter.animation.AnimationNode;
import com.drillgon200.shooter.animation.Transform;
import com.drillgon200.shooter.render.Material;
import com.drillgon200.shooter.util.GLAllocation;

public class Model {

	//Shouldn't have more than 64 sub models in any given model, right?
	public static final IntBuffer offsets = GLAllocation.createDirectIntBuffer(64);
	public static final IntBuffer counts = GLAllocation.createDirectIntBuffer(64);
	
	public Material material;
	public AnimationNode animRoot;
	public Bone rootBone;
	public int vao;
	public List<Pair<String, int[]>> geometry = new ArrayList<>();
	
	private Model() {
	}
	
	public Model(Material mat, Bone root, int vao, List<Pair<String, int[]>> geometry) {
		this.material = mat;
		this.rootBone = root;
		this.vao = vao;
		this.geometry = geometry;
	}
	
	public void renderAll(){
		GL30.glBindVertexArray(vao);
		for(Pair<String, int[]> p : geometry){
			offsets.put(p.right[0]);
			counts.put(p.right[1]);
		}
		offsets.limit(offsets.position());
		counts.limit(counts.position());
		offsets.rewind();
		counts.rewind();
		GL30.glMultiDrawArrays(GL11.GL_TRIANGLES, offsets, counts);
		GL30.glBindVertexArray(0);
		offsets.limit(offsets.capacity());
		counts.limit(counts.capacity());
	}
	
	public void renderPart(String... parts){
		GL30.glBindVertexArray(vao);
		for(Pair<String, int[]> p : geometry){
			boolean flag = false;
			for(String s : parts){
				if(p.left.equals(s)){
					flag = true;
					break;
				}
			}
			if(flag){
				offsets.put(p.right[0]);
				counts.put(p.right[1]);
			}
		}
		offsets.limit(offsets.position());
		counts.limit(counts.position());
		GL30.glMultiDrawArrays(GL11.GL_TRIANGLES, offsets, counts);
		GL30.glBindVertexArray(0);
	}
	
	public Model copy(){
		Model model = new Model();
		model.material = material;
		if(rootBone != null)
			model.rootBone = rootBone.copy();
		model.vao = vao;
		model.geometry = geometry;
		return model;
	}
	
	public void setPose(long time){
		if(animRoot != null){
			Map<String, Transform> transforms = animRoot.generateTransforms(time);
			
		}
	}
}
