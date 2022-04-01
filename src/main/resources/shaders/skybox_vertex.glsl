#version 330

in vec4 position;
in vec2 textureCoords;
in vec3 normal;

out vec2 outTextureCoords;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

void main()
{
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position.xyz, 1.0);
    outTextureCoords = textureCoords;
}