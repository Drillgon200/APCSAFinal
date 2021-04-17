package com.drillgon200.shooter.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import com.drillgon200.physics.AxisAlignedBB;
import com.drillgon200.physics.TreeConsumer;
import com.drillgon200.shooter.Shooter;
import com.drillgon200.shooter.util.GLAllocation;
import com.drillgon200.shooter.util.Matrix3f;
import com.drillgon200.shooter.util.Matrix4f;
import com.drillgon200.shooter.util.Vec3f;
import com.drillgon200.shooter.util.Vec4f;

public class ClusterManager {

	public static final int HEIGHT_SLICES = 8;
	public static final int WIDTH_SLICES = 16;
	public static final int DEPTH_SLICES = 24;
	
	public static final int MAX_LIGHTS = 512;
	public static final int AVG_ITEMS_PER_TILE = 5;
	public static final int BYTES_PER_ITEM = 2*2;
	public static final int BYTES_PER_CLUSTER = 4*4;
	public static final int ITEM_BUFFER_SIZE = BYTES_PER_ITEM*AVG_ITEMS_PER_TILE*HEIGHT_SLICES*WIDTH_SLICES*DEPTH_SLICES;
	
	public static ByteBuffer lightBuffer;
	
	//For depth sub division
	public static float scale;
	public static float bias;
	
	public static Cluster[] clusters;
	
	public static int clusterTexture;
	public static int clusterTBO;
	public static int indexTexture;
	public static int indexTBO;
	public static int lightTexture;
	public static int lightTBO;
	
