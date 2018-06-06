#version 300 es

uniform mat4 u_Model;
uniform mat4 u_MVP;
uniform mat4 u_MVMatrix;
uniform vec3 u_LightPos;

layout(location = 0) in vec4 a_Position;
layout(location = 1) in vec4 a_Color;
layout(location = 2) in vec3 a_Normal;
layout(location = 3) in vec2 a_UV;

out vec4 v_Color;
out vec3 v_Grid;
out vec2 v_UV;

void main() {
   v_Grid = vec3(u_Model * a_Position);

   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);
   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

   float distance = length(u_LightPos - modelViewVertex);
   vec3 lightVector = normalize(u_LightPos - modelViewVertex);
   float diffuse = max(dot(modelViewNormal, lightVector), 0.5);

   diffuse = diffuse * (1.0 / (1.0 + (0.00001 * distance * distance)));
   v_Color = vec4(a_Color.rgb * diffuse, a_Color.a);

   v_UV = a_UV;

   gl_Position = u_MVP * a_Position;
}
