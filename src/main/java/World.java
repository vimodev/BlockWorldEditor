import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic container for anything that a block world might contain
 */
public class World {

    // App reference for global access
    private App app;

    public Camera camera;

    public List<Chunk> chunks;

    public Vector3f skyColor;

    public World(App app) {
        this.app = app;
        camera = new Camera();
        skyColor = new Vector3f(0, 0.6f, 1f);
        chunks = new ArrayList<>();
    }

    private Chunk getChunkFromPosition(Vector3f position) {
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

    // Add a block to its type list
    public void addBlock(Block block) {
        Chunk chunk = getChunkFromPosition(block.position);
        int x = (int) block.position.x % Chunk.WIDTH;
        int y = (int) block.position.y % Chunk.HEIGHT;
        int z = (int) block.position.z % Chunk.WIDTH;
        if (x < 0) x += Chunk.WIDTH;
        if (z < 0) z += Chunk.WIDTH;
        chunk.setBlock(x, y, z, block);
    }

    public void applyInput(double dt) {
        camera.freeMove(app, dt);
    }

    public void render() {
        Renderer.render(this);
    }

    public void fromFile(String filename) {

    }

    public void toFile(String filename) {

    }

}
