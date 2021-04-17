#version 330 compatibility

uniform sampler2D texture;
uniform vec4 color;

in vec2 texCoord;

void main(){
	gl_FragColor = texture2D(texture, texCoord) * color;
}