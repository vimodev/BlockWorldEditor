import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Generic container for anything that a block world might contain
 */
public class World {

    // App reference for global access
    private App app;

    public Camera camera;

    public WorldGenerator worldGenerator;
    public List<Chunk> chunks;

    public Vector3f skyColor;

    public float time = 800f;
    public float timeRate = 15f;

    public Vector3f select1;
    public Block select1Block;
    public Vector3f select2;
    public Block select2Block;

    // Directional light (e.g. the sun)
    public Light dirLight = new Light(
            new Vector3f(0f,-1f,0f),
            new Vector3f(0.1f, 0.1f, 0.1f),
            new Vector3f(0.8f, 0.7f, 0.7f),
            new Vector3f(0.1f, 0.1f, 0.1f)
    );
    // All point lights (e.g. light blocks, torches)
    public List<Light> pointLights = new ArrayList<Light>();

    public boolean flying = false;

    public World(App app, WorldGenerator worldGenerator) {
        this(app);
        this.worldGenerator = worldGenerator;
    }

    public World(App app) {
        this.app = app;
        skyColor = new Vector3f(0.2f, 0.6f, 0.8f);
        chunks = new ArrayList<>();
        camera = new Camera(this);

//        pointLights.add(new Light(
//                new Vector3f(0f, 52f, 3f),
//                new Vector3f(0.1f, 0.1f, 0.5f),
//                new Vector3f(0.1f, 0.1f, 0.4f),
//                new Vector3f(0.1f, 0.1f, 0.3f)
//        ));
//
//        pointLights.add(new Light(
//                new Vector3f(5f, 52f, 3f),
//                new Vector3f(0.5f, 0.1f, 0.1f),
//                new Vector3f(0.4f, 0.1f, 0.1f),
//                new Vector3f(0.3f, 0.1f, 0.1f)
//                ));
//
//        pointLights.add(new Light(
//                new Vector3f(10f, 52f, 3f),
//                new Vector3f(0.1f, 0.5f, 0.1f),
//                new Vector3f(0.1f, 0.4f, 0.1f),
//                new Vector3f(0.1f, 0.3f, 0.1f)
//        ));


    }

    public Chunk getChunkFromPosition(Vector3f position) {
        int floorX = (int) Math.floor(position.x / Chunk.WIDTH) * Chunk.WIDTH;
        int floorZ = (int) Math.floor(position.z / Chunk.WIDTH) * Chunk.WIDTH;
        for (Chunk chunk : chunks) {
            if (chunk.origin.x == floorX && chunk.origin.z == floorZ) {
                return chunk;
            }
        }
        Chunk chunk = new Chunk(this, floorX, 0, floorZ);
        return chunk;
    }

    /**
     * Dispatch a chunk generation job for all unloaded chunks within distance
     * @param distance
     * @return
     */
    public int generateChunksInRange(float distance) {
        int generating = 0;
        Vector3f position = new Vector3f(camera.position);
        for (float x = position.x - distance; x < position.x + distance; x += Chunk.WIDTH) {
            for (float z = position.z - distance; z < position.z + distance; z += Chunk.WIDTH) {
                if (position.distance(x, position.y, z) > distance) continue;
                int floorX = (int) Math.floor(x / Chunk.WIDTH) * Chunk.WIDTH;
                int floorZ = (int) Math.floor(z / Chunk.WIDTH) * Chunk.WIDTH;
                boolean chunkExists = false;
                for (Chunk chunk : chunks) {
                    if (chunk.origin.x == floorX && chunk.origin.z == floorZ) {
                        chunkExists = true;
                        break;
                    }
                }
                if (!chunkExists) {
                    Chunk chunk = new Chunk(this, floorX, 0, floorZ);
                    if (worldGenerator != null) {
                        worldGenerator.dispatch(this, chunk);
                        generating++;
                    } else {
                        chunks.add(chunk);
                    }
                }
            }
        }
        return generating;
    }

