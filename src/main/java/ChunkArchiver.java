import org.joml.Vector3f;
import org.joml.Vector3i;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Maintains archive of on-disk chunks for cold storage to stay memory frugal
 */
public class ChunkArchiver {

    // Archive of unloaded chunks
    public static Map<Vector3i, File> unloadedChunks = new HashMap<>();
    public static Lock unloadedChunksLock = new ReentrantLock(true);

    // Queue of loaded chunks, ready to be gathered by main thread
    public static List<Chunk> chunkQueue = new ArrayList<>();
    public static Lock chunkQueueLock = new ReentrantLock(true);

    // Currently running put / fetch jobs
    public static List<Thread> jobs = new ArrayList<>();

    // All origins of all chunks either in progress or in queue
    public static Set<Vector3i> inProgress = new HashSet<>();

    /**
     * Does the archive contain the key?
     * @param key
     * @return
     */
    public static boolean contains(Vector3i key) {
        unloadedChunksLock.lock();
        boolean contains = unloadedChunks.containsKey(key);
        unloadedChunksLock.unlock();
        return contains;
    }

    /**
     * Gather all finished jobs
     * @return
     */
    public static List<Chunk> gather() {
        List<Thread> finished = new ArrayList<>();
        for (Thread thread : jobs) {
            if (!thread.isAlive()) finished.add(thread);
        }
        for (Thread thread : finished) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        jobs.removeAll(finished);
        List<Chunk> results = clearQueue();
        for (Chunk c : results) inProgress.remove(c.origin);
        return results;
    }

    /**
     * Clear the result queue
     * @return
     */
    public static List<Chunk> clearQueue() {
        chunkQueueLock.lock();
        List<Chunk> results = new ArrayList<>(chunkQueue);
        chunkQueue.clear();
        chunkQueueLock.unlock();
        return results;
    }

    /**
     * Archive the given chunk to disk
     * @param chunk
     */
    public static void archiveChunk(Chunk chunk) {
        Thread thread = new Thread(new ArchivePutJob(chunk));
        jobs.add(thread);
        thread.start();
    }

    /**
     * Unarchive the chunk at given position
     * @param world
     * @param key
     */
    public static void unarchiveChunk(World world, Vector3i key) {
        Thread thread = new Thread(new ArchiveFetchJob(world, key));
        jobs.add(thread);
        inProgress.add(key);
        thread.start();
    }

}
