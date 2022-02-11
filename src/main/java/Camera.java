import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;

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

    private boolean mouseLocked = false;

    /**
     * Create a new camera
     */
    public Camera() {
        position = new Vector3f(2, 0, 5);
        projection = new Matrix4f();
        setProjection(1920.0f / 1080.0f, 70, 0.1f, 1000f);
    }

    /**
     * Allow the camera to fly freely
     * @param dt
     */
    public void freeMove(App app, double dt) {
        // Mouse looking
        if (!mouseLocked && InputController.primaryMouseButtonHeld()) {
            glfwSetCursorPos(app.window.getWindow(), app.WINDOW_WIDTH / 2, app.WINDOW_HEIGHT / 2);
            mouseLocked = true;
        } else if (mouseLocked) {
            DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
            DoubleBuffer y = BufferUtils.createDoubleBuffer(1);
            glfwGetCursorPos(app.window.getWindow(), x, y);
            x.rewind();
            y.rewind();
            double newX = x.get();
            double newY = y.get();
            double deltaX = newX - app.WINDOW_WIDTH / 2;
            double deltaY = newY - app.WINDOW_HEIGHT / 2;
            //System.out.println("Delta X = " + deltaX + " Delta Y = " + deltaY);
            glfwSetCursorPos(app.window.getWindow(), app.WINDOW_WIDTH / 2, app.WINDOW_HEIGHT / 2);
            pitch((float) deltaY * 0.2f);
            yaw((float) deltaX * 0.2f);
            pitch = Math.max(pitch, -90f); pitch = Math.min(pitch, 90f);
        }
        if (!InputController.primaryMouseButtonHeld()) {
            mouseLocked = false;
        }
        // Keyboard movement
        float mv_scl_forward = InputController.keyHeldInt(GLFW_KEY_W) - InputController.keyHeldInt(GLFW_KEY_S);
        Vector3f direction = getDirection();
        translate(direction.mul(mv_scl_forward * 10f * (float) dt));
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
        M.rotateX((float) Math.toRadians(pitch));
        M.rotateY((float) Math.toRadians(yaw));
        M.rotateZ((float) Math.toRadians(roll));
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

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getRoll() {
        return roll;
    }

    public void translate(Vector3f t) {
        position.add(t);
    }

    public void pitch(float p) {
        pitch += p;
    }

    public void yaw(float y) {
        yaw += y;
    }

    public void roll(float r) {
        roll += r;
    }

    public Matrix4f getProjection() {
        return projection;
    }

}