	public static void init(){
		lightBuffer = GLAllocation.createDirectByteBuffer(ITEM_BUFFER_SIZE);
		clusterTBO = GL30.glGenBuffers();
		clusterTexture = GL11.glGenTextures();
		indexTBO = GL30.glGenBuffers();
		indexTexture = GL11.glGenTextures();
		lightTBO = GL30.glGenBuffers();
		lightTexture = GL11.glGenTextures();
		
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, clusterTBO);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, clusterTexture);
		GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32I, clusterTBO);

		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, indexTBO);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, indexTexture);
		GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RG16UI, indexTBO);
		
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, lightTBO);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, lightTexture);
		GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, lightTBO);
		
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
		
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, lightTBO);
		lightBuffer.limit(MAX_LIGHTS*Light.BYTES_PER_LIGHT);
		GL30.glBufferData(GL31.GL_TEXTURE_BUFFER, lightBuffer, GL30.GL_STREAM_DRAW);
		
		lightBuffer.limit(lightBuffer.capacity());
		
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, indexTBO);
		GL30.glBufferData(GL31.GL_TEXTURE_BUFFER, lightBuffer, GL30.GL_STREAM_DRAW);
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
	}
	
	public static void sendUniforms(int shader, int textureOffset){
		GL13.glActiveTexture(textureOffset);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, clusterTexture);
		GL13.glActiveTexture(textureOffset+1);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, indexTexture);
		GL13.glActiveTexture(textureOffset+2);
		GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, lightTexture);
		
		
		textureOffset -= GL14.GL_TEXTURE0;
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "clusters"), textureOffset);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "index_list"), textureOffset+1);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "light_list"), textureOffset+2);
		
		GL20.glUniform1f(GL20.glGetUniformLocation(shader, "scale"), scale);
		GL20.glUniform1f(GL20.glGetUniformLocation(shader, "bias"), bias);
		
		GL20.glUniform1f(GL20.glGetUniformLocation(shader, "zNear"), Shooter.zNear);
		GL20.glUniform1f(GL20.glGetUniformLocation(shader, "zFar"), Shooter.zFar);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}
	
	public static void updateLightLists(){
		List<Light> staticLights = new ArrayList<>();
		List<Light> dynamicLights = new ArrayList<>();
		Vec3f offset = Shooter.viewMatrix.getTranslation();
		Vec3f offsetInv = Shooter.inverseViewMatrix.getTranslation();
		Matrix3f view = Shooter.viewMatrix.toMat3();
		Matrix3f viewInv = Shooter.inverseViewMatrix.toMat3();
		int globalIndexCount = 0;
		for(Cluster c : clusters){
			Shooter.world.level.staticLights.forEach(new LightGatherer(staticLights, c.box, view, viewInv, offset, offsetInv));
			Shooter.world.lights.forEach(new LightGatherer(dynamicLights, c.box, view, viewInv, offset, offsetInv));
			c.offset = globalIndexCount;
			c.staticLights = staticLights.size();
			c.dynamicLights = dynamicLights.size();
			if((globalIndexCount + Math.max(staticLights.size(), dynamicLights.size()))*BYTES_PER_ITEM > ITEM_BUFFER_SIZE){
				break;
			}
			for(int i = 0; i < staticLights.size(); i ++){
				lightBuffer.putShort((globalIndexCount + i)*BYTES_PER_ITEM + 0, (short) staticLights.get(i).getIndex());
			}
			for(int i = 0; i < dynamicLights.size(); i ++){
				lightBuffer.putShort((globalIndexCount + i)*BYTES_PER_ITEM + 2, (short) dynamicLights.get(i).getIndex());
			}
			globalIndexCount += Math.max(staticLights.size(), dynamicLights.size());
			
			staticLights.clear();
			dynamicLights.clear();
		}
		//Upload indices
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, indexTBO);
		//4 ints, 4 bytes per int
		lightBuffer.limit(globalIndexCount*BYTES_PER_ITEM);
		lightBuffer.rewind();
		GL30.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, lightBuffer);
		//Upload clusters
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, clusterTBO);
		lightBuffer.limit(clusters.length*BYTES_PER_CLUSTER);
		for(Cluster c : clusters){
			lightBuffer.putInt(c.offset);
			lightBuffer.putInt(c.staticLights);
			lightBuffer.putInt(c.dynamicLights);
			lightBuffer.putInt(c.padding);
		}
		lightBuffer.rewind();
		GL30.glBufferSubData(GL31.GL_TEXTURE_BUFFER, 0, lightBuffer);
		lightBuffer.limit(lightBuffer.capacity());
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
	}
	
	public static void rebuildClusters(float near, float far, Matrix4f projection){
		clusters = new Cluster[HEIGHT_SLICES*WIDTH_SLICES*DEPTH_SLICES];
		scale = DEPTH_SLICES/(float)Math.log(far/near);
		bias = (DEPTH_SLICES*(float)Math.log(near))/(float)Math.log(far/near);
		
		Matrix4f inv_projection = projection.copy().invert();
		
		for(int i = 0; i < DEPTH_SLICES; i ++){
			float tileNear = -near * (float)Math.pow(far/near, (float)i/DEPTH_SLICES);
			float tileFar = -near * (float)Math.pow(far/near, (float)(i+1)/DEPTH_SLICES);
			for(int j = 0; j < WIDTH_SLICES; j ++){
				float ndcXMin = (float)j/WIDTH_SLICES;
				ndcXMin = ndcXMin*2-1;
				float ndcXMax = (float)(j+1)/WIDTH_SLICES;
				ndcXMax = ndcXMax*2-1;
				for(int k = 0; k < HEIGHT_SLICES; k ++){
					int index = i*WIDTH_SLICES*HEIGHT_SLICES + j*HEIGHT_SLICES + k;
					
					float ndcYMin = (float)k/HEIGHT_SLICES;
					ndcYMin = ndcYMin*2-1;
					float ndcYMax = (float)(k+1)/HEIGHT_SLICES;
					ndcYMax = ndcYMax*2-1;
					
					Vec3f minViewspace = ndcToView(inv_projection, new Vec3f(ndcXMin, ndcYMin, -1));
					Vec3f maxViewspace = ndcToView(inv_projection, new Vec3f(ndcXMax, ndcYMax, -1));
					
					Vec3f minNear = intersectZPlane(minViewspace, tileNear);
					Vec3f minFar = intersectZPlane(minViewspace, tileFar);
					Vec3f maxNear = intersectZPlane(maxViewspace, tileNear);
					Vec3f maxFar = intersectZPlane(maxViewspace, tileFar);
					
					Vec3f minAABB = minNear.min(minFar).min(maxNear).min(maxFar);
					Vec3f maxAABB = minNear.max(minFar).max(maxNear).max(maxFar);
					
					clusters[index] = new Cluster(new AxisAlignedBB(minAABB, maxAABB));
				}
			}
		}
		//Allocate buffer here so we don't continuously reallocate it.
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, clusterTBO);
		lightBuffer.limit(clusters.length*BYTES_PER_CLUSTER);
		GL30.glBufferData(GL31.GL_TEXTURE_BUFFER, lightBuffer, GL30.GL_STREAM_DRAW);
		GL30.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
	}
	
	public static Vec3f ndcToView(Matrix4f inv_projection, Vec3f ndc){
		Vec4f vec = inv_projection.transform(new Vec4f(ndc, 1));
		return new Vec3f(vec.x/vec.w, vec.y/vec.w, vec.z/vec.w);
	}
	
	public static Vec3f intersectZPlane(Vec3f direction, float z){
		float t = z/direction.z;
		return direction.scale(t);
	}
	
	public static class Cluster {
		
		public int offset;
		public int staticLights;
		public int dynamicLights;
		public int padding = 0;
		
		public AxisAlignedBB box;
		
		public Cluster(AxisAlignedBB box) {
			this.box = box;
		}
	}
	
	private static class LightGatherer implements TreeConsumer<Light> {

		private Collection<Light> toAdd;
		private AxisAlignedBB box;
		//Use a sphere surrounding the box for intersection testing because that's easier to deal with when we're rotating.
		private Vec3f spherePos;
		private float radiusSq;
		private Matrix3f rotation;
		private Vec3f offset;
		
		public LightGatherer(Collection<Light> toAdd, AxisAlignedBB box, Matrix3f rotation, Matrix3f rotationInv, Vec3f offset, Vec3f offsetInv) {
			this.toAdd = toAdd;
			this.box = box;
			this.rotation = rotation;
			this.offset = offset;
			spherePos = rotationInv.transform(box.getCenter()).mutateAdd(offsetInv);
			Vec3f diff = new Vec3f((box.maxX-box.minX)*0.5F, (box.maxY-box.minY)*0.5F, (box.maxZ-box.minZ)*0.5F);
			radiusSq = diff.lenSq();
		}
		
		@Override
		public void accept(Light light) {
			Vec3f pos = rotation.transform(light.getPos()).mutateAdd(offset);
			if(box.intersectsSphereSq(pos, light.getRadius()*light.getRadius()))
				toAdd.add(light);
		}

		@Override
		public boolean shouldContinue(AxisAlignedBB box) {
			return box.intersectsSphereSq(spherePos, radiusSq);
		}
		
	}
}
