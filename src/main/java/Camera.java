import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;


/**
 * Contains camera attributes and functionality
 */
public class Camera {

    // Camera position in world space
    public Vector3f position = new Vector3f(0, 10, 3);
    // Camera velocity in world space
    public Vector3f velocity = new Vector3f(0, 0, 0);
    // Is camera currently airborne
    private boolean isAirborne = true;
    // Camera's parent world
    public World world;

    // Projection properties of the camera
    private Matrix4f projection = new Matrix4f();

    // Orientation properties
    public float pitch = 0;
    public float yaw = 0;
    public float roll = 0;

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
    public Camera(World world) {
        setProjection(aspectRatio, fieldOfView, zNear, zFar);
        this.world = world;
    }

    /**
     * Try to apply the camera's current velocity
     * and detect/correct collisions
     * @param dt
     */
    private void applyVelocity(double dt) {
        Vector3f step = velocity.mul((float) dt, new Vector3f());
        // We will collide
        float[] yoffsets = new float[]{-1.5f, -1.0f, -0.5f, 0f, 0.15f};
        float[] xzoffsets = new float[]{-0.15f, 0f, 0.15f};
        boolean colliding = false;
        for (float y : yoffsets) {
            for (float x : xzoffsets) {
                for (float z : xzoffsets) {
                    colliding = colliding || world.getBlockFromPosition(position.add(new Vector3f(step.x + x, step.y + y, step.z + z), new Vector3f())) != null;
                }
            }
        }
        // We will collide
        if (colliding) {
            // Apply movement axis per axis and check for collision
            // Y axis
            boolean yColliding = false;
            for (float y : yoffsets) {
                for (float x : xzoffsets) {
                    for (float z : xzoffsets) {
                        yColliding = yColliding || world.getBlockFromPosition(position.add(new Vector3f(x, step.y + y, z), new Vector3f())) != null;
                    }
                }
            }
            if (yColliding) {
                position.y = (float) Math.floor(position.y) - (yoffsets[0] % 1f);
                isAirborne = false;
                velocity.y = 0;
            } else {
                position.y = position.y + step.y;
                isAirborne = true;
            }
            boolean xColliding = false;
            for (float y : yoffsets) {
                for (float x : xzoffsets) {
                    for (float z : xzoffsets) {
                        xColliding = xColliding || world.getBlockFromPosition(position.add(new Vector3f(step.x + x, y, z), new Vector3f())) != null;
                    }
                }
            }
            if (xColliding) {
                position.x = (float) ((step.x < 0) ? (Math.floor(position.x) - xzoffsets[0] + 0.005f) : (Math.ceil(position.x)) + xzoffsets[0] - 0.005f);
                velocity.x = 0;
            } else {
                position.x = position.x + step.x;
            }
            boolean zColliding = false;
            for (float y : yoffsets) {
                for (float x : xzoffsets) {
                    for (float z : xzoffsets) {
                        zColliding = zColliding || world.getBlockFromPosition(position.add(new Vector3f(x, y, step.z + z), new Vector3f())) != null;
                    }
                }
            }
            if (zColliding) {
                position.z = (float) ((step.z < 0) ? (Math.floor(position.z) - xzoffsets[0] + 0.005f) : (Math.ceil(position.z)) + xzoffsets[0] - 0.005f);
                velocity.z = 0;
            } else {
                position.z = position.z + step.z;
            }
        } else {
            translate(step);
        }
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

        velocity = new Vector3f(0);
        velocity.add(direction.mul(mv_scl_forward * movementSpeed));
        velocity.add(up.mul(mv_scl_upward * verticalSpeed));
        velocity.add(right.mul(mv_scl_rightward * strafeSpeed));
        applyVelocity(dt);
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
        direction.normalize();
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f right = new Vector3f(direction);
        right.cross(up);

        velocity.x = 0;
        velocity.z = 0;
        velocity.add(direction.mul(mv_scl_forward * movementSpeed));
        velocity.add(right.mul(mv_scl_rightward * strafeSpeed));
        if(jump == 1 && !isAirborne) {
            velocity.y = jumpStrength;
            isAirborne = true;
        }
        velocity.y += gravity * (float) dt;

        applyVelocity(dt);
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
     * Based on crosshair, cast a ray, and check where a new block
     * would be placed
     * @param app App instance
     * @param world World instance
     * @return world space coordinate of new block
     */
    public Vector3f getBlockPlaceCoordinatesAtCrosshair(App app, World world) {
        Vector3f direction = getDirection();
        // March a ray until we hit a block
        Chunk previous = null;
        Vector3i previousCoords = new Vector3i();
        for (float length = marchStep; length < clickRange; length += marchStep) {
            direction.normalize(length);
            Vector3f wp = position.add(direction, new Vector3f());
            Chunk chunk = world.getChunkFromPosition(wp);
            if (previous == null) previous = chunk;
            int x = (int) Math.floor(wp.x) % Chunk.WIDTH;
            int y = (int) Math.floor(wp.y) % Chunk.HEIGHT;
            int z = (int) Math.floor(wp.z) % Chunk.WIDTH;
            if (x < 0) x += Chunk.WIDTH;
            if (z < 0) z += Chunk.WIDTH;
            if (y < 0 || y >= Chunk.HEIGHT) return null;
            if (chunk.blocks[x][z][y] != null) {
                // Backtrack to previous ray position and return
                return new Vector3f(previous.origin.x + previousCoords.x, previous.origin.y + previousCoords.y, previous.origin.z + previousCoords.z);
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
    public Block getBlockAtCrosshair(App app, World world, float range) {
        Vector3f direction = getDirection();
        // March a ray until we hit a block
        for (float length = marchStep; length < range; length += marchStep) {
            direction.normalize(length);
            Vector3f wp = position.add(direction, new Vector3f());
            Chunk chunk = world.getChunkFromPosition(wp);
            int x = (int) Math.floor(wp.x) % Chunk.WIDTH;
            int y = (int) Math.floor(wp.y) % Chunk.HEIGHT;
            int z = (int) Math.floor(wp.z) % Chunk.WIDTH;
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
     * Output this object to a JSON object
     * @return
     */
    public JSONObject toJSON() {
        JSONObject cameraJSON = new JSONObject();
        JSONObject cameraPosition = new JSONObject();
        cameraPosition.put("x", position.x);
        cameraPosition.put("y", position.y);
        cameraPosition.put("z", position.z);
        cameraJSON.put("position", cameraPosition);
        JSONObject cameraRotation = new JSONObject();
        cameraRotation.put("pitch", pitch);
        cameraRotation.put("yaw", yaw);
        cameraRotation.put("roll", roll);
        cameraJSON.put("rotation", cameraRotation);
        return cameraJSON;
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
