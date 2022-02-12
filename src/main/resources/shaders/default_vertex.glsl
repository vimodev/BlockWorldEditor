#version 330 core

uniform mat4 transformationMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

in vec4 position;
in vec2 textureCoords;
in vec3 normal;

out vec2 pass_textureCoords;
out vec3 fragPosition;
out vec3 fragNormal;


void main() {
    fragPosition = vec3(transformationMatrix * position);
    fragNormal = normal;

    gl_Position = projectionMatrix * viewMatrix * transformationMatrix * position;

    pass_textureCoords = textureCoords;
}