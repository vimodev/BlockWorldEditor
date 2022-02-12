#version 330 core

uniform sampler2D textureSampler;
uniform vec3 viewPosition;

in vec2 pass_textureCoords;
in vec3 fragPosition;
in vec3 fragNormal;

out vec4 pixel_colour;

void main() {
    // make Sun / light class and pass these from cpu
    float ambientStrength = 0.1;
    float specularStrength = 0.5;
    vec3 lightColor = vec3(1.0, 0.9, 0.8);
    vec3 lightPosition = vec3(0, 100, 0);

    vec4 objectColor = texture(textureSampler, pass_textureCoords);

    // compute ambient
    vec3 ambient = ambientStrength * lightColor;

    // compute diffuse
    vec3 norm = normalize(fragNormal);
    vec3 lightDirection = normalize(lightPosition - fragPosition);
    vec3 diffuse = max(dot(norm, lightDirection), 0.0) * lightColor;

    // compute specular
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    vec3 reflectDirection = reflect(-lightDirection, norm);
    vec3 specular = specularStrength * pow(max(dot(viewDirection, reflectDirection), 0.0), 32) * lightColor;

    // compute full light
    pixel_colour = vec4(ambient + diffuse + specular, 1.0) * objectColor;

}