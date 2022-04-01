#version 330

in vec2 outTextureCoords;
in vec3 mvPos;
out vec4 fragColor;

uniform sampler2D texture_sampler;
uniform float fullShadow;

void main()
{
    vec3 t = texture(texture_sampler, outTextureCoords).xyz;

    if (t.x > 0.9 && t.y > 0.9) {
        fragColor = vec4(t, 1.0);
    } else {
        fragColor = vec4(min(fullShadow+0.1, 1.0) * t, 1.0);
    }
}