package com.drillgon200.shooter.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import com.drillgon200.shooter.util.ShaderManager.Shader.Uniform;

public class ShaderManager {
	
	public static final FloatBuffer AUX_GL_BUFFER = GLAllocation.createDirectFloatBuffer(16);
	private static final List<Shader> ALL_SHADERS = new ArrayList<>();
	private static final Map<String, String> cachedShaders = new HashMap<>();
	private static int active_shader = 0;
	
	public static final Uniform MODELVIEW_PROJECTION_MATRIX = shader -> {
		//No idea if all these rewind calls are necessary. I'll have to check that later.
		GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, AUX_GL_BUFFER);
		AUX_GL_BUFFER.rewind();
		Matrix4f mvMatrix = new Matrix4f();
		mvMatrix.load(AUX_GL_BUFFER);
		AUX_GL_BUFFER.rewind();
		
		GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, AUX_GL_BUFFER);
		AUX_GL_BUFFER.rewind();
		Matrix4f pMatrix = new Matrix4f();
		pMatrix.load(AUX_GL_BUFFER);
		AUX_GL_BUFFER.rewind();
		
		Matrix4f.mul(pMatrix, mvMatrix, mvMatrix).store(AUX_GL_BUFFER);
		AUX_GL_BUFFER.rewind();
		
		GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(shader, "modelViewProjectionMatrix"), false, AUX_GL_BUFFER);
	};
	
	public static final Uniform MODELVIEW_MATRIX = shader -> {
		GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, AUX_GL_BUFFER);
		AUX_GL_BUFFER.rewind();
		GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(shader, "modelview"), false, AUX_GL_BUFFER);
	};
	
	public static final Uniform PROJECTION_MATRIX = shader -> {
		GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, AUX_GL_BUFFER);
		AUX_GL_BUFFER.rewind();
		GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(shader, "projection"), false, AUX_GL_BUFFER);
	};

	//Later
	/*
	public static final int bloomLayers = 4;
	public static int height = 0;
    public static int width = 0;
    public static Framebuffer[] bloomBuffers;
    public static Framebuffer bloomData;
    
	public static void bloom(){
		if(true)
			return;
		if(height != Shooter.displayHeight || width != Minecraft.getMinecraft().displayWidth){
			height = Shooter.displayHeight;
            width = Shooter.displayWidth;
            recreateFBOs();
        }
		downsampleBloomData();
		GlStateManager.enableBlend();
		for(int i = bloomLayers-1; i >= 0; i --){
			GlStateManager.blendFunc(SourceFactor.ONE, DestFactor.ZERO);
			bloomBuffers[i*2+1].bindFramebuffer(true);
			ResourceManager.bloom_h.use();
			GL20.glUniform1f(GL20.glGetUniformLocation(ResourceManager.bloom_h.getShaderId(), "frag_width"), 1F/(float)bloomBuffers[i*2].framebufferWidth);
			renderFboTriangle(bloomBuffers[i*2], bloomBuffers[i*2+1].framebufferWidth, bloomBuffers[i*2+1].framebufferHeight);
			
			
			GlStateManager.blendFunc(SourceFactor.ONE, DestFactor.ONE);
			int tWidth, tHeight;
			if(i == 0){
				Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
				tWidth = Minecraft.getMinecraft().getFramebuffer().framebufferWidth;
				tHeight = Minecraft.getMinecraft().getFramebuffer().framebufferHeight;
			} else {
				GlStateManager.glBlendEquation(GL14.GL_MAX);
				bloomBuffers[(i-1)*2].bindFramebuffer(true);
				tWidth = bloomBuffers[(i-1)*2].framebufferWidth;
				tHeight = bloomBuffers[(i-1)*2].framebufferHeight;
			}
			ResourceManager.bloom_v.use();
			GL20.glUniform1f(GL20.glGetUniformLocation(ResourceManager.bloom_v.getShaderId(), "frag_height"), 1F/(float)bloomBuffers[i*2].framebufferHeight);
			renderFboTriangle(bloomBuffers[i*2+1], tWidth, tHeight);
			GlStateManager.glBlendEquation(GL14.GL_FUNC_ADD);
		}
		releaseShader();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableBlend();
		bloomData.framebufferClear();
		Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
		
		GlStateManager.enableAlpha();
		GlStateManager.enableLighting();
		GlStateManager.enableDepth();
	}
	
	public static void downsampleBloomData(){
		bloomBuffers[0].bindFramebuffer(true);
		ResourceManager.downsample.use();
		GL20.glUniform2f(GL20.glGetUniformLocation(ResourceManager.downsample.getShaderId(), "texel"), 1F/(float)bloomData.framebufferTextureWidth, 1F/(float)bloomData.framebufferTextureHeight);
		renderFboTriangle(bloomData, bloomBuffers[0].framebufferWidth, bloomBuffers[0].framebufferHeight);
		for(int i = 1; i < bloomLayers; i ++){
			bloomBuffers[i*2].bindFramebuffer(true);
			GL20.glUniform2f(GL20.glGetUniformLocation(ResourceManager.downsample.getShaderId(), "texel"), 1F/(float)bloomBuffers[(i-1)*2].framebufferTextureWidth, 1F/(float)bloomBuffers[(i-1)*2].framebufferTextureHeight);
			renderFboTriangle(bloomBuffers[(i-1)*2], bloomBuffers[i*2].framebufferWidth, bloomBuffers[i*2].framebufferHeight);
		}
		releaseShader();
	}
	
	public static void recreateFBOs(){
		if(bloomBuffers != null)
			for(Framebuffer buf : bloomBuffers){
				buf.deleteFramebuffer();
			}
		if(bloomData != null)
			bloomData.deleteFramebuffer();
		bloomData = new Framebuffer(width, height, true);
		bloomData.bindFramebufferTexture();
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_SHORT, (IntBuffer)null);
		bloomData.bindFramebuffer(false);
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, Minecraft.getMinecraft().getFramebuffer().depthBuffer);
		OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT, OpenGlHelper.GL_RENDERBUFFER, Minecraft.getMinecraft().getFramebuffer().depthBuffer);
		bloomData.setFramebufferFilter(GL11.GL_LINEAR);
		bloomData.setFramebufferColor(0, 0, 0, 0);
		bloomData.framebufferClear();
		bloomBuffers = new Framebuffer[bloomLayers*2];
		float bloomW = width;
		float bloomH = height;
		for(int i = 0; i < bloomLayers; i ++){
			
			bloomBuffers[i*2] = new Framebuffer((int)bloomW, (int)bloomH, false);
			bloomBuffers[i*2+1] = new Framebuffer((int)bloomW, (int)bloomH, false);
			bloomBuffers[i*2].bindFramebufferTexture();
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, (int)bloomW, (int)bloomH, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_SHORT, (IntBuffer)null);
			bloomBuffers[i*2+1].bindFramebufferTexture();
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, (int)bloomW, (int)bloomH, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_SHORT, (IntBuffer)null);
			bloomBuffers[i*2].setFramebufferFilter(GL11.GL_LINEAR);
			bloomBuffers[i*2+1].setFramebufferFilter(GL11.GL_LINEAR);
			bloomBuffers[i*2].setFramebufferColor(0, 0, 0, 0);
			bloomBuffers[i*2+1].setFramebufferColor(0, 0, 0, 0);
			if(i < 2){
				bloomW *= 0.25F;
				bloomH *= 0.25F;
			} else {
				bloomW *= 0.5F;
				bloomH *= 0.5F;
			}
		}
	}
	*/
    
   /* public static void doPostProcess(){
        if(height != Minecraft.getMinecraft().displayHeight || width != Minecraft.getMinecraft().displayWidth){
            recreateFBOs();
            height = Minecraft.getMinecraft().displayHeight;
            width = Minecraft.getMinecraft().displayWidth;
        }
        GL11.glPushMatrix();
        
        buf.bindFramebuffer(false);
        
        ResourceManager.testlut.use();
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE3);
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.lut);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUniform1i(GL20.glGetUniformLocation(ResourceManager.testlut.getShaderId(), "tempTest"), 3);
        
        Minecraft.getMinecraft().getFramebuffer().framebufferRender(buf.framebufferWidth, buf.framebufferHeight);
        
        HbmShaderManager2.releaseShader();
        
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, buf.framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, Minecraft.getMinecraft().getFramebuffer().framebufferObject);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
       
        GL11.glPopMatrix();
        GlStateManager.enableDepth();
    }
    
    public static void recreateFBOs(){
        if(buf != null)
            buf.deleteFramebuffer();
        buf = new Framebuffer(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight, false);
    }*/
	
    public static Shader loadShader(String file) {
    	return loadShader(file, false);
    }
    
    public static Shader loadShader(String file, String ext) {
    	return loadShader(file, false, ext);
    }
    
    public static Shader loadShader(String file, boolean hasGeoShader) {
    	return loadShader(file, hasGeoShader, null);
    }
    
	public static Shader loadShader(String file, boolean hasGeoShader, String extension) {
		int vertexShader = 0;
		int fragmentShader = 0;
		int geometryShader = 0;
		try {
			int program = GL20.glCreateProgram();
			
			vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
			GL20.glShaderSource(vertexShader, preprocess(readFileToString(file + ".vert"), extension, ".vext"));
			GL20.glCompileShader(vertexShader);
			if(GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				System.err.println(GL20.glGetShaderInfoLog(vertexShader, GL20.GL_INFO_LOG_LENGTH));
				throw new RuntimeException("Error creating vertex shader: " + file);
			}
			
			fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
			GL20.glShaderSource(fragmentShader, preprocess(readFileToString(file + ".frag"), extension, ".fext"));
			GL20.glCompileShader(fragmentShader);
			if(GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				System.err.println(GL20.glGetShaderInfoLog(fragmentShader, GL20.GL_INFO_LOG_LENGTH));
				throw new RuntimeException("Error creating fragment shader: " + file);
			}
			
			if(hasGeoShader){
				geometryShader = GL20.glCreateShader(GL32.GL_GEOMETRY_SHADER);
				GL20.glShaderSource(geometryShader, preprocess(readFileToString(file + ".geo"), extension, ".gext"));
				GL20.glCompileShader(geometryShader);
				if(GL20.glGetShaderi(geometryShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
					System.err.println(GL20.glGetShaderInfoLog(geometryShader, GL20.GL_INFO_LOG_LENGTH));
					throw new RuntimeException("Error creating geometry shader: " + file);
				}
			}
			
			GL20.glAttachShader(program, vertexShader);
			GL20.glAttachShader(program, fragmentShader);
			if(hasGeoShader){
				GL20.glAttachShader(program, geometryShader);
			}
			GL20.glLinkProgram(program);
			if(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				System.err.println(GL20.glGetProgramInfoLog(program, GL20.GL_INFO_LOG_LENGTH));
				throw new RuntimeException("Error linking shader: " + file);
			}
			
			GL20.glDeleteShader(vertexShader);
			GL20.glDeleteShader(fragmentShader);
			if(hasGeoShader){
				GL20.glDeleteShader(geometryShader);
			}
			Shader shader = new Shader(program);
			ALL_SHADERS.add(shader);
			return shader;
		} catch(Exception x) {
			GL20.glDeleteShader(vertexShader);
			GL20.glDeleteShader(fragmentShader);
			if(hasGeoShader){
				GL20.glDeleteShader(geometryShader);
			}
			x.printStackTrace();
		}
		return new Shader(0);
	}
	
	public static void deleteShaders(){
		for(Shader s : ALL_SHADERS){
			GL20.glDeleteProgram(s.shader);
			s.shader = 0;
		}
		ALL_SHADERS.clear();
	}

	//Adds any imported functions or replaces functions from the extension shader
	private static String preprocess(String shader, String extension, String extType) throws IOException {
		StringBuilder build = new StringBuilder();
		int lenImport = "import ".length();
		int lenExtStart = "extension_start:".length();
		int lenExtEnd = "extension_end:".length();
		
		for(int i = 0; i < shader.length(); i ++){
			if(i < shader.length()-lenImport){
				if(shader.substring(i, i+lenImport).equals("import ")){
					int endIdx = i+lenImport;
					for(int j = i+lenImport; j < shader.length(); j ++){
						if(shader.charAt(j) == ';'){
							endIdx = j;
							break;
						}
					}
					String name = shader.substring(i+lenImport, endIdx);
					String file = readFileToString("/assets/shooter/shaders/" + name + ".ext");
					build.append(file);
					i = endIdx;
				}
			}
			
			String extFile = readFileToString("/assets/shooter/shaders/" + extension + extType);
			if(extension != null && extFile != null){
				if(i < shader.length()-lenExtStart && shader.substring(i, i+lenExtStart).equals("extension_start:")){
					int endIdx = i+lenExtStart;
					for(int j = i+lenExtStart; j < shader.length(); j ++){
						if(shader.substring(j, j+lenExtEnd).equals("extension_end:")){
							endIdx = j;
							break;
						}
					}
					build.append(extFile);
					i = endIdx;
				}
			} else {
				if(i < shader.length()-lenExtStart && shader.substring(i, i+lenExtStart).equals("extension_start:")){
					i += lenExtStart;
				}
				if(i < shader.length()-lenExtEnd && shader.substring(i, i+lenExtEnd).equals("extension_end:")){
					i += lenExtEnd;
				}
			}
			
			build.append(shader.charAt(i));
		}
		
		return build.toString();
	}
	
	private static String readFileToString(String file) throws IOException {
		InputStream in = ShaderManager.class.getResourceAsStream(file);
		if(in == null)
			return null;
		byte[] bytes = new byte[in.available()];
		in.read(bytes);
		in.close();
		String strFile = new String(bytes);
		cachedShaders.put(file, strFile);
		return strFile;
	}
	
	public static void releaseShader(){
		active_shader = 0;
		GL20.glUseProgram(0);
	}
	
	public static void color(float r, float g, float b, float a){
		GL20.glUniform4f(GL20.glGetUniformLocation(active_shader, "color"), r, g, b, a);
	}
	
	public static class Shader {

		private int shader;
		private List<Uniform> uniforms = new ArrayList<>(2);
		
		public Shader(int shader) {
			this.shader = shader;
		}
		
		public Shader withUniforms(Uniform... uniforms){
			for(Uniform u : uniforms){
				this.uniforms.add(u);
			}
			return this;
		}
		
		public void use(){
			if(active_shader == shader)
				return;
			active_shader = shader;
			GL20.glUseProgram(shader);
			for(Uniform u : uniforms){
				u.apply(shader);
			}
		}
		
		public int getShaderId(){
			return shader;
		}
		
		public static interface Uniform {
			public void apply(int shader);
		}
	}
}
