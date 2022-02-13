import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;

import static org.lwjgl.opengl.GL11.*;

/**
 * Contains camera attributes and functionality
 */
public class Camera {

    // Properties of the camera
    public Vector3f position = new Vector3f(0, 10, 3);
    private float upwardsVelocity = 0f;
    private boolean isAirborne = true;

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
    public float aspectRatio = 1920f / 1080f;
    public float zNear = 0.1f;
    public float zFar = 1000f;
    public float clickRange = 10f;
    public float marchStep = 0.005f;



    /**
     * Create a new camera
     */
    public Camera() {
        setProjection(aspectRatio, fieldOfView, zNear, zFar);
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
        if(jump == 1 && !isAirborne) {
            upwardsVelocity = jumpStrength;
        }
        translate(up.mul(upwardsVelocity * (float) dt));
        upwardsVelocity += gravity * (float) dt;

        // Hard-code collision at y=4
        position.y = Math.max(position.y, 4f);
        isAirborne = position.y != 4f;
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
     * Read depth buffer and scale back to linear depth to get the depth at the center
     * pixel of the window
     * @param app
     * @return
     */
    public float getDepthAtCrosshair(App app) {
        // Read depth component at center of frame buffer
        FloatBuffer buffer = BufferUtils.createFloatBuffer(1);
        glReadPixels(app.WINDOW_WIDTH / 2, app.WINDOW_HEIGHT / 2, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, buffer);
        float result = buffer.get();
        // Scale back the result from [0,1] non-linear depth to world linear depth
        result = result * 2f - 1f;
        result = (2f * zNear * zFar) / (zFar + zNear - result * (zFar - zNear));
        return result;
    }

    public Vector3f getBlockPlaceCoordinatesAtCrosshair(App app, World world) {
        Vector3f direction = getDirection();
        // March a ray until we hit a block
        Chunk previous;
        Vector3i previousCoords = new Vector3i();
        for (float length = marchStep; length < clickRange; length += marchStep) {
            direction.normalize(length);
            Vector3f wp = position.add(direction, new Vector3f());
            Chunk chunk = world.getChunkFromPosition(wp);
            int x = (int) wp.x % Chunk.WIDTH;
            int y = (int) wp.y % Chunk.HEIGHT;
            int z = (int) wp.z % Chunk.WIDTH;
            if (x < 0) x += Chunk.WIDTH;
            if (z < 0) z += Chunk.WIDTH;
            if (y < 0 || y >= Chunk.HEIGHT) return null;
            if (chunk.blocks[x][z][y] != null) {
                // Backtrack to previous ray position and return
                return new Vector3f(chunk.origin.x + previousCoords.x, chunk.origin.y + previousCoords.y, chunk.origin.z + previousCoords.z);
            }
            previous = chunk;
            previousCoords = new Vector3i(x, y, z);
        }
        return null;
    }

    /**
     * Get the block currently pointed at by the crosshair
     * @param app
     * @param world
     * @return
     */
    public Block getBlockAtCrosshair(App app, World world) {
        Vector3f direction = getDirection();
        // March a ray until we hit a block
        for (float length = marchStep; length < clickRange; length += marchStep) {
            direction.normalize(length);
            Vector3f wp = position.add(direction, new Vector3f());
            Chunk chunk = world.getChunkFromPosition(wp);
            int x = (int) wp.x % Chunk.WIDTH;
            int y = (int) wp.y % Chunk.HEIGHT;
            int z = (int) wp.z % Chunk.WIDTH;
            if (x < 0) x += Chunk.WIDTH;
            if (z < 0) z += Chunk.WIDTH;
            if (y < 0 || y >= Chunk.HEIGHT) return null;
            if (chunk.blocks[x][z][y] != null) {
                return chunk.blocks[x][z][y];
            }
        }
        return null;
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
