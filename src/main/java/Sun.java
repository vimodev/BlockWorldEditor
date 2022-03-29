import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Class describing the sun (directional light), its dynamic movement, and the functions needed for shadow mapping
 */
public class Sun {

    private World world;
    private Light directionalLight;


    private float dawn = 400;
    private float sunrise = 800;
    private float sunset = 1600;
    private float dusk = 2000;

    // Should be positioned right above player to prevent issues with shadow mapping
    private Vector3f position;

    private Matrix4f projection;

    private ShadowMap shadowMap;

    public Sun(World world) {
        this.world = world;
        this.directionalLight = new Light(
                new Vector3f(0f, 0.5f, 0.5f),
                new Vector3f(0.2f, 0.2f, 0.2f),
                new Vector3f(0.8f, 0.7f, 0.7f),
                new Vector3f(0.1f, 0.1f, 0.1f)
        );

        this.shadowMap = new ShadowMap();

        this.position = new Vector3f();

        this.projection = new Matrix4f();
        this.projection.identity();
        this.projection.setOrtho(-1000.0f, 1000.0f, -1000.0f, 1000.00f, -1.0f, 5000.0f);
    }

    private static float map(float a0, float a1, float b0, float b1, float x) {
        float a_range = (a1 - a0);
        float b_range = (b1 - b0);
        if (x <= a0) return b0;
        if (x >= a1) return b1;
        return (((x - a0) * b_range) / a_range) + b0;
    }

    public void setTime(float time, Vector3f position) {
        float y = map(dawn, dusk, 1.5f, 2.5f, time);
        this.directionalLight.position = new Vector3f(0f, 1f, 0f);
        this.directionalLight.position.rotateX(y * (float) Math.PI);
        this.directionalLight.position.normalize();

        this.position = new Vector3f(0, 100f, 0);
        this.position.add(position);
    }

    public float getTimeMultiplier(float time) {
        if (time > sunrise && time < sunset) {
            return 1.0f;
        }
        if (time <= sunrise) {
            return (float) Math.pow((time/sunrise), 5);
        }
        if (time >= sunset) {
            return (float) Math.pow(((2400-time)/sunrise), 5);
        }
        return time;
    }

    public Light getLight() {
        return this.directionalLight;
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public Vector3f getDirection() {
        return this.directionalLight.position;
    }

    public Matrix4f getProjection() {
        return this.projection;
    }
    public ShadowMap getShadowMap() {
        return this.shadowMap;
    }

    public Matrix4f getTransformation() {
        // Set matrix to id
        Matrix4f M = new Matrix4f();
        M.identity();
        // Apply transformations
        // First do the rotation so camera rotates over its position
        float lightAngleX = (float)Math.toDegrees(Math.acos(directionalLight.position.z));
        float lightAngleY = (float)Math.toDegrees(Math.asin(directionalLight.position.x));
        M.rotate((float)Math.toRadians(lightAngleX), new Vector3f(1, 0, 0));
        M.rotate((float)Math.toRadians(lightAngleY), new Vector3f(0, 1, 0));
        // Then do the translation
        M.translate(-position.x, -position.y, -position.z);
        return M;
    }


}
