#version 330 core

uniform mat4 transformationMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

in vec4 position;

void main() {
//    gl_Position = projectionMatrix * viewMatrix * transformationMatrix * position;
    gl_Position = projectionMatrix * viewMatrix * transformationMatrix * position;
}