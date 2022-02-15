import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Generic container for anything that a block world might contain
 */
public class World {

    // App reference for global access
    private App app;

    public Camera camera;

    public List<Chunk> chunks;

    public Vector3f skyColor;

    // Directional light (e.g. the sun)
    public Light dirLight = new Light(
            new Vector3f(0f,-1f,0f),
            new Vector3f(0.1f, 0.1f, 0.1f),
            new Vector3f(0.4f, 0.35f, 0.35f),
            new Vector3f(0.1f, 0.1f, 0.1f)
    );
    // All point lights (e.g. light blocks, torches)
    public List<Light> pointLights = new ArrayList<Light>();

    public boolean flying = false;

    public World(App app) {
        this.app = app;
        skyColor = new Vector3f(0.2f, 0.6f, 0.8f);
        chunks = new ArrayList<>();
        camera = new Camera(this);

        pointLights.add(new Light(
                new Vector3f(0f, 52f, 3f),
                new Vector3f(0.1f, 0.1f, 0.5f),
                new Vector3f(0.1f, 0.1f, 0.4f),
                new Vector3f(0.1f, 0.1f, 0.3f)
        ));

        pointLights.add(new Light(
                new Vector3f(5f, 52f, 3f),
                new Vector3f(0.5f, 0.1f, 0.1f),
                new Vector3f(0.4f, 0.1f, 0.1f),
                new Vector3f(0.3f, 0.1f, 0.1f)
                ));

        pointLights.add(new Light(
                new Vector3f(10f, 52f, 3f),
                new Vector3f(0.1f, 0.5f, 0.1f),
                new Vector3f(0.1f, 0.4f, 0.1f),
                new Vector3f(0.1f, 0.3f, 0.1f)
        ));


    }

    public Chunk getChunkFromPosition(Vector3f position) {
        int floorX = (int) Math.floor(position.x / Chunk.WIDTH) * Chunk.WIDTH;
        int floorZ = (int) Math.floor(position.z / Chunk.WIDTH) * Chunk.WIDTH;
        for (Chunk chunk : chunks) {
            if (chunk.origin.x == floorX && chunk.origin.z == floorZ) {
                return chunk;
            }
        }
        Chunk chunk = new Chunk(floorX, 0, floorZ);
        chunks.add(chunk);
        return chunk;
    }

    public Block getBlockFromPosition(Vector3f position) {
        Chunk chunk = getChunkFromPosition(position);
        Vector3i loc = chunk.getLocalPosition(position);
        if (loc.y >= 0) {
            return chunk.blocks[loc.x][loc.z][loc.y];
        }
        return null;
    }

    // Add a block to its type list
    public void addBlock(Block block) {
        Chunk chunk = getChunkFromPosition(block.position);
        int x = (int) Math.floor(block.position.x) % Chunk.WIDTH;
        int y = (int) Math.floor(block.position.y) % Chunk.HEIGHT;
        int z = (int) Math.floor(block.position.z) % Chunk.WIDTH;
        if (x < 0) x += Chunk.WIDTH;
        if (z < 0) z += Chunk.WIDTH;
        chunk.setBlock(x, y, z, block);
    }

    public void applyInput(App app, double dt) {
        if (InputController.keyPressed(GLFW_KEY_F)) {
            flying = !flying;
        }

        if (InputController.primaryMouseClicked()) {
            Block block = camera.getBlockAtCrosshair(app, this);
            if (block != null) {
                Chunk c = block.chunk;
                c.removeBlock(block.positionInChunk.x, block.positionInChunk.y, block.positionInChunk.z);
                c.regenerateMesh();
            }
        }

        if (InputController.secondaryMouseClicked()) {
            Vector3f loc = camera.getBlockPlaceCoordinatesAtCrosshair(app, this);
            if (loc != null && Toolbar.getSelectedBlock() != null) {
                addBlock(new Block(loc.x, loc.y, loc.z, Toolbar.getSelectedBlock()));
                getChunkFromPosition(loc).regenerateMesh();
            }
        }

        if (flying) {
            camera.freeMove(app, dt);
        } else {
            camera.walkMove(app, dt);
        }

    }

    public void render() {
        Renderer.render(this);
    }

    public void fromFile(String filename) {

    }

    public void toFile(String filename) {

    }

}
