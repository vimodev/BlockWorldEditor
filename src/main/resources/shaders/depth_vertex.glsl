#version 330 core

in vec4 position;
in vec2 textureCoords;
in vec3 normal;

uniform mat4 shadowTransformationViewMatrix;
uniform mat4 shadowProjectionMatrix;

void main()
{
    gl_Position = shadowProjectionMatrix * shadowTransformationViewMatrix * position;
}