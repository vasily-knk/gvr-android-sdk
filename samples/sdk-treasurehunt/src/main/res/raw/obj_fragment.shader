#version 300 es

precision mediump float;

in vec3 v_Color;

out vec4 FragColor;

uniform sampler2D tex;

void main() {
    FragColor = vec4(v_Color, 1);
}
