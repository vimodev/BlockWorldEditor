import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;

import static org.lwjgl.opengl.GL11.*;

/**
 * Contains camera attributes and functionality
 */
public class Camera {

    // Properties of the camera
    public Vector3f position = new Vector3f(0, 10, 3);
    public Vector3f velocity = new Vector3f(0, 0, 0);
    private boolean isAirborne = true;
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

    private static boolean isAabbCollision(Vector3f bo, Vector3f bd, Block block) {
        Vector3f p = new Vector3f(block.position);
        return (
                        bo.x        < p.x + 1   &&
                        bo.x + bd.x > p.x       &&
                        bo.y        < p.y + 1   &&
                        bo.y + bd.y > p.y       &&
                        bo.z        < p.z + 1   &&
                        bo.z + bd.z > p.z
                );
    }

    private static Vector3f getAabbDistance(Vector3f bo, Vector3f bd, Block block) {
        Vector3f delta = new Vector3f(0);
        Vector3f p = new Vector3f(block.position);
        if (bo.x < p.x)         delta.x = p.x - (bo.x + bd.x);
        else if (p.x > bo.x)    delta.x = bo.x - (p.x + 1);
        if (bo.y < p.y)         delta.y = p.y - (bo.y + bd.y);
        else if (p.y > bo.y)    delta.y = bo.y - (p.y + 1);
        if (bo.z < p.z)         delta.z = p.z - (bo.z + bd.z);
        else if (p.z > bo.z)    delta.z = bo.z - (p.z + 1);
        return delta;
    }

    private void collisionCheck(Vector3f bo, Vector3f bd) {
        List<Block> candidates = new ArrayList<>();
        for (float dx = -1; dx < bd.x + 1; dx++) {
            for (float dy = -1; dy < bd.y + 1; dy++) {
                for (float dz = -1; dz < bd.z + 1; dz++) {
                    Block block = world.getBlockFromPosition(new Vector3f(
                            bo.x + dx, bo.y + dy, + bo.z + dz
                    ));
                    if (block != null && !candidates.contains(block)) candidates.add(block);
                }
            }
        }
        if (candidates.isEmpty()) return;
        for (Block candidate : candidates) {
            if (isAabbCollision(bo, bd, candidate)) System.out.println("colliding");
        }
    }

    private void applyVelocity(double dt) {
        Vector3f step = velocity.mul((float) dt, new Vector3f());
        Vector3f next = position.add(step, new Vector3f());
        Block block = world.getBlockFromPosition(next);
        // We will collide
        if (block != null) {
            // Apply movement axis per axis and check for collision
            // Y axis
            if (world.getBlockFromPosition(position.add(new Vector3f(0, step.y, 0), new Vector3f())) != null) {
                position.y = (float) ((step.y < 0) ? Math.floor(position.y) : Math.ceil(position.y));
                isAirborne = false;
                velocity.y = 0;
            } else {
                position.y = position.y + step.y;
            }
            if (world.getBlockFromPosition(position.add(new Vector3f(step.x, 0, 0), new Vector3f())) != null) {
                position.x = (float) ((step.x < 0) ? Math.floor(position.x) : Math.ceil(position.x));
            } else {
                position.x = position.x + step.x;
            }
            if (world.getBlockFromPosition(position.add(new Vector3f(0, 0, step.z), new Vector3f())) != null) {
                position.z = (float) ((step.z < 0) ? Math.floor(position.z) : Math.ceil(position.z));
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

    public Vector3f getBlockPlaceCoordinatesAtCrosshair(App app, World world) {
        Vector3f direction = getDirection();
        // March a ray until we hit a block
        Chunk previous;
        Vector3i previousCoords = new Vector3i();
        for (float length = marchStep; length < clickRange; length += marchStep) {
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
