package com.drillgon200.shooter.util;

import org.lwjgl.opengl.GL11;

import com.drillgon200.physics.AxisAlignedBB;

public class RenderHelper {

	public static void drawGuiRect(float x, float y, float width, float height, float u, float v, float uMax, float vMax){
        Tessellator tessellator = Tessellator.instance;
        tessellator.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
        tessellator.pos(x, y, 0.0F).tex(u, v).endVertex();
        tessellator.pos(x + width, y, 0.0F).tex(uMax, v).endVertex();
        tessellator.pos(x + width, y + height, 0.0F).tex(uMax, vMax).endVertex();
        tessellator.pos(x, y + height, 0.0F).tex(u, vMax).endVertex();
        
        tessellator.draw();
	}
	
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
	
	public static void drawBoundingBox(AxisAlignedBB box){
        drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public static void drawBoundingBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ){
        Tessellator.instance.begin(GL11.GL_LINE_STRIP, VertexFormat.POSITION);
        drawBoundingBox2(minX, minY, minZ, maxX, maxY, maxZ);
        Tessellator.instance.draw();
    }

    public static void drawBoundingBox2(float minX, float minY, float minZ, float maxX, float maxY, float maxZ){
        Tessellator.instance.pos(minX, minY, minZ).endVertex();
        Tessellator.instance.pos(minX, minY, minZ).endVertex();
        Tessellator.instance.pos(maxX, minY, minZ).endVertex();
        Tessellator.instance.pos(maxX, minY, maxZ).endVertex();
        Tessellator.instance.pos(minX, minY, maxZ).endVertex();
        Tessellator.instance.pos(minX, minY, minZ).endVertex();
        Tessellator.instance.pos(minX, maxY, minZ).endVertex();
        Tessellator.instance.pos(maxX, maxY, minZ).endVertex();
        Tessellator.instance.pos(maxX, maxY, maxZ).endVertex();
        Tessellator.instance.pos(minX, maxY, maxZ).endVertex();
        Tessellator.instance.pos(minX, maxY, minZ).endVertex();
        Tessellator.instance.pos(minX, maxY, maxZ).endVertex();
        Tessellator.instance.pos(minX, minY, maxZ).endVertex();
        Tessellator.instance.pos(maxX, maxY, maxZ).endVertex();
        Tessellator.instance.pos(maxX, minY, maxZ).endVertex();
        Tessellator.instance.pos(maxX, maxY, minZ).endVertex();
        Tessellator.instance.pos(maxX, minY, minZ).endVertex();
        Tessellator.instance.pos(maxX, minY, minZ).endVertex();
    }
    
    public static void drawCapsule(Vec3f offset, float height, float radius, float numSegments){
		float circle = 2*(float)Math.PI;
		float semicircle = (float)Math.PI;
		float circleStep = circle/numSegments;
		Tessellator.instance.setOffset(offset);
		Tessellator.instance.begin(GL11.GL_LINE_STRIP, VertexFormat.POSITION);
		Vec3f vec = new Vec3f(0, 0, radius);
		//Top cylinder circle
		for(float i = 0; i < circle; i+=circleStep){
			Vec3f pos = vec.rotateY(i);
			Tessellator.instance.pos(pos.x, pos.y+height*0.5F, pos.z).endVertex();
		}
		//Z plane top semicircle
		for(float i = 0; i <= semicircle; i += circleStep){
			Vec3f pos = vec.rotateX(i);
			Tessellator.instance.pos(pos.x, pos.y+height*0.5F, pos.z).endVertex();
		}
		vec = new Vec3f(0, 0, -radius);
		//Bottom Z plane quarter circle before big loop
		for(float i = 0; i < semicircle*0.5F; i += circleStep){
			Vec3f pos = vec.rotateX(i);
			Tessellator.instance.pos(pos.x, pos.y-height*0.5F, pos.z).endVertex();
		}
		//Do big X plane loop
		vec = new Vec3f(0, -radius, 0);
		for(float i = 0; i <= semicircle*0.5F; i += circleStep){
			Vec3f pos = vec.rotateZ(i);
			Tessellator.instance.pos(pos.x, pos.y-height*0.5F, pos.z).endVertex();
		}
		vec = new Vec3f(-radius, 0, 0);
		for(float i = 0; i <= semicircle; i += circleStep){
			Vec3f pos = vec.rotateZ(i);
			Tessellator.instance.pos(pos.x, pos.y+height*0.5F, pos.z).endVertex();
		}
		vec = new Vec3f(radius, 0, 0);
		for(float i = 0; i < semicircle*0.5F; i += circleStep){
			Vec3f pos = vec.rotateZ(i);
			Tessellator.instance.pos(pos.x, pos.y-height*0.5F, pos.z).endVertex();
		}
		vec = new Vec3f(0, -radius, 0);
		//Big X plane loop completed, finish other X plane bottom quarter circle
		for(float i = 0; i < semicircle*0.5F; i += circleStep){
			Vec3f pos = vec.rotateX(i);
			Tessellator.instance.pos(pos.x, pos.y-height*0.5F, pos.z).endVertex();
		}
		vec = new Vec3f(0, 0, radius);
		//Bottom cylinder circle
		for(float i = 0; i <= circle; i+=circleStep){
			Vec3f pos = vec.rotateY(i);
			Tessellator.instance.pos(pos.x, pos.y-height*0.5F, pos.z).endVertex();
		}
		//Last connecting vertex back to start
		Tessellator.instance.pos(0, height*0.5F, radius).endVertex();
		Tessellator.instance.draw();
		Tessellator.instance.setOffset(0, 0, 0);
    }
}
