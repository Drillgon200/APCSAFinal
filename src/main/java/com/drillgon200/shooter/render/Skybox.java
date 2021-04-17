package com.drillgon200.shooter.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import com.drillgon200.shooter.Resources;
import com.drillgon200.shooter.util.Tessellator;
import com.drillgon200.shooter.util.TextureManager;
import com.drillgon200.shooter.util.VertexFormat;

public class Skybox {

	private String top;
	private String bottom;
	private String back;
	private String front;
	private String left;
	private String right;
	
	public Skybox(String location){
		top = location + "/top.png";
		bottom = location + "/bottom.png";
		back = location + "/back.png";
		front = location + "/front.png";
		left = location + "/left.png";
		right = location + "/right.png";
		
		TextureManager.filterMipmapRepeat(top, true, false, GL12.GL_CLAMP_TO_EDGE);
		TextureManager.filterMipmapRepeat(bottom, true, false, GL12.GL_CLAMP_TO_EDGE);
		TextureManager.filterMipmapRepeat(back, true, false, GL12.GL_CLAMP_TO_EDGE);
		TextureManager.filterMipmapRepeat(front, true, false, GL12.GL_CLAMP_TO_EDGE);
		TextureManager.filterMipmapRepeat(left, true, false, GL12.GL_CLAMP_TO_EDGE);
		TextureManager.filterMipmapRepeat(right, true, false, GL12.GL_CLAMP_TO_EDGE);
	}
	
	public void draw(){
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		Tessellator tes = Tessellator.instance;
		
		Resources.basic_texture.use();
		
		TextureManager.bindTexture(top);
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		tes.pos(-1, 1, -1).tex(0, 0).endVertex();
		tes.pos(1, 1, -1).tex(1, 0).endVertex();
		tes.pos(1, 1, 1).tex(1, 1).endVertex();
		tes.pos(-1, 1, 1).tex(0, 1).endVertex();
		
		tes.draw();
		
		TextureManager.bindTexture(bottom);
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		tes.pos(-1, -1, 1).tex(0, 0).endVertex();
		tes.pos(1, -1, 1).tex(1, 0).endVertex();
		tes.pos(1, -1, -1).tex(1, 1).endVertex();
		tes.pos(-1, -1, -1).tex(0, 1).endVertex();
		tes.draw();
		
		TextureManager.bindTexture(back);
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		tes.pos(-1, 1, 1).tex(1, 1).endVertex();
		tes.pos(1, 1, 1).tex(0, 1).endVertex();
		tes.pos(1, -1, 1).tex(0, 0).endVertex();
		tes.pos(-1, -1, 1).tex(1, 0).endVertex();
		
		tes.draw();
		
		TextureManager.bindTexture(front);
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		tes.pos(-1, -1, -1).tex(0, 0).endVertex();
		tes.pos(1, -1, -1).tex(1, 0).endVertex();
		tes.pos(1, 1, -1).tex(1, 1).endVertex();
		tes.pos(-1, 1, -1).tex(0, 1).endVertex();
		tes.draw();
		
		TextureManager.bindTexture(right);
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		tes.pos(1, -1, 1).tex(1, 0).endVertex();
		tes.pos(1, 1, 1).tex(1, 1).endVertex();
		tes.pos(1, 1, -1).tex(0, 1).endVertex();
		tes.pos(1, -1, -1).tex(0, 0).endVertex();
		tes.draw();
		
		TextureManager.bindTexture(left);
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		tes.pos(-1, -1, -1).tex(1, 0).endVertex();
		tes.pos(-1, 1, -1).tex(1, 1).endVertex();
		tes.pos(-1, 1, 1).tex(0, 1).endVertex();
		tes.pos(-1, -1, 1).tex(0, 0).endVertex();
		tes.draw();
		
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
	}
	
}