    /**
     * Add all chunks that are done loading to the world
     * and generate their mesh
     */
    public void gatherChunks() {
        if (worldGenerator == null) return;
        List<Chunk> results = worldGenerator.gather();
        chunks.addAll(results);
        for (Chunk c : results) c.regenerateMesh();
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

    public void removeBlocks(Vector3f p1, Vector3f p2) {
        Set<Chunk> affectedChunks = new HashSet<>();
        for (float x = Math.min(p1.x, p2.x); x <= Math.max(p1.x, p2.x); x++) {
            for (float y = Math.min(p1.y, p2.y); y <= Math.max(p1.y, p2.y); y++) {
                for (float z = Math.min(p1.z, p2.z); z <= Math.max(p1.z, p2.z); z++) {
                    Block block = getBlockFromPosition(new Vector3f(x, y, z));
                    if (block != null) {
                        Chunk c = getChunkFromPosition(new Vector3f(x, y, z));
                        c.removeBlock(block.positionInChunk.x, block.positionInChunk.y, block.positionInChunk.z);
                        affectedChunks.add(c);
                    }
                }
            }
        }
        for (Chunk c : affectedChunks) c.regenerateMesh();
    }

    public void replaceBlocks(Vector3f p1, Vector3f p2, BlockType oType, BlockType nType) {
        Set<Chunk> affectedChunks = new HashSet<>();
        for (float x = Math.min(p1.x, p2.x); x <= Math.max(p1.x, p2.x); x++) {
            for (float y = Math.min(p1.y, p2.y); y <= Math.max(p1.y, p2.y); y++) {
                for (float z = Math.min(p1.z, p2.z); z <= Math.max(p1.z, p2.z); z++) {
                    Block block = getBlockFromPosition(new Vector3f(x, y, z));
                    if (block != null && block.type == oType) {
                        block.type = nType;
                        affectedChunks.add(block.chunk);
                    }
                }
            }
        }
        for (Chunk c : affectedChunks) c.regenerateMesh();
    }

    public void setBlocks(Vector3f p1, Vector3f p2, BlockType type) {
        Set<Chunk> affectedChunks = new HashSet<>();
        for (float x = Math.min(p1.x, p2.x); x <= Math.max(p1.x, p2.x); x++) {
            for (float y = Math.min(p1.y, p2.y); y <= Math.max(p1.y, p2.y); y++) {
                for (float z = Math.min(p1.z, p2.z); z <= Math.max(p1.z, p2.z); z++) {
                    Block block = getBlockFromPosition(new Vector3f(x, y, z));
                    if (block != null) {
                        block.type = type;
                    } else {
                        block = new Block((float) Math.floor(x), (float) Math.floor(y), (float) Math.floor(z), type);
                        addBlock(block);
                    }
                    affectedChunks.add(block.chunk);
                }
            }
        }
        for (Chunk c : affectedChunks) c.regenerateMesh();
    }

    /**
     * Set a sphere of given blocktype at given coordinate
     * @param p
     * @param r
     * @param t
     */
    public void setSphere(Vector3f p, int r, BlockType t, boolean hollow) {
        Set<Chunk> affectedChunks = new HashSet<>();
        for (float x = p.x - r; x < p.x + r; x++) {
            for (float z = p.z - r; z < p.z + r; z++) {
                for (float y = p.y - r; y < p.y + r; y++) {
                    if (y < 0 || y > Chunk.HEIGHT - 1) continue;
                    if (p.distance((float) Math.floor(x), (float) Math.floor(y), (float) Math.floor(z)) > r) continue;
                    if (hollow && p.distance((float) Math.floor(x), (float) Math.floor(y), (float) Math.floor(z)) < r - 1) continue;
                    Block block = getBlockFromPosition(new Vector3f(x, y, z));
                    if (block != null) {
                        block.type = t;
                    } else {
                        block = new Block((float) Math.floor(x), (float) Math.floor(y), (float) Math.floor(z), t);
                        addBlock(block);
                    }
                    affectedChunks.add(block.chunk);
                }
            }
        }
        for (Chunk c : affectedChunks) c.regenerateMesh();
    }

    /**
     * Trace a line from p1 to p2 and set to type t
     * @param p1
     * @param p2
     * @param t
     */
    public void setLine(Vector3f p1, Vector3f p2, BlockType t) {
        Set<Chunk> affectedChunks = new HashSet<>();
        Vector3f ray = p1.sub(p2, new Vector3f());
        float distance = ray.length();
        ray.normalize(0.1f);
        Vector3f step = new Vector3f(ray);
        while (ray.length() <= distance) {
            Vector3f p = p2.add(ray, new Vector3f());
            Block b = getBlockFromPosition(p);
            if (b == null) {
                p.floor();
                b = new Block(p.x, p.y, p.z, t);
                addBlock(b);
            } else {
                b.type = t;
            }
            affectedChunks.add(b.chunk);
            ray.add(step);
        }
        for (Chunk c : affectedChunks) c.regenerateMesh();
    }

    public void tick(App app, double dt) {
        // Apply time for day night cycle
        time += timeRate * (float) dt;
        if (time >= 2400f) time -= 2400f;
        // Reposition sun based on time
        dirLight.position = new Vector3f(0, 1, 0);
        dirLight.position.rotateX((time / 2400f) * 2 * (float) Math.PI);

        if (InputController.keyPressed(GLFW_KEY_F)) {
            flying = !flying;
        }

        if (InputController.primaryMouseClicked()) {
            Block block = camera.getBlockAtCrosshair(app, this, camera.clickRange);
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

        if (InputController.keyPressed(GLFW_KEY_1)) {
            Block block = camera.getBlockAtCrosshair(app, this, 100f);
            if (block != null) {
                select1 = new Vector3f(block.position);
                Block previous = select1Block;
                select1Block = block;
                if (previous != null) previous.chunk.regenerateMesh();
                block.chunk.regenerateMesh();
            }
        }
        if (InputController.keyPressed(GLFW_KEY_2)) {
            Block block = camera.getBlockAtCrosshair(app, this, 100f);
            if (block != null) {
                select2 = new Vector3f(block.position);
                Block previous = select2Block;
                select2Block = block;
                if (previous != null) previous.chunk.regenerateMesh();
                block.chunk.regenerateMesh();
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
