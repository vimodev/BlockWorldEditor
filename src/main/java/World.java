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

    // List of blocks for each block type
    public Map<BlockType, List<Block>> blocks;

    public Vector3f skyColor;

    public World(App app) {
        this.app = app;
        camera = new Camera();
        blocks = new HashMap<>();
        for (BlockType type : BlockType.values()) blocks.put(type, new ArrayList<>());
        skyColor = new Vector3f(0, 0.6f, 1f);
    }

    // Add a block to its type list
    public void addBlock(Block block) {
        blocks.get(block.type).add(block);
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
