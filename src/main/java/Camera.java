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
    public Vector3f position = new Vector3f(0, 10, 3);

    // Current upwards velocity
    private float upwardsVelocity = 0f;

    // Projection properties of the camera
    private Matrix4f projection = new Matrix4f();

    // Orientation properties
    private float pitch = 0;
    private float yaw = 0;
    private float roll = 0;

    // Settings
    private float mouseSensitivity = 0.2f;
    private float movementSpeed = 15f;
    private float strafeSpeed = 10f;
    private float verticalSpeed = 10f;
    private float fieldOfView = 70f;
    private float gravity = -50;
    private float jumpStrength = 20;

    // Is the mouse currently held
    private boolean mouseLocked = false;

    /**
     * Create a new camera
     */
    public Camera() {
        setProjection(1920.0f / 1080.0f, fieldOfView, 0.1f, 1000f);
    }

    /**
     * Allow the camera to fly freely
     * @param dt
     */
    public void freeMove(App app, double dt) {
        // Use delta of mouse to move camera
        DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer y = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(app.window.getWindow(), x, y);
        x.rewind();
        y.rewind();
        double newX = x.get();
        double newY = y.get();
        double deltaX = newX - app.WINDOW_WIDTH / 2;
        double deltaY = newY - app.WINDOW_HEIGHT / 2;

        glfwSetCursorPos(app.window.getWindow(), app.WINDOW_WIDTH / 2, app.WINDOW_HEIGHT / 2);

        yaw((float) deltaX * mouseSensitivity);
        pitch((float) deltaY * mouseSensitivity);

        // Keyboard movement
        float mv_scl_forward = InputController.keyHeldInt(GLFW_KEY_W) - InputController.keyHeldInt(GLFW_KEY_S);
        float mv_scl_upward = InputController.keyHeldInt(GLFW_KEY_SPACE) - InputController.keyHeldInt(GLFW_KEY_LEFT_SHIFT);
        float mv_scl_rightward = InputController.keyHeldInt(GLFW_KEY_D) - InputController.keyHeldInt(GLFW_KEY_A);

        Vector3f direction = getDirection();
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f right = new Vector3f(direction);
        right.cross(up);

        translate(direction.mul(mv_scl_forward * movementSpeed * (float) dt));
        translate(up.mul(mv_scl_upward * verticalSpeed * (float) dt));
        translate(right.mul(mv_scl_rightward * strafeSpeed * (float) dt));
    }

    /**
     * Allow the camera to walk freely
     * @param dt
     */
    public void walkMove(App app, double dt) {
        // Use delta of mouse to move camera
        DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer y = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(app.window.getWindow(), x, y);
        x.rewind();
        y.rewind();
        double newX = x.get();
        double newY = y.get();
        double deltaX = newX - app.WINDOW_WIDTH / 2;
        double deltaY = newY - app.WINDOW_HEIGHT / 2;

        glfwSetCursorPos(app.window.getWindow(), app.WINDOW_WIDTH / 2, app.WINDOW_HEIGHT / 2);

        yaw((float) deltaX * mouseSensitivity);
        pitch((float) deltaY * mouseSensitivity);

        // Keyboard movement
        float mv_scl_forward = InputController.keyHeldInt(GLFW_KEY_W) - InputController.keyHeldInt(GLFW_KEY_S);
        float mv_scl_rightward = InputController.keyHeldInt(GLFW_KEY_D) - InputController.keyHeldInt(GLFW_KEY_A);

        int jump = InputController.keyHeldInt(GLFW_KEY_SPACE);

        Vector3f direction = getDirection();
        direction.y = 0;
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f right = new Vector3f(direction);
        right.cross(up);

        translate(direction.mul(mv_scl_forward * movementSpeed * (float) dt));
        translate(right.mul(mv_scl_rightward * strafeSpeed * (float) dt));
        if(jump == 1) {
            upwardsVelocity = jumpStrength;
        }
        translate(up.mul(upwardsVelocity * (float) dt));
        upwardsVelocity += gravity * (float) dt;

        // Hard-code collision at y=4
        position.y = Math.max(position.y, 4f);
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
        double yawRad = Math.toRadians(yaw-90f);
        double pitchRad = Math.toRadians(-pitch);



        // Apply transformations
        Vector3f dir = new Vector3f(
                (float) (Math.cos(yawRad) * Math.cos(pitchRad)),
                (float) Math.sin(pitchRad),
                (float) (Math.sin(yawRad) * Math.cos(pitchRad))).normalize();
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
        pitch = Math.max(pitch, -89f);
        pitch = Math.min(pitch, 89f);
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
