#version 330 compatibility

layout (location = 0) in vec3 position;

void main(){
	gl_Position = gl_ModelViewProjectionMatrix * vec4(position, 1);
}