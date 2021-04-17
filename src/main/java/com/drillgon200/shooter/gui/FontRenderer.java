package com.drillgon200.shooter.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.drillgon200.shooter.Resources;
import com.drillgon200.shooter.util.GLAllocation;
import com.drillgon200.shooter.util.Tessellator;
import com.drillgon200.shooter.util.VertexFormat;

public class FontRenderer {

	public static FontTexture roboto_mono;
	public static FontTexture metal_lord;
	public static FontTexture perfect_dark;
	public static FontTexture teko_bold;
	
	private static FontTexture activeFont;
	
	public static void init(){
		roboto_mono = initializeFont("/assets/shooter/fonts/RobotoMono-Bold.ttf");
		metal_lord = initializeFont("/assets/shooter/fonts/METALORD.TTF");
		perfect_dark = initializeFont("/assets/shooter/fonts/pdark.ttf");
		teko_bold = initializeFont("/assets/shooter/fonts/Teko-Bold.ttf");
		activeFont = roboto_mono;
	}
	
	public static void setActiveFont(FontTexture font){
		activeFont = font;
	}
	
	public static FontTexture initializeFont(String fontLocation) {
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, FontRenderer.class.getResourceAsStream(fontLocation)).deriveFont(100.0F);
			FontTexture tex = new FontTexture(font, "ascii");
			return tex;
		} catch(Exception x){
			x.printStackTrace();
		}
		return null;
	}
	
	public static float getStringWidth(String str, float scale){
		float width = 0;
		for(char c : str.toCharArray()){
			CharInfo info = activeFont.charMap.get(c);
			width += ((float)info.width * scale)/activeFont.height;
		}
		return width;
	}
	
	public static void drawString(String str, float x, float y, float scale){
		drawString(str, x, y, scale, 1, 1, 1, 1);
	}
	
	public static void drawString(String str, float x, float y, float scale, float r, float g, float b, float a){
		float zLevel = 0F;
		scale *= 100;
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, activeFont.textureId);
		Resources.font_basic.use();
		GL20.glUniform4f(GL20.glGetUniformLocation(Resources.font_basic.getShaderId(), "color"), r, g, b, a);
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		Tessellator tes = Tessellator.instance;
		tes.begin(GL11.GL_QUADS, VertexFormat.POSITION_TEX);
		float currentWidth = 0;
		for(char c : str.toCharArray()){
			CharInfo info = activeFont.charMap.get(c);
			float width = ((float)info.width * scale)/activeFont.height;
			tes.pos(currentWidth + x, y, zLevel).tex(info.minU, info.minV).endVertex();
			tes.pos(currentWidth + width + x, y, zLevel).tex(info.maxU, info.minV).endVertex();
			tes.pos(currentWidth + width + x, y + scale, zLevel).tex(info.maxU, info.maxV).endVertex();
			tes.pos(currentWidth + x, y + scale, zLevel).tex(info.minU, info.maxV).endVertex();
			currentWidth += width;
		}
		tes.draw();
		
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private static String getAvailableCharacters(String charsetName){
		CharsetEncoder enc = Charset.forName(charsetName).newEncoder();
		StringBuilder build = new StringBuilder();
		for(char c = 0; c < Character.MAX_VALUE; c ++){
			if(enc.canEncode(c))
				build.append(c);
		}
		return build.toString();
	}
	
	public static class FontTexture {
		public Font font;
		public String charSetName;
		public int width = 0;
		public int height = 0;
		public int textureId;
		public Map<Character, CharInfo> charMap = new HashMap<>();
		
		public FontTexture(Font f, String csn) throws Exception {
			font = f;
			charSetName = csn;
			
			buildTexture();
		}
		
		public void buildTexture() throws Exception {
			BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2D = img.createGraphics();
			g2D.setFont(font);
			FontMetrics metrics = g2D.getFontMetrics();
			
			String allChars = getAvailableCharacters(charSetName);
			for(char c : allChars.toCharArray()){
				CharInfo info = new CharInfo(width, metrics.charWidth(c));
				charMap.put(c, info);
				info.minU = width;
				width += info.width;
				info.maxU = width;
			}
			height = metrics.getHeight();
			for(char c : allChars.toCharArray()){
				CharInfo info = charMap.get(c);
				//Just in case something weird happens if I try to use an integer
				float fWidth = (float)width;
				info.maxU /= fWidth;
				info.minU /= fWidth;
				info.minV = 0;
				info.maxV = 1;
			}
			g2D.dispose();
			
			img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			g2D = img.createGraphics();
			g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2D.setFont(font);
			metrics = g2D.getFontMetrics();
			g2D.setColor(Color.WHITE);
			g2D.drawString(allChars, 0, metrics.getAscent());
			g2D.dispose();
			
			textureId = GL11.glGenTextures();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
			
			ByteBuffer buf = GLAllocation.createDirectByteBuffer(width*height);
			for(int y = img.getHeight()-1; y >= 0; y --){
				for(int x = 0; x < img.getWidth(); x ++){
					int argb = img.getRGB(x, y);
					buf.put((byte) ((argb >> 24) & 255));
				}
			}
			buf.flip();
			
			GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, width, height, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, buf);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
			
			GLAllocation.freeDirectBuffer(buf);
		}
	}
	
	public static class CharInfo {
		public int startX;
		public int width;
		public float minU;
		public float minV;
		public float maxU;
		public float maxV;
		
		public CharInfo(int sX, int width){
			this.startX = sX;
			this.width = width;
		}
	}
}
