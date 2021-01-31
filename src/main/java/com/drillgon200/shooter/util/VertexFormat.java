package com.drillgon200.shooter.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class VertexFormat {

	public static final Element POS_3F = new Element(GL11.GL_FLOAT, 3, false, 12);
	public static final Element TEX_2F = new Element(GL11.GL_FLOAT, 2, false, 8);
	
	public static final VertexFormat POSITION_TEX = new VertexFormat(POS_3F, TEX_2F);
	
	public Element[] elements;
	public int bytes_per_vertex;
	
	public VertexFormat(Element... elements) {
		this.elements = elements;
		for(Element e : elements){
			bytes_per_vertex += e.byteCount;
		}
	}
	
	public void setState(){
		int i = 0;
		int offset = 0;
		for(Element e : elements){
			e.enable(i++, offset, bytes_per_vertex);
			offset += e.byteCount;
		}
	}
	
	public void unsetState(){
		for(int i = 0; i < elements.length; i ++){
			GL20.glDisableVertexAttribArray(i++);
		}
	}
	
	public static class Element {
		
		public int byteCount;
		public int type;
		public int size;
		boolean normalized;
		
		public Element(int type, int size, boolean normalized, int bytecount) {
			this.type = type;
			this.size = size;
			this.byteCount = bytecount;
			this.normalized = normalized;
		}
		
		public void enable(int index, int offset, int stride){
			GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset);
			GL20.glEnableVertexAttribArray(index);
		}
	}
	
}
