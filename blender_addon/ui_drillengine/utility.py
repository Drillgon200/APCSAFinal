import struct

class ByteBuf(bytearray):
	def write(self, v):
		self.extend(struct.pack('>B', v))
		return self
	def writeInt(self, v):
		self.extend(struct.pack('>i', v))
		return self
	def writeString(self, v):
		bytes = v.encode('ascii');
		self.writeInt(len(bytes))
		self.extend(bytes)
		return self
	def writeFloat(self, v):
		self.extend(struct.pack('>f', v))
		return self
	def writeMatrix(self, v):
		for vec in v:
			for f in vec:
				self.writeFloat(f)
		return self

class Document:
	
	def __init__(self):
		self.children = []

	def toBuffer(self):
		buffer = ByteBuf()
		buffer.extend("DUCK".encode('ascii'))
		for child in self.children:
			buffer.extend(child.toBuffer())
		buffer.extend("EOF".encode('ascii'))
		return buffer
		
	
class DocumentNode:
	
	def __init__(self, name):
		self.children = []
		self.data = []
		self.name = name;
		
	def toBuffer(self):
		buffer = ByteBuf()
		buffer.writeString(self.name)
		data_buf = ByteBuf()
		child_buf = ByteBuf()
		for dat in self.data:
			data_buf.extend(dat.toBuffer())
		for child in self.children:
			child_buf.extend(child.toBuffer())
		buffer.writeInt(len(data_buf) + len(child_buf))
		buffer.writeInt(len(data_buf))
		buffer.extend(data_buf)
		buffer.extend(child_buf)
		return buffer
	
class DocumentData:

	def __init__(self, name, data):
		self.name = name;
		self.data = data;
		
	def toBuffer(self):
		buffer = ByteBuf()
		buffer.writeString(self.name)
		buffer.writeInt(len(self.data))
		buffer.extend(self.data)
		return buffer

def getChildren(obj): 
	children = [] 
	for obj2 in obj.children: 
		if obj2.parent == obj: 
			children.append(obj2) 
	return children