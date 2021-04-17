#version 330 compatibility

layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 texCoord;

out vec2 pass_texCoord;

void main(){
	pass_texCoord = texCoord;
	gl_Position = gl_ModelViewProjectionMatrix * vec4(pos, 1);
}