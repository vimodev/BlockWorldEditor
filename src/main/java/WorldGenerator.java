/**
 * Interface one can implement to define the generation of new chunks
 */
public interface WorldGenerator {

    Chunk generate(World world, Chunk chunk);

}
