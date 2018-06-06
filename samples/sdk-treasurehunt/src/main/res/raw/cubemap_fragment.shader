#version 300 es

precision mediump float;

in vec2 v_UV;

out vec4 FragColor;

uniform sampler2D tex;

void main() {
    vec3 texColor = texture(tex, v_UV).rgb;
    FragColor = vec4(texColor, 1);
}
