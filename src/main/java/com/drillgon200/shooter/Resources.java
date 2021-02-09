package com.drillgon200.shooter;

import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.ShaderManager.Shader;
import com.drillgon200.shooter.util.TextureManager;

public class Resources {

	//Textures
	public static String duck_test = "/assets/shooter/textures/orbus.png";
	
	public static String cc0_gravel = "/assets/shooter/textures/cc0/gravel.png";
	
	public static String crosshair0 = "/assets/shooter/textures/hud/crosshair0.png";
	
	//Shaders
	public static Shader blit;
	public static Shader world;
	public static Shader red;
	
	public static void reload(){
		TextureManager.loadTexture(duck_test);
		TextureManager.filterMipmap(duck_test, true, true);
		TextureManager.loadTexture(cc0_gravel);
		TextureManager.filterMipmap(cc0_gravel, true, true);
		TextureManager.loadTexture(crosshair0);
		TextureManager.filterMipmap(crosshair0, false, false);
		
		blit = ShaderManager.loadShader("/assets/shooter/shaders/blit");
		world = ShaderManager.loadShader("/assets/shooter/shaders/world");
		red = ShaderManager.loadShader("/assets/shooter/shaders/red");
	}
	
	public static void delete(){
		TextureManager.deleteAll();
		ShaderManager.deleteShaders();
	}
}
