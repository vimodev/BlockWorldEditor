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

    @Override
    public void run() {
        // Generate the chunk
        generator.generate(world, chunk);
        // And add it to the 'done' queue
        generator.addChunkToQueue(chunk);
    }

}
