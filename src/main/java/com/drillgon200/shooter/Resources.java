package com.drillgon200.shooter;

import com.drillgon200.shooter.util.ShaderManager;
import com.drillgon200.shooter.util.ShaderManager.Shader;
import com.drillgon200.shooter.util.TextureManager;

public class Resources {

	//Textures
	public static String duck_test = "/assets/shooter/textures/orbus.png";
	
	public static String cc0_gravel = "/assets/shooter/textures/cc0/gravel.png";
	
	//Shaders
	public static Shader blit;
	public static Shader world;
	
	public static void reload(){
		TextureManager.loadTexture(duck_test);
		TextureManager.interp(duck_test, true);
		TextureManager.loadTexture(cc0_gravel);
		TextureManager.interp(cc0_gravel, true);
		
		blit = ShaderManager.loadShader("/assets/shooter/shaders/blit");
		world = ShaderManager.loadShader("/assets/shooter/shaders/world");
	}
	
	public static void delete(){
		TextureManager.deleteAll();
		ShaderManager.deleteShaders();
	}
}
