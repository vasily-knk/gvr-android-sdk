#version 300 es

uniform mat4 u_Model;
uniform mat4 u_MVP;
uniform mat4 u_MVMatrix;
uniform vec3 u_LightPos;

layout(location = 0) in vec4 a_Position;
layout(location = 1) in vec2 a_TexCoord;
layout(location = 2) in vec3 a_Normal;

out vec2 v_TexCoord;
out vec3 v_Color;

void main() {
   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);
   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

   float distance = length(u_LightPos - modelViewVertex);
   vec3 lightVector = normalize(u_LightPos - modelViewVertex);
   float diffuse = abs(a_Normal.x);//max(dot(modelViewNormal, lightVector), 0.5);

   v_Color = vec3(0.75,0.75,0.75) * diffuse * (1.0 / (1.0 + (0.00001 * distance * distance)));

   v_TexCoord = a_TexCoord;

   gl_Position = u_MVP * a_Position;
}
