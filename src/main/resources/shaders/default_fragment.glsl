#version 330 core

uniform sampler2D textureSampler;

in vec2 pass_textureCoords;

out vec4 pixel_colour;

void main() {
//    pixel_colour = vec4(1, 0, 0, 1);
    pixel_colour = texture(textureSampler, pass_textureCoords);
}