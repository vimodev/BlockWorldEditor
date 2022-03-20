import org.joml.Vector3f;
import org.json.JSONObject;

public class Light {

    // Position of the light, in the case of a directional light, this is the direction
    public Vector3f position;

    // Light attenuation, irrelevant (read: not used) for direction light
    // Default: distance=50 (https://wiki.ogre3d.org/tiki-index.php?page=-Point+Light+Attenuation)
    public float constant = 1.0f;
    public float linear = 0.09f;
    public float quadratic = 0.032f;

    // Light properties
    public Vector3f ambient = new Vector3f(0.5f, 0.5f, 0.5f);
    public Vector3f diffuse = new Vector3f(0.5f, 0.5f, 0.5f);
    public Vector3f specular = new Vector3f(0.2f, 0.2f, 0.2f);

    public Light(Vector3f position) {
        this.position = position;
    }

    public Light(Vector3f position, Vector3f ambient, Vector3f diffuse, Vector3f specular) {
        this.position = position;
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
    }

    public Light(Vector3f position, Vector3f ambient, Vector3f diffuse, Vector3f specular, float constant, float linear, float quadratic) {
        this.position = position;
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.constant = constant;
        this.linear = linear;
        this.quadratic = quadratic;
    }

    public JSONObject toJSON() {
        JSONObject lightJSON = new JSONObject();
        JSONObject lightPosition = new JSONObject();
        lightPosition.put("x", position.x);
        lightPosition.put("y", position.y);
        lightPosition.put("z", position.z);
        lightJSON.put("position", lightPosition);
        JSONObject lightAmbient = new JSONObject();
        lightAmbient.put("r", ambient.x);
        lightAmbient.put("g", ambient.y);
        lightAmbient.put("b", ambient.z);
        lightJSON.put("ambient", lightAmbient);
        JSONObject lightDiffuse = new JSONObject();
        lightDiffuse.put("r", diffuse.x);
        lightDiffuse.put("g", diffuse.y);
        lightDiffuse.put("b", diffuse.z);
        lightJSON.put("diffuse", lightDiffuse);
        JSONObject lightSpecular = new JSONObject();
        lightSpecular.put("r", specular.x);
        lightSpecular.put("g", specular.y);
        lightSpecular.put("b", specular.z);
        lightJSON.put("specular", lightSpecular);
        return lightJSON;
    }

    public void addToShaderAsDirLight(Shader shader) {
        shader.setUniform("dirLight.direction", position);
        shader.setUniform("dirLight.ambient", ambient);
        shader.setUniform("dirLight.diffuse", diffuse);
        shader.setUniform("dirLight.specular", specular);
    }

    public void addToShaderAsPointLight(Shader shader, int i) {
        shader.setUniform("pointLights[" + i + "].position", position);
        shader.setUniform("pointLights[" + i + "].ambient", ambient);
        shader.setUniform("pointLights[" + i + "].diffuse", diffuse);
        shader.setUniform("pointLights[" + i + "].specular", specular);
        shader.setUniform("pointLights[" + i + "].constant", constant);
        shader.setUniform("pointLights[" + i + "].linear", linear);
        shader.setUniform("pointLights[" + i + "].quadratic", quadratic);
    }

    public static void clearShaderOfPointLights(Shader shader, int limit) {
        for (int i = 0; i < limit; i++) {
            shader.setUniform("pointLights[" + i + "].diffuse", new Vector3f(0.0f, 0.0f, 0.0f));
        }
    }
}
