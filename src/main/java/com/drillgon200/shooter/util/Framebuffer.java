package com.drillgon200.shooter.util;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import com.drillgon200.shooter.MainConfig;

public class Framebuffer {

	public int fbo = -1;
	public int fbo_tex = -1;
	public int depth_tex = -1;
	public int width;
	public int height;
	public boolean usesDepth;
	public boolean usesStencil;
	public boolean multisample;
	public float[] color = {0, 0, 0, 1};
	
	public Framebuffer(int width, int height, boolean depth, boolean stencil, boolean multisample){
		this.width = width;
		this.height = height;
		usesDepth = depth;
		usesStencil = stencil;
		if(MainConfig.MSAA <= 0)
			multisample = false;
		this.multisample = multisample;
		setupFBO(width, height, depth, stencil);
		checkFramebuffer();
	}

	public void checkFramebuffer() {
		int test = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if(test != GL30.GL_FRAMEBUFFER_COMPLETE){
			throw new RuntimeException("Failed to create framebuffer! Code: " + test);
		}
	}

	public void setupFBO(int width, int height, boolean depth, boolean stencil) {
		fbo = GL30.glGenFramebuffers();
		bindFramebuffer(true);
		fbo_tex = GL11.glGenTextures();
		if(multisample){
			GL11.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, fbo_tex);
			GL32.glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, MainConfig.MSAA, GL11.GL_RGBA8, width, height, false);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D_MULTISAMPLE, fbo_tex, 0);
			if(depth){
				depth_tex = GL11.glGenTextures();
				GL11.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, depth_tex);
				if(stencil){
					GL32.glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, MainConfig.MSAA, GL30.GL_DEPTH24_STENCIL8, width, height, false);
					GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL32.GL_TEXTURE_2D_MULTISAMPLE, depth_tex, 0);
				} else {
					GL32.glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, MainConfig.MSAA, GL14.GL_DEPTH_COMPONENT24, width, height, false);
					GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL32.GL_TEXTURE_2D_MULTISAMPLE, depth_tex, 0);
				}
			}
		} else {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo_tex);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (IntBuffer)null);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fbo_tex, 0);
			if(depth){
				depth_tex = GL11.glGenTextures();
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, depth_tex);
				if(stencil){
					GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, (IntBuffer)null);
					GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, depth_tex, 0);
				} else {
					GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer)null);
					GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depth_tex, 0);
				}
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			}
		}
	}
	
	public void clear(){
		bindFramebuffer(false);
		GL11.glClearColor(color[0], color[1], color[2], color[3]);
		if(usesDepth){
			GL11.glClearDepth(1);
			if(usesStencil){
				GL11.glClearStencil(0);
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
			} else {
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			}
		} else {
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		}
	}
	
	public void deleteFBO(){
		GL11.glDeleteTextures(fbo_tex);
		GL11.glDeleteTextures(depth_tex);
		GL30.glDeleteFramebuffers(fbo);
	}
	
	public void bindFramebuffer(boolean viewport){
		if(viewport)
			GL11.glViewport(0, 0, width, height);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
	}
	
	public void unbindFramebuffer() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
	
	public void blit(){
		GL11.glColorMask(true, true, true, false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo_tex);
        Tessellator tessellator = Tessellator.instance;
        tessellator.begin(GL11.GL_TRIANGLES, VertexFormat.POSITION_TEX);
        tessellator.pos(-1, -1, 0.0F).tex(0, 0).endVertex();
        tessellator.pos(3, -1, 0.0F).tex(2, 0).endVertex();
        tessellator.pos(-1, 3, 0.0F).tex(0, 2).endVertex();
        tessellator.draw();
        GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
	}
}
