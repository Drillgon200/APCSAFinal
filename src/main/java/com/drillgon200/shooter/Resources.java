package com.drillgon200.shooter;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.drillgon200.shooter.render.Material;
import com.drillgon200.shooter.util.Matrix4f;
import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.ShaderManager.Shader;
import com.drillgon200.shooter.util.ShaderManager.Shader.Uniform;
import com.drillgon200.shooter.util.TextureManager;
import com.drillgon200.shooter.util.Vec4f;

public class Resources {

	public static final Uniform SCREEN_SIZE = shader -> {
		GL20.glUniform2f(GL20.glGetUniformLocation(shader, "screenSize"), Shooter.displayWidth, Shooter.displayHeight);
	};
	
	public static final Uniform VIEW_MATRIX = shader -> {
		Shooter.viewMatrix.store(Shooter.MATRIX_BUFFER);
		Shooter.MATRIX_BUFFER.rewind();
		GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(shader, "view_matrix"), false, Shooter.MATRIX_BUFFER);
	};
	
	public static final Uniform MODEL_MATRIX = shader -> {
		Matrix4f model = new Matrix4f();
		GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, Shooter.MATRIX_BUFFER);
		model.load(Shooter.MATRIX_BUFFER);
		Shooter.MATRIX_BUFFER.rewind();
		Matrix4f.mul(Shooter.inverseViewMatrix, model, model);
		model.store(Shooter.MATRIX_BUFFER);
		Shooter.MATRIX_BUFFER.rewind();
		GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(shader, "model_matrix"), false, Shooter.MATRIX_BUFFER);
	};
	
	public static final Uniform COLOR = shader -> {
		ShaderManager.color(1, 1, 1, 1);
	};
	
	//Textures
	public static String duck_test = "/assets/shooter/textures/orbus.png";
	public static String white = "/assets/shooter/textures/white.png";
	
	public static Material gravel;
	
	
	//General HUD textures
	public static String ctf_arena = "/assets/shooter/textures/hud/ctf_arena.png";
	public static String crosshair0 = "/assets/shooter/textures/hud/crosshair0.png";
	public static String button0 = "/assets/shooter/textures/hud/button.png";
	public static String menuButton0 = "/assets/shooter/textures/hud/menubutton.png";
	public static String textBox0 = "/assets/shooter/textures/hud/textbox.png";
	public static String infoBox0 = "/assets/shooter/textures/hud/infobox.png";
	
	//Shaders
	public static Shader blit;
	public static Shader world;
	public static Shader font_basic;
	public static Shader basic_texture;
	public static Shader red;
	
	public static void reload(){
		ImageIO.setUseCache(false);
		gravel = new Material("cc0/gravelc.png", "cc0/gravelr.png", "cc0/graveln.png", 0, 1, 1.5F, new Vec4f(0));
		TextureManager.filterMipmap(duck_test, true, true);
		TextureManager.filterMipmap(white, false, false);
		
		TextureManager.filterMipmap(ctf_arena, true, true);
		TextureManager.filterMipmap(crosshair0, false, false);
		TextureManager.filterMipmap(button0, true, true);
		TextureManager.filterMipmap(menuButton0, true, true);
		TextureManager.filterMipmap(textBox0, true, true);
		TextureManager.filterMipmap(infoBox0, true, true);
		
		blit = ShaderManager.loadShader("/assets/shooter/shaders/blit");
		world = ShaderManager.loadShader("/assets/shooter/shaders/world").withUniforms(SCREEN_SIZE, VIEW_MATRIX);
		font_basic = ShaderManager.loadShader("/assets/shooter/shaders/font_basic");
		basic_texture = ShaderManager.loadShader("/assets/shooter/shaders/basic_texture").withUniforms(VIEW_MATRIX, COLOR);
		red = ShaderManager.loadShader("/assets/shooter/shaders/red").withUniforms(VIEW_MATRIX);
	}
	
	public static void delete(){
		TextureManager.deleteAll();
		ShaderManager.deleteShaders();
	}
	
	public static byte[] resourceToByteArray(String path){
		InputStream rsc = Resources.class.getResourceAsStream(path);
		try {
			byte[] bytes = new byte[256];
			int idx = 0;
			int current = 0;
			while((current = rsc.read()) != -1){
				if(idx >= bytes.length){
					byte[] old = bytes;
					bytes = new byte[bytes.length*2];
					System.arraycopy(old, 0, bytes, 0, old.length);
				}
				bytes[idx++] = (byte)current;
			}
			byte[] last = new byte[idx];
			System.arraycopy(bytes, 0, last, 0, idx);
			return last;
		} catch(IOException e) {
			e.printStackTrace();
		}
		return new byte[0];
	}
}
