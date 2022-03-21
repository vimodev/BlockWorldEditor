import org.joml.Vector3f;

/**
 * Runnable Job that generates the given chunk with the given generator
 */
public class GenerationJob implements Runnable {

    // Job parameters
    public WorldGenerator generator;
    public World world;
    public Chunk chunk;

    public GenerationJob(WorldGenerator generator, World world, Chunk chunk) {
        this.generator = generator;
        this.world = world;
        this.chunk = chunk;
    }

    /**
     * Fix the sides of a chunk
     */
    private void fixSides() {
        fixSidesFacingLoadedChunks();
        fixSidesFacingLoadingChunks();
    }

    /**
     * Fix sides facing already loaded chunks
     */
    private void fixSidesFacingLoadedChunks() {
        Vector3f px = new Vector3f(chunk.origin).add(Chunk.WIDTH, 0, 0, new Vector3f());
        Vector3f nx = new Vector3f(chunk.origin).add(-Chunk.WIDTH, 0, 0, new Vector3f());
        Vector3f pz = new Vector3f(chunk.origin).add(0, 0, Chunk.WIDTH, new Vector3f());
        Vector3f nz = new Vector3f(chunk.origin).add(0, 0, -Chunk.WIDTH, new Vector3f());
        // If a chunk in the positive x direction is already loaded
        if (world.getChunkFromXZ((int) px.x, (int) px.z) != null) {
            Chunk c = world.getChunkFromXZ((int) px.x, (int) px.z);
            c.lock.lock(); chunk.lock.lock();
            for (int z = 0; z < Chunk.WIDTH; z++){
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.blocks[Chunk.WIDTH - 1][z][y] != null && c.blocks[0][z][y] != null) {
                        chunk.blocks[Chunk.WIDTH - 1][z][y].setFace(1, false);
                        c.blocks[0][z][y].setFace(3, false);
                    }
                }
            }
            c.calculateMesh();
            c.lock.unlock(); chunk.lock.unlock();
        }
        // If a chunk in the negative x direction is already loaded
        if (world.getChunkFromXZ((int) nx.x, (int) nx.z) != null) {
            Chunk c = world.getChunkFromXZ((int) nx.x, (int) nx.z);
            c.lock.lock(); chunk.lock.lock();
            for (int z = 0; z < Chunk.WIDTH; z++){
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.blocks[0][z][y] != null && c.blocks[Chunk.WIDTH - 1][z][y] != null) {
                        chunk.blocks[0][z][y].setFace(3, false);
                        c.blocks[Chunk.WIDTH - 1][z][y].setFace(1, false);
                    }
                }
            }
            c.calculateMesh();
            c.lock.unlock(); chunk.lock.unlock();
        }
        // If a chunk in the positive z direction is already loaded
        if (world.getChunkFromXZ((int) pz.x, (int) pz.z) != null) {
            Chunk c = world.getChunkFromXZ((int) pz.x, (int) pz.z);
            c.lock.lock(); chunk.lock.lock();
            for (int x = 0; x < Chunk.WIDTH; x++){
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.blocks[x][Chunk.WIDTH - 1][y] != null && c.blocks[x][0][y] != null) {
                        chunk.blocks[x][Chunk.WIDTH - 1][y].setFace(2, false);
                        c.blocks[x][0][y].setFace(0, false);
                    }
                }
            }
            c.calculateMesh();
            c.lock.unlock(); chunk.lock.unlock();
        }
        // If a chunk in the negative z direction is already loaded
        if (world.getChunkFromXZ((int) nz.x, (int) nz.z) != null) {
            Chunk c = world.getChunkFromXZ((int) nz.x, (int) nz.z);
            c.lock.lock(); chunk.lock.lock();
            for (int x = 0; x < Chunk.WIDTH; x++){
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.blocks[x][0][y] != null && c.blocks[x][Chunk.WIDTH - 1][y] != null) {
                        chunk.blocks[x][0][y].setFace(0, false);
                        c.blocks[x][Chunk.WIDTH - 1][y].setFace(2, false);
                    }
                }
            }
            c.calculateMesh();
            c.lock.unlock(); chunk.lock.unlock();
        }
    }

    /**
     * Fix sides facing currently loading chunks
     */
    private void fixSidesFacingLoadingChunks() {
        // Not implemented, maybe not necessary
    }

    /**
     * Run this generation job
     */
    @Override
    public void run() {
        // Generate the chunk
        generator.generate(world, chunk);
        // Mark as unmodified as it has been freshly generated
        chunk.modified = false;
        // Set parent world
        chunk.world = world;
        // Fix faces on sides of chunk
        fixSides();
        // Calculate the mesh
        chunk.calculateMesh();
        // And add it to the 'done' queue
        generator.addChunkToQueue(chunk);
    }

}
