import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Contains camera attributes and functionality
 */
public class Camera {

    // Position of the camera
    public Vector3f position;
    // Projection properties of the camera
    private Matrix4f projection;
    // Orientation properties
    private float pitch = 0;
    private float yaw = 0;
    private float roll = 0;

    /**
     * Create a new camera
     */
    public Camera() {
        position = new Vector3f(2, 0, 5);
        projection = new Matrix4f();
        setProjection(1920.0f / 1080.0f, 70, 0.1f, 1000f);
    }

    /**
     * Get the transformation matrix for this camera
     * Which is inverse to what you normally do
     * @return Transformation matrix for all other object
     */
    public Matrix4f getTransformation() {
        // Set matrix to id
        Matrix4f M = new Matrix4f();
        M.identity();
        // Apply transformations
//        M.rotateX((float) Math.toRadians(pitch));
//        M.rotateY((float) Math.toRadians(yaw));
//        M.rotateZ((float) Math.toRadians(roll));
        // And inverse of translation
        M.translate(position.mul(-1, new Vector3f()));
        // *note:
        // When we move the camera, we actually move the entire world in the opposite direction, thats why
        return M;
    }

    /**
     * What is the normalized direction vector of this camera
     * @return
     */
    public Vector3f getDirection() {
        // Apply transformations
        Vector3f dir = new Vector3f(0, 0, -1);
        dir.rotateX((float) Math.toRadians(-pitch));
        dir.rotateY((float) Math.toRadians(-yaw));
        dir.rotateZ((float) Math.toRadians(roll));
        return dir;
    }

    /**
     * Set projective properties of the camera
     * @param a aspect ratio
     * @param fov field of view, vertically
     * @param znear near plane
     * @param zfar far plane
     */
    public void setProjection(float a, float fov, float znear, float zfar) {
        this.projection.setPerspective(fov, a, znear, zfar);
    }

    public Matrix4f getProjection() {
        return projection;
    }

}
