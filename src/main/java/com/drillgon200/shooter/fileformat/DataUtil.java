package com.drillgon200.shooter.fileformat;

import static com.drillgon200.shooter.fileformat.DocumentLoader.readString;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.drillgon200.shooter.fileformat.DocumentLoader.DocumentData;
import com.drillgon200.shooter.render.Material;
import com.drillgon200.shooter.util.Vec4f;

public class DataUtil {

	public static Material readMaterial(DocumentData mat){
		ByteBuffer buf = ByteBuffer.wrap(mat.data);
		buf.order(ByteOrder.BIG_ENDIAN);
		String color = "level/" + readString(buf);
		String metallic = "level/" + readString(buf);
		String normal = "level/" + readString(buf);
		float spec = buf.getFloat();
		float rough = buf.getFloat();
		float ior = buf.getFloat();
		Vec4f emission = new Vec4f(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
		
		Material material = new Material(color, metallic, normal, spec, rough, ior, emission);
		material.name = mat.name;
		return material;
	}

}
