#version 330 core

uniform mat4 transformationMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

uniform mat4 shadowTransformationViewMatrix;
uniform mat4 shadowProjectionMatrix;

in vec4 position;
in vec2 textureCoords;
in vec3 normal;

out vec2 pass_textureCoords;
out vec3 fragPosition;
out vec3 fragNormal;
out vec4 shadowPosition;


void main() {
    fragPosition = vec3(transformationMatrix * position);
    fragNormal = normal;

    gl_Position = projectionMatrix * viewMatrix * transformationMatrix * position;

    shadowPosition = shadowProjectionMatrix * shadowTransformationViewMatrix * position;

    pass_textureCoords = textureCoords;
}