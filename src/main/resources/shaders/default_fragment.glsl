#version 330 core

struct DirLight {
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

uniform DirLight dirLight;
struct PointLight {
    vec3 position;

    float constant;
    float linear;
    float quadratic;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};
#define NR_POINT_LIGHTS 100
uniform PointLight pointLights[NR_POINT_LIGHTS];

uniform sampler2D textureSampler;
uniform vec3 viewPosition;
uniform float renderDistance;
uniform vec3 skyColor;
uniform sampler2D shadowMap;

in vec2 pass_textureCoords;
in vec3 fragPosition;
in vec3 fragNormal;
in vec4 shadowPosition;

out vec4 pixel_colour;

vec3 CalcDirLight(DirLight light, vec3 normal, vec3 viewDir, float shadow) {
    vec3 lightDir = normalize(light.direction);
    // diffuse shading
    float diff = max(dot(normal, lightDir), 0.0);
    // specular shading
    vec3 reflectDir = reflect(-lightDir, normal);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
    // combine results
    vec3 ambient  = light.ambient;
    vec3 diffuse  = light.diffuse  * diff;
    vec3 specular = light.specular * spec;
    return (ambient + diffuse*shadow + specular*shadow);
}

vec3 CalcPointLight(PointLight light, vec3 normal, vec3 fragPos, vec3 viewDir) {
    vec3 lightDir = normalize(light.position - fragPos);
    // diffuse shading
    float diff = max(dot(normal, lightDir), 0.0);
    // specular shading
    vec3 reflectDir = reflect(-lightDir, normal);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
    // attenuation
    float distance    = length(light.position - fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance +
    light.quadratic * (distance * distance));
    // combine results
    vec3 ambient  = light.ambient;
    vec3 diffuse  = light.diffuse  * diff;
    vec3 specular = light.specular * spec;
    ambient  *= attenuation;
    diffuse  *= attenuation;
    specular *= attenuation;
    return (ambient + diffuse + specular);
}

float calcShadow(vec4 position) {
    // Transform from screen coordinates to texture coordinates
    vec3 projCoords = position.xyz;
    projCoords = projCoords * 0.5 + 0.5;
    float bias = 0.001;

    float shadowFactor = 0.0;
    vec2 inc = 1.0 / textureSize(shadowMap, 0);
    for(int row = -1; row <= 1; ++row)
    {
        for(int col = -1; col <= 1; ++col)
        {
            float textDepth = texture(shadowMap, projCoords.xy + vec2(row, col) * inc).r;
            shadowFactor += projCoords.z - bias > textDepth ? 1.0 : 0.0;
        }
    }
    shadowFactor /= 9.0;

    return 1 - shadowFactor;
}

void main() {
    // properties
    vec3 norm = normalize(fragNormal);
    vec3 viewDir = normalize(viewPosition - fragPosition);

    // phase 1: Directional lighting
    float shadow = calcShadow(shadowPosition);
    vec3 lightColor = CalcDirLight(dirLight, norm, viewDir, shadow);
    // phase 2: Point lights
    for(int i = 0; i < NR_POINT_LIGHTS; i++) {
        // if diffuse is black, the lights are undefined, we have handled all lights!
        if (pointLights[i].diffuse == vec3(0.0,0.0,0.0)) break;
        lightColor += CalcPointLight(pointLights[i], norm, fragPosition, viewDir);
    }

    // get object color from texture
    vec4 objectColor = texture(textureSampler, pass_textureCoords);

    // compute full light
    pixel_colour = vec4(lightColor, 1.0) * objectColor;

    // Fog
    float gradient = 0.0225;
    float distance = length(fragPosition.xz - viewPosition.xz);
    float visibility = clamp(-gradient * (distance - renderDistance - 10), 0.0, 1.0);
    pixel_colour = mix(vec4(skyColor, 1.0), pixel_colour, visibility);
}