import org.joml.Vector3i;

import java.io.File;

/**
 * Runnable job to archive a chunk to disk
 */
public class ArchiveFetchJob implements Runnable {

    public Vector3i key;
    public World world;

    public ArchiveFetchJob(World world, Vector3i key) {
        this.key = key;
        this.world = world;
    }

    @Override
    public void run() {
        // Fetch the file
        ChunkArchiver.unloadedChunksLock.lock();
        File file = ChunkArchiver.unloadedChunks.remove(key);
        ChunkArchiver.unloadedChunksLock.unlock();
        // Load the chunk
        Chunk chunk = Chunk.fromFile(world, key, file);
        chunk.calculateMesh();
        // Remove the file
        file.delete();
        // Add it to queue
        ChunkArchiver.chunkQueueLock.lock();
        ChunkArchiver.chunkQueue.add(chunk);
        ChunkArchiver.chunkQueueLock.unlock();
    }
}
