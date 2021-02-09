package com.drillgon200.shooter.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public class TextureManager {

	private static final Map<String, Integer> textures = new HashMap<>();
	private static ByteBuffer data = GLAllocation.createDirectByteBuffer(1024*1024);
	
	public static int loadTexture(String location){
		if(textures.containsKey(location)){
			deleteTexture(location);
		}
		try {
			int tex = GL11.glGenTextures();
			BufferedImage img = ImageIO.read(TextureManager.class.getResourceAsStream(location));
			int size = img.getWidth()*img.getHeight()*4;
			if(size > data.capacity()){
				data = GLAllocation.createDirectByteBuffer(size);
			}
			for(int y = img.getHeight()-1; y >= 0; y --){
				for(int x = 0; x < img.getWidth(); x ++){
					int argb = img.getRGB(x, y);
					data.put((byte) ((argb >> 16) & 255));
					data.put((byte) ((argb >> 8) & 255));
					data.put((byte) (argb & 255));
					data.put((byte) ((argb >> 24) & 255));
				}
			}
			data.rewind();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, img.getWidth(), img.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_REPEAT);
			textures.put(location, tex);
		} catch(IOException e){
			throw new RuntimeException("Failed to load texture: " + location, e);
		}
		return -1;
	}
	
	public static void bindTexture(String tex){
		int id = textures.getOrDefault(tex, -1);
		if(id == -1){
			id = loadTexture(tex);
		}
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
	}
	
	public static int getTexture(String tex){
		int id = textures.getOrDefault(tex, -1);
		return id;
	}
	
	public static void deleteTexture(String tex){
		int id = textures.getOrDefault(tex, -1);
		if(id != -1){
			GL11.glDeleteTextures(id);
			textures.remove(tex);
		}
	}
	
	public static void deleteAll(){
		for(int tex : textures.values()){
			GL11.glDeleteTextures(tex);
		}
		textures.clear();
	}
	
	public static void filterMipmap(String tex, boolean interp, boolean mipmap){
		bindTexture(tex);
		int nearFilter = GL11.GL_NEAREST;
		int farFilter = GL11.GL_NEAREST;
		if(mipmap){
			GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
			if(GL.getCapabilities().GL_EXT_texture_filter_anisotropic){
				float amount = Math.min(4F, GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT));
				GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
			}
		}
		if(interp){
			farFilter = mipmap ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR;
			nearFilter = GL11.GL_LINEAR;
		} else {
			farFilter = mipmap ? GL11.GL_LINEAR_MIPMAP_NEAREST : GL11.GL_NEAREST;
		}
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, nearFilter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, farFilter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_REPEAT);
	}
}
