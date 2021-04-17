package com.drillgon200.shooter.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import com.drillgon200.shooter.util.TextureManager;
import com.drillgon200.shooter.util.Vec4f;

public class Material {

	public String name = "";
	
	//Mat textures
	public int color;
	public int metallic;
	public int normals;
	public float specular;
	public float roughness;
	public float ior;
	public Vec4f emission;
	
	public Material(String c, String r, String n, float spec, float rough, float ior, Vec4f emission) {
		int tex = TextureManager.getTexture("/assets/shooter/textures/" + c);
		if(tex == -1){
			tex = TextureManager.loadTexture("/assets/shooter/textures/" + c);
			TextureManager.filterMipmap("/assets/shooter/textures/" + c, true, true);
		}
		color = tex;
		tex = TextureManager.getTexture("/assets/shooter/textures/" + r);
		if(tex == -1){
			tex = TextureManager.loadTexture("/assets/shooter/textures/" + r);
			TextureManager.filterMipmap("/assets/shooter/textures/" + r, true, true);
		}
		metallic = tex;
		tex = TextureManager.getTexture("/assets/shooter/textures/" + n);
		if(tex == -1){
			tex = TextureManager.loadTexture("/assets/shooter/textures/" + n);
			TextureManager.filterMipmap("/assets/shooter/textures/" + n, true, true);
		}
		normals = tex;
		this.specular = spec;
		this.roughness = rough;
		this.ior = ior;
		this.emission = emission;
	}
	
	public Material(int color, int metallic, int normals, float specular, float roughness, float ior, Vec4f emission) {
		this.color = color;
		this.metallic = metallic;
		this.normals = normals;
		this.specular = specular;
		this.roughness = roughness;
		this.ior = ior;
		this.emission = emission;
	}
	
	public void sendUniformData(int shader, String mat){
		GL14.glActiveTexture(GL14.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, color);
		GL14.glActiveTexture(GL14.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, metallic);
		GL14.glActiveTexture(GL14.GL_TEXTURE2);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, normals);
		GL14.glActiveTexture(GL14.GL_TEXTURE0);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, mat + "color"), 0);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, mat + "metallic"), 1);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, mat + "normal"), 2);
		GL20.glUniform1f(GL20.glGetUniformLocation(shader, mat + "specular"), specular);
		GL20.glUniform1f(GL20.glGetUniformLocation(shader, mat + "roughness"), roughness);
		GL20.glUniform1f(GL20.glGetUniformLocation(shader, mat + "ior"), ior);
		GL20.glUniform4f(GL20.glGetUniformLocation(shader, mat + "emission"), emission.x, emission.y, emission.z, emission.w);
	}
}
