import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Generic container for anything that a block world might contain
 */
public class World {

    // App reference for global access
    App app;

    public Camera camera;
    public Sun sun;
    public HillWorldGenerator worldGenerator;
    public List<Chunk> chunks;
    public HashMap<Integer, HashMap<Integer, Chunk>> chunkMap;

    // Chunks inside this range should be loaded
    public static float chunkLoadRange = Renderer.RENDER_DISTANCE * 1.30f;
    // Chunks outside this range should be unloaded
    public static float chunkUnloadRange = chunkLoadRange + 128f;

    public Vector3f peakSkyColor = new Vector3f(27f/255f, 49f/255f, 61f/255f);
    public Vector3f skyColor;

    public float time = 900f;
    public float timeRate = 1f;

    public Vector3f select1;
    public Block select1Block;
    public Vector3f select2;
    public Block select2Block;
    public HashMap<Vector3i, BlockType> clipboard;

    public boolean flying = false;

    public World(App app, HillWorldGenerator worldGenerator) {
        this(app);
        this.worldGenerator = worldGenerator;
    }

    public World(App app) {
        this.app = app;
        skyColor = new Vector3f(peakSkyColor);
        chunks = new ArrayList<>();
        chunkMap = new HashMap<>();
        camera = new Camera(this);
        sun = new Sun(this);
    }

    public Chunk addChunk(Chunk c) {
        chunks.add(c);
        if (!chunkMap.containsKey(c.origin.x)) chunkMap.put(c.origin.x, new HashMap<>());
        chunkMap.get(c.origin.x).put(c.origin.z, c);
        return c;
    }

    public Chunk removeChunk(Chunk c) {
        chunks.remove(c);
        chunkMap.get(c.origin.x).remove(c.origin.z);
        if (chunkMap.get(c.origin.x).isEmpty()) chunkMap.remove(c.origin.x);
        return c;
    }

    public Chunk getChunkFromXZ(int x, int z) {
        if (chunkMap.containsKey(x)) {
            return chunkMap.get(x).get(z);
        }
        return null;
    }

    public Chunk getChunkFromPosition(Vector3f position) {
        int floorX = (int) Math.floor(position.x / Chunk.WIDTH) * Chunk.WIDTH;
        int floorZ = (int) Math.floor(position.z / Chunk.WIDTH) * Chunk.WIDTH;
        Chunk chunk = getChunkFromXZ(floorX, floorZ);
        if (chunk == null) chunk = new Chunk(this, floorX, 0, floorZ);
        return chunk;
    }

    /**
     * Load and unload chunks as necessary
     * @return
     */
    public int manageChunks() {
        Vector3f position = new Vector3f(camera.position);
        position.y = 0;
        // Unload all chunks outside of unload range
        List<Chunk> unloadedChunks = new ArrayList<>();
        for (Chunk chunk : chunks) {
            // If outside unload range
            if (position.distance(chunk.origin.x, chunk.origin.y, chunk.origin.z) > chunkUnloadRange) {
                chunk.unloadMesh();
                // If modified we write it to disk, otherwise we can just regenerate it when we need it again
                if (chunk.modified) {
                    ChunkArchiver.archiveChunk(chunk);
                    System.out.println("Archiving modified chunk");
                }
                unloadedChunks.add(chunk);
            }
        }
        for (Chunk c : unloadedChunks) removeChunk(c);
        // Load all missing chunks inside the range
        int loading = 0;
        for (float x = position.x - chunkLoadRange; x < position.x + chunkLoadRange; x += Chunk.WIDTH) {
            for (float z = position.z - chunkLoadRange; z < position.z + chunkLoadRange; z += Chunk.WIDTH) {
                int floorX = (int) Math.floor(x / Chunk.WIDTH) * Chunk.WIDTH;
                int floorZ = (int) Math.floor(z / Chunk.WIDTH) * Chunk.WIDTH;
                if (position.distance(floorX, 0, floorZ) > chunkLoadRange) continue;
                // Check if the chunk already exists
                boolean chunkExists = (getChunkFromXZ(floorX, floorZ) != null);
                // If chunk does not exist, and it is not currently being loaded already
                if (!chunkExists && !isBeingLoaded(new Vector3i(floorX, 0, floorZ))) {
                    Chunk chunk = new Chunk(this, floorX, 0, floorZ);
                    // Generate it or load it
                    if (ChunkArchiver.contains(new Vector3i(floorX, 0, floorZ))) {
                        ChunkArchiver.unarchiveChunk(this, new Vector3i(floorX, 0, floorZ));
                        System.out.println("Unarchiving modified chunk");
                        loading++;
                    } else if (worldGenerator != null) {
                        worldGenerator.dispatch(this, chunk);
                        loading++;
                    } else { // Or add empty one
                        addChunk(chunk);
                    }
                }
            }
        }
        return loading;
    }

    public boolean isBeingLoaded(Vector3i key) {
        return (ChunkArchiver.inProgress.contains(key) || worldGenerator.inProgress.contains(key));
    }

