public class HillWorldGenerator extends WorldGenerator {

    // Random seed
    public long seed;
    // Amplitude of hills scaled by [-1, 1]
    // So amplitude of 50 has height differences of 100
    public int amplitude;
    // What height does a 0 sample yield, i.e. the base value of the terrain height
    public int height;
    // How noisy is the sampling, also affects steepness
    public float frequency;

    public HillWorldGenerator(long seed, int height, int amplitude, float frequency) {
        super();
        this.seed = seed;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.height = height;
        config.put("seed", String.valueOf(seed));
        config.put("height", String.valueOf(height));
        config.put("amplitude", String.valueOf(amplitude));
        config.put("frequency", String.valueOf(frequency));
    }

    @Override
    public Chunk generate(World world, Chunk chunk) {
        // Chunk must be empty for safety
        if (!chunk.blockList.isEmpty()) return chunk;
        // Generate the hilly chunk based on the supplied configuration
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.WIDTH; z++) {
                // Sample noise based on frequency
                float sample = OpenSimplex2.noise2(seed, (chunk.origin.x + x) / frequency, (chunk.origin.z + z) / frequency);
                // Calculate the resulting height
                int h = height + (int) (sample * amplitude);
                // Fill up to height with different block types
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
