/**
 * Generates flat chunks with the specified layers
 */
public class FlatWorldGenerator implements WorldGenerator {

    public int l1; // Layer 1 thickness
    public BlockType l1t; // Layer 1 type
    public int l2; // Layer 2 thickness
    public BlockType l2t; //etc
    public int l3;
    public BlockType l3t;

    public FlatWorldGenerator(int l1, BlockType l1t, int l2, BlockType l2t, int l3, BlockType l3t) {
        this.l1 = l1;
        this.l1t = l1t;
        this.l2 = l2;
        this.l2t = l2t;
        this.l3 = l3;
        this.l3t = l3t;
    }

    @Override
    public Chunk generate(World world, Chunk chunk) {
        // Chunk must be empty for safety
        if (!chunk.blockList.isEmpty()) return chunk;
        // Generate the flat chunk based on the supplied layer configuration
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.WIDTH; z++) {
                // Set the first layer
                for (int y = 0; y < l1; y++) {
                    if (y < 0 || y > Chunk.HEIGHT) continue;
                    chunk.setBlock(x, y, z, new Block(chunk.origin.x + x, chunk.origin.y + y, chunk.origin.z + z, l1t));
                }
                // Set the second layer
                for (int y = l1; y < l1 + l2; y++) {
                    if (y < 0 || y > Chunk.HEIGHT) continue;
                    chunk.setBlock(x, y, z, new Block(chunk.origin.x + x, chunk.origin.y + y, chunk.origin.z + z, l2t));
                }
                // Set the third layer
                for (int y = l1 + l2; y < l1 + l2 + l3; y++) {
                    if (y < 0 || y > Chunk.HEIGHT) continue;
                    chunk.setBlock(x, y, z, new Block(chunk.origin.x + x, chunk.origin.y + y, chunk.origin.z + z, l3t));
                }
            }
        }
        return chunk;
    }
}
