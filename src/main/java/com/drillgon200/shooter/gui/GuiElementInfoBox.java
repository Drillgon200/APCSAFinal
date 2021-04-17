package com.drillgon200.shooter.gui;

import java.util.Deque;

import org.lwjgl.opengl.GL11;

import com.drillgon200.shooter.Resources;
import com.drillgon200.shooter.util.RenderHelper;
import com.drillgon200.shooter.util.TextureManager;

public class GuiElementInfoBox extends GuiElement {

	public Deque<String> info;
	
	public GuiElementInfoBox(GuiScreen screen, float posX, float posY, float width, float height, Deque<String> info) {
		super(screen, posX, posY, width, height);
		this.info = info;
	}
	
	@Override
	public void draw(float w, float h) {
		TextureManager.bindTexture(Resources.infoBox0);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		RenderHelper.drawGuiRect(posX, posY, this.width, this.height, 0, 0, 1, 1);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		
		int maxLog = (int) (height/15.5);
		while(info.size() > maxLog){
			info.removeFirst();
		}
		
		GL11.glPushMatrix();
		for(String s : info){
			FontRenderer.setActiveFont(FontRenderer.teko_bold);
			FontRenderer.drawString(s, posX+0.13F*width, posY+0.85F*height, 0.25F, 0.5F, 0, 0, 1);
			GL11.glTranslated(0, -0.025*height, 0);
		}
		Resources.basic_texture.use();
		GL11.glPopMatrix();
	}

}
