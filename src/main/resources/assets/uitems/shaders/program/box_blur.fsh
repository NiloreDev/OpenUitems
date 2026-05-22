#version 330 core

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float RadiusMultiplier;

in vec2 TexCoords;
out vec4 FragColor;

void main() {
    vec2 texelSize = (1.0 / InSize) * BlurDir * RadiusMultiplier;
    vec4 sum = vec4(0.0);
    
    sum += texture(DiffuseSampler, TexCoords + texelSize * 0.0);
    sum += texture(DiffuseSampler, TexCoords + texelSize * 1.0);
    sum += texture(DiffuseSampler, TexCoords - texelSize * 1.0);
    sum += texture(DiffuseSampler, TexCoords + texelSize * 2.0);
    sum += texture(DiffuseSampler, TexCoords - texelSize * 2.0);

    FragColor = sum / 5.0;
}