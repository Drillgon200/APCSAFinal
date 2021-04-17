import bpy
import bmesh
import struct
from .utility import *
from bpy_extras.io_utils import axis_conversion

rotate = axis_conversion(from_forward='-Y', 
        from_up='Z',
        to_forward='Z',
        to_up='Y').to_4x4()

def writeKeyframes(bone):
	first_matrix = None
	matrices = []
	should_write = False
	for i in range(bpy.context.scene.frame_start, bpy.context.scene.frame_end+1):
		bpy.context.scene.frame_set(i)
		matrix = bone.matrix
		if(bone.parent):
			matrix = bone.parent.matrix.inverted() @ bone.matrix
		matrices.append(matrix)
		if(first_matrix is None):
			first_matrix = bone.matrix_basis.copy()
		else:
			if(bone.matrix_basis != first_matrix):
				should_write = True
	if(should_write):
		buf = ByteBuf()
		for mat in matrices:
			buf.writeMatrix(mat)
		return buf
	else:
		return None

def save(path):
	bpy.context.scene.frame_set(bpy.context.scene.frame_start)

	out_file = open(path, 'wb')
	doc = Document()
	
	meta = DocumentNode("metadata")
	frame_count = (bpy.context.scene.frame_end-bpy.context.scene.frame_start)+1
	framerate = bpy.context.scene.render.fps
	length_millis = int((float(frame_count)/framerate)*1000.0)
	meta.data.append(DocumentData("frame_count", ByteBuf().writeInt(frame_count)))
	meta.data.append(DocumentData("framerate", ByteBuf().writeFloat(framerate)))
	meta.data.append(DocumentData("length_millis", ByteBuf().writeInt(length_millis)))
	doc.children.append(meta)
	
	anims = DocumentNode("anims")
	
	obj = bpy.context.active_object
	
	if(obj is not None and obj.type == 'ARMATURE' and obj.parent == None):
		for bone in obj.pose.bones:
			anim = writeKeyframes(bone)
			if(anim is not None):
				anims.data.append(DocumentData(bone.bone.name, anim))
		doc.children.append(anims)
	
	out_file.write(doc.toBuffer())
	out_file.close()
	return {'FINISHED'}