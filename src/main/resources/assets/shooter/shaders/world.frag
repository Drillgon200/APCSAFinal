#version 330 compatibility

uniform sampler2D tex;

in vec2 texCoord;

void main(){
	gl_FragColor = texture2D(tex, texCoord);
}