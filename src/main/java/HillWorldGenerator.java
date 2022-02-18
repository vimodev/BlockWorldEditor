public class HillWorldGenerator implements WorldGenerator {

    public long seed;
    public int amplitude;
    public int height;
    public float frequency;

    public HillWorldGenerator(long seed, int height, int amplitude, float frequency) {
        this.seed = seed;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.height = height;
    }

    @Override
    public Chunk generate(World world, Chunk chunk) {
        // Chunk must be empty for safety
        if (!chunk.blockList.isEmpty()) return chunk;
        // Generate the hilly chunk based on the supplied configuration
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.WIDTH; z++) {
                float sample = OpenSimplex2.noise2(seed, (chunk.origin.x + x) / frequency, (chunk.origin.z + z) / frequency);
                int h = height + (int) (sample * amplitude);
                for (int y = 0; y < h; y++) {
                    if (y >= Chunk.HEIGHT) break;
                    BlockType t = BlockType.STONE;
                    if (y == h - 1) t = BlockType.GRASS;
                    else if (y >= h - 5) t = BlockType.DIRT;
                    chunk.setBlock(x, y, z, new Block(chunk.origin.x + x, chunk.origin.y + y, chunk.origin.z + z, t));
                }
            }
        }
        return chunk;
    }
}
