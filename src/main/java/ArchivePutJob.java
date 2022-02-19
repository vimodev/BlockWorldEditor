import java.io.File;

/**
 * Runnable job to archive a chunk to disk
 */
public class ArchivePutJob implements Runnable {

    public Chunk chunk;

    public ArchivePutJob(Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public void run() {
        // Convert chunk to file
        File file = Chunk.toFile(chunk);
        file.deleteOnExit();
        // Add to archive
        ChunkArchiver.unloadedChunksLock.lock();
        ChunkArchiver.unloadedChunks.put(chunk.origin, file);
        ChunkArchiver.unloadedChunksLock.unlock();
    }
}
