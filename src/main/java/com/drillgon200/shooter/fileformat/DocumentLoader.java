package com.drillgon200.shooter.fileformat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.drillgon200.shooter.Resources;

public class DocumentLoader {

	public static String readString(ByteBuffer buf){
		int len = buf.getInt();
		byte[] bytes = new byte[len];
		buf.get(bytes);
		return new String(bytes);
	}
	
	public static DocumentNode parseDocument(String path){
		ByteBuffer data = ByteBuffer.wrap(Resources.resourceToByteArray(path));
		data.order(ByteOrder.BIG_ENDIAN);
		byte[] magic = new byte[4];
		data.get(magic);
		if(!"DUCK".equals(new String(magic))){
			throw new RuntimeException("Bad level file magic");
		}
		byte[] end = new byte[3];
		DocumentNode root = new DocumentNode();
		List<DocumentNode> children = new ArrayList<>();
		while(data.remaining() >= 3){
			data.get(end);
			if("EOF".equals(new String(end)))
				break;
			data.position(data.position()-3);
			
			DocumentNode node = parseNode(data);
			children.add(node);
		}
		root.children = children.toArray(new DocumentNode[children.size()]);
		return root;
	}
	
	public static DocumentNode parseNode(ByteBuffer data){
		DocumentNode node = new DocumentNode();
		node.name = readString(data);
		int prevPos = data.position();
		int totalLength = data.getInt();
		int dataLength = data.getInt();
		List<DocumentData> subdata = new ArrayList<>();
		while(data.position()-prevPos < dataLength){
			DocumentData dat = new DocumentData();
			dat.name = readString(data);
			dat.data = new byte[data.getInt()];
			data.get(dat.data);
			subdata.add(dat);
		}
		List<DocumentNode> children = new ArrayList<>();
		while(data.position()-prevPos < totalLength){
			children.add(parseNode(data));
		}
		node.children = children.toArray(new DocumentNode[children.size()]);
		node.data = subdata.toArray(new DocumentData[subdata.size()]);
		return node;
	}
	
	public static class DocumentNode {
		public String name;
		public DocumentNode[] children;
		public DocumentData[] data;
		
		public DocumentNode getChild(String name){
			for(DocumentNode node : children){
				if(node.name.equals(name)){
					return node;
				}
			}
			return null;
		}
		
		public DocumentData getData(String name){
			for(DocumentData dat : data){
				if(dat.name.equals(name)){
					return dat;
				}
			}
			return null;
		}
	}
	
	public static class DocumentData {
		public String name;
		public byte[] data;
		
		public boolean getBoolean(){
			return data[0] != 0 ? true : false;
		}
		public byte getByte(){
			return data[0];
		}
		public short getShort(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			return buf.getShort();
		}
		public int getInt(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			return buf.getInt();
		}
		public long getLong(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			return buf.getLong();
		}
		public float getFloat(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			return buf.getFloat();
		}
		public double getDouble(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			return buf.getDouble();
		}
		public short[] getShortArray(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			short[] arr = new short[data.length/2];
			int i = 0;
			while(buf.remaining() > 0){
				arr[i++] = buf.getShort();
			}
			return arr;
		}
		public int[] getIntArray(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			int[] arr = new int[data.length/4];
			int i = 0;
			while(buf.remaining() > 0){
				arr[i++] = buf.getInt();
			}
			return arr;
		}
		public long[] getLongArray(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			long[] arr = new long[data.length/8];
			int i = 0;
			while(buf.remaining() > 0){
				arr[i++] = buf.getLong();
			}
			return arr;
		}
		public float[] getFloatArray(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			float[] arr = new float[data.length/4];
			int i = 0;
			while(buf.remaining() > 0){
				arr[i++] = buf.getFloat();
			}
			return arr;
		}
		public double[] getDoubleArray(){
			ByteBuffer buf = ByteBuffer.wrap(data);
			buf.order(ByteOrder.BIG_ENDIAN);
			double[] arr = new double[data.length/8];
			int i = 0;
			while(buf.remaining() > 0){
				arr[i++] = buf.getDouble();
			}
			return arr;
		}
	}
}
