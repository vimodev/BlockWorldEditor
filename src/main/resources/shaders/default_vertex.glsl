#version 330 core

uniform mat4 transformationMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

in vec4 position;
in vec2 textureCoords;
in vec3 normal;

out vec2 pass_textureCoords;

void main() {
    gl_Position = projectionMatrix * viewMatrix * transformationMatrix * position;
    pass_textureCoords = textureCoords;
}