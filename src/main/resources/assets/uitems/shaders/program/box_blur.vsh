#version 330 core

layout (location = 0) in vec3 Position;

uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;

out vec2 TexCoords;

void main() {
    vec4 outPos = ProjMat * vec4(Position, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);
    TexCoords = outPos.xy * 0.5 + 0.5;
}