    /**
     * Add all chunks that are done loading to the world
     * and generate their mesh
     */
    public void gatherChunks() {
        List<Chunk> results = ChunkArchiver.gather();
        for (Chunk c : results) {
            c.loadCalculatedMesh();
            addChunk(c);
        }
        if (worldGenerator == null) return;
        results = worldGenerator.gather();
        for (Chunk c : results) {
            c.loadCalculatedMesh();
            addChunk(c);
        }
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
    public Block addBlock(Block block, Vector3f position) {
        Chunk chunk = getChunkFromPosition(position);
        int x = (int) Math.floor(position.x) % Chunk.WIDTH;
        int y = (int) Math.floor(position.y) % Chunk.HEIGHT;
        int z = (int) Math.floor(position.z) % Chunk.WIDTH;
        if (x < 0) x += Chunk.WIDTH;
        if (z < 0) z += Chunk.WIDTH;
        chunk.setBlock(x, y, z, block);
        return block;
    }

    public void toClipboard(Vector3f p1, Vector3f p2) {
        Vector2i xRange = new Vector2i((int) Math.floor(Math.min(p1.x, p2.x)), (int) Math.floor(Math.max(p1.x, p2.x)) + 1);
        Vector2i yRange = new Vector2i((int) Math.floor(Math.min(p1.y, p2.y)), (int) Math.floor(Math.max(p1.y, p2.y)) + 1);
        Vector2i zRange = new Vector2i((int) Math.floor(Math.min(p1.z, p2.z)), (int) Math.floor(Math.max(p1.z, p2.z)) + 1);
        clipboard = new HashMap<>();
        for (int x = xRange.x; x <= xRange.y; x++) {
            for (int y = yRange.x; y <= yRange.y; y++) {
                for (int z = zRange.x; z <= zRange.y; z++) {
                    Block b = getBlockFromPosition(new Vector3f(x, y, z));
                    if (b != null) clipboard.put(new Vector3i(x - (int) Math.floor(p1.x), y - (int) Math.floor(p1.y), z - (int) Math.floor(p1.z)), b.type);
                }
            }
        }
    }

    public void fromClipboard(Vector3f p) {
        if (clipboard == null || clipboard.isEmpty()) return;
        Set<Chunk> affectedChunks = new HashSet<>();
        for (Vector3i key : clipboard.keySet()) {
            Block b = addBlock(new Block(clipboard.get(key)), p.add(key.x, key.y, key.z, new Vector3f()));
            affectedChunks.add(b.chunk);
        }
        for (Chunk c : affectedChunks) c.regenerateMesh();
    }

    public void removeBlocks(Vector3f p1, Vector3f p2) {
        Set<Chunk> affectedChunks = new HashSet<>();
        for (float x = Math.min(p1.x, p2.x); x <= Math.max(p1.x, p2.x); x++) {
            for (float y = Math.min(p1.y, p2.y); y <= Math.max(p1.y, p2.y); y++) {
                for (float z = Math.min(p1.z, p2.z); z <= Math.max(p1.z, p2.z); z++) {
                    Block block = getBlockFromPosition(new Vector3f(x, y, z));
                    if (block != null) {
                        Chunk c = getChunkFromPosition(new Vector3f(x, y, z));
                        c.removeBlock(block.inChunkX, block.inChunkY, block.inChunkZ);
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
                        Vector3f p = new Vector3f((float) Math.floor(x), (float) Math.floor(y), (float) Math.floor(z));
                        block = new Block(type);
                        addBlock(block, p);
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
                        Vector3f pos = new Vector3f((float) Math.floor(x), (float) Math.floor(y), (float) Math.floor(z));
                        block = new Block(t);
                        addBlock(block, pos);
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
                b = new Block(t);
                addBlock(b, new Vector3f(p.x, p.y, p.z));
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
        sun.setTime(time, camera.position);
        double skyColorMultiplier = Math.sqrt((Math.sin((time - 600.0) * Math.PI * 2.0 / 2400.0) + 1) / 2);
        skyColorMultiplier = Math.max(skyColorMultiplier, 0.15f);
        skyColor = peakSkyColor.mul((float) skyColorMultiplier, new Vector3f());

        sun.getSkybox().setRotation((float) (Math.PI + 2*Math.PI*(-time/2400)), 0f, 0f);

        if (InputController.keyPressed(GLFW_KEY_F)) {
            flying = !flying;
        }

        if (InputController.primaryMouseClicked()) {
            Block block = camera.getBlockAtCrosshair(app, this, camera.clickRange);
            if (block != null) {
                Chunk c = block.chunk;
                c.removeBlock(block.inChunkX, block.inChunkY, block.inChunkZ);
                c.regenerateMesh();
            }
        }

        if (InputController.secondaryMouseClicked()) {
            Vector3f loc = camera.getBlockPlaceCoordinatesAtCrosshair(app, this);
            if (loc != null && Toolbar.getSelectedBlock() != null) {
                addBlock(new Block(Toolbar.getSelectedBlock()), new Vector3f(loc.x, loc.y, loc.z));
                getChunkFromPosition(loc).regenerateMesh();
            }
        }

        if (InputController.keyPressed(GLFW_KEY_1)) {
            Block block = camera.getBlockAtCrosshair(app, this, 100f);
            if (block != null) {
                select1 = new Vector3f(block.getPosition());
                Block previous = select1Block;
                select1Block = block;
                if (previous != null) previous.chunk.regenerateMesh();
                block.chunk.regenerateMesh();
            }
        }
        if (InputController.keyPressed(GLFW_KEY_2)) {
            Block block = camera.getBlockAtCrosshair(app, this, 100f);
            if (block != null) {
                select2 = new Vector3f(block.getPosition());
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

}
