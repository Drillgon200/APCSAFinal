package com.drillgon200.shooter.util;

import org.lwjgl.opengl.GL11;

public class RenderHelper {

	public static void drawFullscreenTriangle(){
		GL11.glColorMask(true, true, true, false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

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
